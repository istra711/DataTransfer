package de.willuhn.jameica.hbci.datatransfer.parser;

import de.willuhn.jameica.hbci.datatransfer.OcrSettings;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * OCR-Engine Wrapper fuer Tesseract (ueber tess4j).
 * Verwendet Reflection, um direkte Compile-Abhaengigkeit zu vermeiden.
 */
public class OcrEngine {

    private static final Logger logger = Logger.getLogger(OcrEngine.class.getName());
    private static OcrEngine instance;

    private Object tessInstance;
    private boolean available = false;

    private OcrEngine() {
        initTess4j();
    }

    public static synchronized OcrEngine getInstance() {
        if (instance == null) {
            instance = new OcrEngine();
        }
        return instance;
    }

    private void initTess4j() {
        try {
            Class<?> tessClass = Class.forName("net.sourceforge.tess4j.Tesseract");
            tessInstance = tessClass.getConstructor().newInstance();
            available = true;
            logger.info("tess4j erfolgreich geladen");
        } catch (Exception e) {
            logger.warning("tess4j nicht verfuegbar: " + e.getMessage());
            available = false;
        }
    }

    public void init(String datapath, String language) {
        if (!available || tessInstance == null) return;

        try {
            Method setDatapath = tessInstance.getClass().getMethod("setDatapath", String.class);
            setDatapath.invoke(tessInstance, datapath);

            Method setLanguage = tessInstance.getClass().getMethod("setLanguage", String.class);
            setLanguage.invoke(tessInstance, language);

            OcrSettings settings = OcrSettings.getInstance();

            Method setOcrEngineMode = tessInstance.getClass().getMethod("setOcrEngineMode", int.class);
            setOcrEngineMode.invoke(tessInstance, Integer.parseInt(settings.getOcrEngineMode()));

            Method setPageSegMode = tessInstance.getClass().getMethod("setPageSegMode", int.class);
            setPageSegMode.invoke(tessInstance, Integer.parseInt(settings.getPageSegmentationMode()));

            if (settings.getWhitelist() != null && !settings.getWhitelist().isEmpty()) {
                Method setVariable = tessInstance.getClass().getMethod("setVariable", String.class, String.class);
                setVariable.invoke(tessInstance, "tessedit_char_whitelist", settings.getWhitelist());
            }

            if (settings.getBlacklist() != null && !settings.getBlacklist().isEmpty()) {
                Method setVariable = tessInstance.getClass().getMethod("setVariable", String.class, String.class);
                setVariable.invoke(tessInstance, "tessedit_char_blacklist", settings.getBlacklist());
            }

            Method setVariable = tessInstance.getClass().getMethod("setVariable", String.class, String.class);
            setVariable.invoke(tessInstance, "preserve_interword_spaces",
                settings.isPreserveInterwordSpaces() ? "1" : "0");

            logger.info("Tesseract initialisiert: datapath=" + datapath + ", language=" + language);
        } catch (Exception e) {
            logger.warning("Fehler bei Tesseract-Initialisierung: " + e.getMessage());
        }
    }

    public String doOCR(BufferedImage image) {
        if (!available || tessInstance == null) return null;

        try {
            File tempFile = File.createTempFile("ocr_", ".png");
            javax.imageio.ImageIO.write(image, "png", tempFile);

            Method doOCR = tessInstance.getClass().getMethod("doOCR", File.class);
            String result = (String) doOCR.invoke(tessInstance, tempFile);

            tempFile.delete();
            return result;
        } catch (Exception e) {
            logger.warning("OCR fehlgeschlagen: " + e.getMessage());
            return null;
        }
    }

    public String doOCR(File file) {
        if (!available || tessInstance == null) return null;

        try {
            Method doOCR = tessInstance.getClass().getMethod("doOCR", File.class);
            return (String) doOCR.invoke(tessInstance, file);
        } catch (Exception e) {
            logger.warning("OCR fehlgeschlagen: " + e.getMessage());
            return null;
        }
    }

    public boolean isAvailable() {
        return available;
    }
}
