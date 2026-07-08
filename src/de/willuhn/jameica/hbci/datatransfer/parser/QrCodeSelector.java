package de.willuhn.jameica.hbci.datatransfer.parser;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;

import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Multi-QR-Erkennung und Auswahl.
 * Uebernommen aus QRtransfer.
 */
public class QrCodeSelector {

    private static final Logger logger = Logger.getLogger(QrCodeSelector.class.getName());
    private static I18N i18n;

    private static synchronized I18N getI18n() {
        if (i18n == null) {
            try {
                i18n = Application.getPluginLoader()
                    .getPlugin("de.willuhn.jameica.hbci.datatransfer.DataTransferPlugin")
                    .getResources()
                    .getI18N();
            } catch (Exception e) {
                // Fallback wenn Plugin noch nicht geladen
                return null;
            }
        }
        return i18n;
    }

    private static final QrCodeParser[] PARSERS = {
        new EpcParser(),
        new EmvParser()
    };

    public static List<String> decodeMultiple(BufferedImage image) throws Exception {
        List<String> results = new ArrayList<>();
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));

        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader delegate = new MultiFormatReader();
            GenericMultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(delegate);
            Result[] multiResults = multiReader.decodeMultiple(bitmap, hints);
            if (multiResults != null) {
                for (Result r : multiResults) {
                    if (r != null && r.getText() != null && !r.getText().isEmpty()) {
                        if (!results.contains(r.getText())) {
                            results.add(r.getText());
                        }
                    }
                }
            }
            logger.info("GenericMultipleBarcodeReader found " + results.size() + " QR code(s) in full image");
        } catch (NotFoundException e) {
            logger.info("GenericMultipleBarcodeReader: no QR codes in full image");
        }

        if (results.isEmpty()) {
            int w = image.getWidth();
            int h = image.getHeight();
            if (w > 100 && h > 100) {
                int halfW = w / 2;
                int halfH = h / 2;

                int[][] crops = {
                    {0, 0, halfW, halfH},
                    {halfW, 0, w, halfH},
                    {0, halfH, halfW, h},
                    {halfW, halfH, w, h}
                };

                for (int[] crop : crops) {
                    try {
                        BufferedImage sub = image.getSubimage(crop[0], crop[1], crop[2] - crop[0], crop[3] - crop[1]);
                        LuminanceSource subSource = new BufferedImageLuminanceSource(sub);
                        BinaryBitmap subBitmap = new BinaryBitmap(new HybridBinarizer(subSource));
                        Result result = new MultiFormatReader().decode(subBitmap, hints);
                        if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                            if (!results.contains(result.getText())) {
                                results.add(result.getText());
                            }
                        }
                    } catch (NotFoundException e) {
                        // ignore
                    }
                }
                logger.info("After 2x2 quadrant scan: " + results.size() + " QR code(s)");
            }
        }

        if (results.isEmpty() && image.getWidth() > 100 && image.getHeight() > 100) {
            int cols = 3;
            int rows = 3;
            int cellW = image.getWidth() / cols;
            int cellH = image.getHeight() / rows;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    try {
                        int x = c * cellW;
                        int y = r * cellH;
                        int cw = Math.min(cellW, image.getWidth() - x);
                        int ch = Math.min(cellH, image.getHeight() - y);
                        if (cw > 50 && ch > 50) {
                            BufferedImage sub = image.getSubimage(x, y, cw, ch);
                            LuminanceSource subSource = new BufferedImageLuminanceSource(sub);
                            BinaryBitmap subBitmap = new BinaryBitmap(new HybridBinarizer(subSource));
                            Result result = new MultiFormatReader().decode(subBitmap, hints);
                            if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                                if (!results.contains(result.getText())) {
                                    results.add(result.getText());
                                }
                            }
                        }
                    } catch (NotFoundException e) {
                        // ignore
                    }
                }
            }
            logger.info("After 3x3 grid scan: " + results.size() + " QR code(s)");
        }

        return results;
    }

    public static List<String> filterValidSepa(List<String> allQrTexts) {
        List<String> valid = new ArrayList<>();
        for (String text : allQrTexts) {
            for (QrCodeParser parser : PARSERS) {
                if (parser.canParse(text)) {
                    try {
                        parser.parse(text);
                        valid.add(text);
                        break;
                    } catch (ParserException e) {
                        logger.info("Invalid SEPA QR: " + e.getMessage());
                    }
                }
            }
        }
        return valid;
    }

    public static String decodeAndSelect(BufferedImage image, Component parent) throws Exception {
        List<String> all = decodeMultiple(image);
        List<String> valid = filterValidSepa(all);
        if (valid.isEmpty()) {
            return null;
        }
        return selectFromMultiple(valid, parent);
    }

    public static String selectFromMultiple(List<String> validQrTexts, Component parent) {
        if (validQrTexts.size() == 1) {
            return validQrTexts.get(0);
        }

        String title = "QR-Code auswaehlen";
        String message = validQrTexts.size() + " SEPA-QR-Codes gefunden. Waehlen Sie einen aus:";

        String[] options = new String[validQrTexts.size()];
        for (int idx = 0; idx < validQrTexts.size(); idx++) {
            String text = validQrTexts.get(idx);
            options[idx] = (idx + 1) + ": " + (text.length() > 80 ? text.substring(0, 80) + "..." : text);
        }

        String selected = (String) JOptionPane.showInputDialog(
            parent,
            message,
            title,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

        if (selected == null) {
            return null;
        }

        int selectedIndex = Integer.parseInt(selected.split(":")[0].trim()) - 1;
        if (selectedIndex >= 0 && selectedIndex < validQrTexts.size()) {
            return validQrTexts.get(selectedIndex);
        }

        return null;
    }
}
