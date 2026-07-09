package de.willuhn.jameica.hbci.datatransfer.parser;

import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData.Source;

import com.google.zxing.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Automatische Erkennung: QR-Code oder OCR.
 */
public class SmartDetector {

    private static final Logger logger = Logger.getLogger(SmartDetector.class.getName());

    private static final QrCodeParser[] QR_PARSERS = {
        new EpcParser(),
        new EmvParser()
    };

    /**
     * Erkennt automatisch die Quelle und extrahiert die Daten.
     * Reihenfolge: QR-Code (EPC/EMV) -> OCR
     */
    public static TransferData detectFromFile(File file) throws Exception {
        String lowerPath = file.getName().toLowerCase();

        if (lowerPath.endsWith(".pdf")) {
            return detectFromPDF(file);
        } else {
            return detectFromImage(file);
        }
    }

    /**
     * Erkennt automatisch aus einem BufferedImage.
     */
    public static TransferData detectFromImage(BufferedImage image, String quelle) {
        // 1. QR-Code versuchen
        TransferData qrResult = tryQrCode(image);
        if (qrResult != null) {
            qrResult.setQuelle(quelle);
            return qrResult;
        }

        // 2. OCR als Fallback
        return tryOcr(image, quelle);
    }

    /**
     * Erkennt automatisch aus einem BufferedImage (ohne Quelle).
     */
    public static TransferData detectFromImage(BufferedImage image) {
        return detectFromImage(image, null);
    }

    /**
     * Erkennt automatisch aus einer Datei (Bild).
     */
    private static TransferData detectFromImage(File file) throws Exception {
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new Exception("Bild konnte nicht geladen werden: " + file.getName());
        }
        return detectFromImage(image, file.getAbsolutePath());
    }

    /**
     * Erkennt automatisch aus einer PDF-Datei.
     * Versucht zuerst QR-Code, dann Textextraktion, dann OCR.
     */
    private static TransferData detectFromPDF(File file) throws Exception {
        // 1. QR-Code in PDF versuchen
        try {
            TransferData qrResult = tryQrCodeInPDF(file);
            if (qrResult != null) {
                qrResult.setQuelle(file.getAbsolutePath());
                return qrResult;
            }
        } catch (Exception e) {
            logger.info("Kein QR-Code in PDF: " + e.getMessage());
        }

        // 2. Text aus PDF extrahieren (wenn moeglich)
        try {
            String text = extractTextFromPDF(file);
            if (text != null && !text.trim().isEmpty()) {
                InvoiceTextParser parser = new InvoiceTextParser();
                TransferData data = parser.parse(text);
                data.setQuelle(file.getAbsolutePath());
                data.setSource(Source.OCR);
                data.setOcrVerwendet(true);
                return data;
            }
        } catch (Exception e) {
            logger.info("Kein Text in PDF: " + e.getMessage());
        }

        // 3. OCR als Fallback (PDF-Seiten rendern)
        return tryOcrFromPDF(file);
    }

    /**
     * Versucht einen QR-Code aus einem Bild zu lesen.
     * Unterstuetzt mehrere QR-Codes mit Auswahl.
     */
    private static TransferData tryQrCode(BufferedImage image) {
        try {
            List<String> allQrTexts = QrCodeSelector.decodeMultiple(image);
            if (allQrTexts.isEmpty()) {
                return null;
            }

            List<String> validSepa = QrCodeSelector.filterValidSepa(allQrTexts);
            if (validSepa.isEmpty()) {
                return null;
            }

            String selected;
            if (validSepa.size() == 1) {
                selected = validSepa.get(0);
            } else {
                selected = QrCodeSelector.selectFromMultiple(validSepa, null);
            }

            if (selected == null) {
                return null;
            }

            return parseQrText(selected);
        } catch (Exception e) {
            logger.info("QR-Code-Erkennung fehlgeschlagen: " + e.getMessage());
        }
        return null;
    }

    /**
     * Versucht QR-Codes aus einer PDF zu lesen.
     * Scannt alle Seiten und sammelt alle QR-Codes.
     */
    private static TransferData tryQrCodeInPDF(File file) throws Exception {
        List<String> allQrTexts = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 200);
                try {
                    List<String> pageQrCodes = QrCodeSelector.decodeMultiple(image);
                    for (String qrText : pageQrCodes) {
                        if (!allQrTexts.contains(qrText)) {
                            allQrTexts.add(qrText);
                        }
                    }
                } catch (Exception e) {
                    logger.info("QR-Code-Erkennung auf Seite " + (page + 1) + " fehlgeschlagen: " + e.getMessage());
                }
            }
        }

        if (allQrTexts.isEmpty()) {
            return null;
        }

        logger.info("Insgesamt " + allQrTexts.size() + " QR-Code(s) in PDF gefunden");

        List<String> validSepa = QrCodeSelector.filterValidSepa(allQrTexts);
        if (validSepa.isEmpty()) {
            logger.info("Keine gueltigen SEPA-QR-Codes gefunden");
            return null;
        }

        String selected;
        if (validSepa.size() == 1) {
            selected = validSepa.get(0);
        } else {
            selected = QrCodeSelector.selectFromMultiple(validSepa, null);
        }

        if (selected == null) {
            return null;
        }

        return parseQrText(selected);
    }

    /**
     * Parst einen QR-Code-Text zu TransferData.
     */
    private static TransferData parseQrText(String qrText) {
        for (QrCodeParser parser : QR_PARSERS) {
            if (parser.canParse(qrText)) {
                try {
                    TransferData data = parser.parse(qrText);
                    data.setRawText(qrText);
                    return data;
                } catch (ParserException e) {
                    logger.warning("Parser-Fehler: " + e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Fuehrt OCR auf einem Bild durch.
     */
    private static TransferData tryOcr(BufferedImage image, String quelle) {
        try {
            de.willuhn.jameica.hbci.datatransfer.OcrSettings settings =
                de.willuhn.jameica.hbci.datatransfer.OcrSettings.getInstance();
            OcrEngine ocrEngine = OcrEngine.getInstance();
            ocrEngine.init(settings.getTessdataPath(), settings.getLanguage());

            if (!ocrEngine.isAvailable()) {
                logger.warning("OCR-Engine nicht verfuegbar");
                return null;
            }

            String text = ocrEngine.doOCR(image);
            if (text == null || text.trim().isEmpty()) {
                return null;
            }

            InvoiceTextParser parser = new InvoiceTextParser();
            TransferData data = parser.parse(text);
            data.setQuelle(quelle);
            data.setSource(Source.OCR);
            data.setOcrVerwendet(true);
            return data;
        } catch (Exception e) {
            logger.warning("OCR fehlgeschlagen: " + e.getMessage());
            return null;
        }
    }

    /**
     * Fuehrt OCR auf einer PDF-Datei durch.
     */
    private static TransferData tryOcrFromPDF(File file) throws Exception {
        de.willuhn.jameica.hbci.datatransfer.OcrSettings settings =
            de.willuhn.jameica.hbci.datatransfer.OcrSettings.getInstance();
        OcrEngine ocrEngine = OcrEngine.getInstance();
        ocrEngine.init(settings.getTessdataPath(), settings.getLanguage());

        if (!ocrEngine.isAvailable()) {
            throw new Exception("OCR-Engine nicht verfuegbar");
        }

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            StringBuilder fullText = new StringBuilder();
            int dpi = settings.getDpi();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                String pageText = ocrEngine.doOCR(image);
                if (pageText != null && !pageText.isEmpty()) {
                    fullText.append(pageText).append("\n");
                }
            }

            if (fullText.length() == 0) {
                throw new Exception("Kein Text erkannt");
            }

            InvoiceTextParser parser = new InvoiceTextParser();
            TransferData data = parser.parse(fullText.toString());
            data.setQuelle(file.getAbsolutePath());
            data.setSource(Source.OCR);
            data.setOcrVerwendet(true);
            return data;
        }
    }

    /**
     * Extrahiert Text aus einer PDF-Datei.
     */
    private static String extractTextFromPDF(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Erkennt automatisch aus einem InputStream.
     * Wird vom Importer verwendet.
     */
    public static TransferData detectFromStream(InputStream is, de.willuhn.util.ProgressMonitor monitor) throws Exception {
        if (is == null) {
            throw new Exception("InputStream ist null");
        }

        // In temporaere Datei schreiben (fuer PDF-Behandlung noetig)
        File tempFile = File.createTempFile("datatransfer_", ".tmp");
        tempFile.deleteOnExit();

        try {
            // InputStream in Datei kopieren
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();

            // Detection durchfuehren
            return detectFromFile(tempFile);
        } finally {
            tempFile.delete();
        }
    }
}
