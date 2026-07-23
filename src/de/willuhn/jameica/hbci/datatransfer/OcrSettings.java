package de.willuhn.jameica.hbci.datatransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Einstellungen fuer OCR und QR-Code-Erkennung.
 */
public class OcrSettings {

    private static final Logger logger = Logger.getLogger(OcrSettings.class.getName());
    private static final String SETTINGS_FILE = "ocr-settings.properties";

    // Page Segmentation Modes
    public static final String PSM_AUTO = "3";
    public static final String PSM_AUTO_OSD = "0";
    public static final String PSM_SINGLE_BLOCK = "6";
    public static final String PSM_SINGLE_LINE = "7";
    public static final String PSM_SINGLE_WORD = "8";
    public static final String PSM_SINGLE_CHAR = "10";
    public static final String PSM_SPARSE_TEXT = "11";

    // OCR Engine Modes
    public static final String OEM_DEFAULT = "0";
    public static final String OEM_LSTM_ONLY = "1";
    public static final String OEM_LEGACY_ONLY = "2";
    public static final String OEM_LSTM_AND_LEGACY = "3";

    private static OcrSettings instance;

    // OCR settings
    private String language = "deu";
    private String pageSegmentationMode = PSM_AUTO;
    private String ocrEngineMode = OEM_LSTM_ONLY;
    private String tessdataPath = "tessdata";
    private String whitelist = "";
    private String blacklist = "";
    private int dpi = 300;
    private boolean preserveInterwordSpaces = true;

    // Keywords
    private String empfaengerKeywords = "Empfänger,Zahlungsempfänger,Rechnungsempfänger,Kontoinhaber,"
        + "Begünstigter,Name:,Name des Empfängers,Firma,Firma:,Unternehmen,"
        + "Rechnungssteller,Gläubiger,Kunde,Kunde:,Account Holder,Beneficiary,Recipient,"
        + "Rechnung an,Invoice to,An:,An Frau,An Herr,"
        + "Für:,Für Frau,Für Herr,Bei:,Bei Frau,Bei Herr,"
        + "Lieferung an,Versand an";

    private String verwendungszweckKeywords = "Verwendungszweck,Verw.Zweck,Zweck,Purpose,Reference,"
        + "Referenz,Betreff,Rechnungsnr,Rechnungs-Nr,Invoice No,"
        + "Kundenreferenz,Leistungszeitraum,Auftragsnummer,"
        + "Rechnung Nr.,Invoice No.,Kundennummer,Vertragsnummer";

    // QR/Webcam settings
    private String webcamSource = "local"; // "local" or "ip"
    private int localDeviceIndex = 0;
    private String ipProtocol = "http"; // http, https, rtsp
    private String ipAddress = "";
    private int ipPort = 8080;
    private String ipPath = "/video"; // URL-Pfad (z.B. /video, /mjpg/video.mjpg)
    private String ipUsername = "";
    private String ipPassword = "";
    private int ipTimeout = 20;
    private int rotation = 0; // 0, 90, 180, 270

    private OcrSettings() {
        load();
    }

    public static synchronized OcrSettings getInstance() {
        if (instance == null) {
            instance = new OcrSettings();
        }
        return instance;
    }

    private String getSettingsFile() {
        String jameicaHome = System.getProperty("jameica.home",
            System.getProperty("user.dir", "."));
        return jameicaHome + File.separator + SETTINGS_FILE;
    }

    public void load() {
        File file = new File(getSettingsFile());
        if (!file.exists()) {
            logger.info("Keine Einstellungen gefunden, verwende Defaults");
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
            // OCR settings
            language = props.getProperty("ocr.language", language);
            pageSegmentationMode = props.getProperty("ocr.psm", pageSegmentationMode);
            ocrEngineMode = props.getProperty("ocr.oem", ocrEngineMode);
            tessdataPath = props.getProperty("ocr.tessdata", tessdataPath);
            whitelist = props.getProperty("ocr.whitelist", whitelist);
            blacklist = props.getProperty("ocr.blacklist", blacklist);
            dpi = Integer.parseInt(props.getProperty("ocr.dpi", String.valueOf(dpi)));
            preserveInterwordSpaces = Boolean.parseBoolean(
                props.getProperty("ocr.preserveSpaces", String.valueOf(preserveInterwordSpaces)));
            empfaengerKeywords = props.getProperty("ocr.empfaengerKeywords", empfaengerKeywords);
            verwendungszweckKeywords = props.getProperty("ocr.verwendungszweckKeywords", verwendungszweckKeywords);
            // QR/Webcam settings
            webcamSource = props.getProperty("webcam.source", webcamSource);
            localDeviceIndex = Integer.parseInt(props.getProperty("webcam.local.deviceIndex", String.valueOf(localDeviceIndex)));
            ipProtocol = props.getProperty("webcam.ip.protocol", ipProtocol);
            ipAddress = props.getProperty("webcam.ip.address", ipAddress);
            ipPort = Integer.parseInt(props.getProperty("webcam.ip.port", String.valueOf(ipPort)));
            ipPath = props.getProperty("webcam.ip.path", ipPath);
            ipUsername = props.getProperty("webcam.ip.username", ipUsername);
            ipPassword = props.getProperty("webcam.ip.password", ipPassword);
            ipTimeout = Integer.parseInt(props.getProperty("webcam.ip.timeout", String.valueOf(ipTimeout)));
            rotation = Integer.parseInt(props.getProperty("webcam.rotation", String.valueOf(rotation)));
            logger.info("Einstellungen geladen: " + file.getAbsolutePath());
        } catch (Exception e) {
            logger.warning("Fehler beim Laden der Einstellungen: " + e.getMessage());
        }
    }

    public void save() {
        Properties props = new Properties();
        // OCR settings
        props.setProperty("ocr.language", language);
        props.setProperty("ocr.psm", pageSegmentationMode);
        props.setProperty("ocr.oem", ocrEngineMode);
        props.setProperty("ocr.tessdata", tessdataPath);
        props.setProperty("ocr.whitelist", whitelist);
        props.setProperty("ocr.blacklist", blacklist);
        props.setProperty("ocr.dpi", String.valueOf(dpi));
        props.setProperty("ocr.preserveSpaces", String.valueOf(preserveInterwordSpaces));
        props.setProperty("ocr.empfaengerKeywords", empfaengerKeywords);
        props.setProperty("ocr.verwendungszweckKeywords", verwendungszweckKeywords);
        // QR/Webcam settings
        props.setProperty("webcam.source", webcamSource);
        props.setProperty("webcam.local.deviceIndex", String.valueOf(localDeviceIndex));
        props.setProperty("webcam.ip.protocol", ipProtocol);
        props.setProperty("webcam.ip.address", ipAddress);
        props.setProperty("webcam.ip.port", String.valueOf(ipPort));
        props.setProperty("webcam.ip.path", ipPath);
        props.setProperty("webcam.ip.username", ipUsername);
        props.setProperty("webcam.ip.password", ipPassword);
        props.setProperty("webcam.ip.timeout", String.valueOf(ipTimeout));
        props.setProperty("webcam.rotation", String.valueOf(rotation));

        File file = new File(getSettingsFile());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "OCR und QR/Webcam Einstellungen");
            logger.info("Einstellungen gespeichert: " + file.getAbsolutePath());
        } catch (Exception e) {
            logger.warning("Fehler beim Speichern der Einstellungen: " + e.getMessage());
        }
    }

    // Getters and Setters

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getPageSegmentationMode() { return pageSegmentationMode; }
    public void setPageSegmentationMode(String psm) { this.pageSegmentationMode = psm; }

    public String getOcrEngineMode() { return ocrEngineMode; }
    public void setOcrEngineMode(String oem) { this.ocrEngineMode = oem; }

    public String getTessdataPath() { return tessdataPath; }
    public void setTessdataPath(String path) { this.tessdataPath = path; }

    public String getWhitelist() { return whitelist; }
    public void setWhitelist(String whitelist) { this.whitelist = whitelist; }

    public String getBlacklist() { return blacklist; }
    public void setBlacklist(String blacklist) { this.blacklist = blacklist; }

    public int getDpi() { return dpi; }
    public void setDpi(int dpi) { this.dpi = dpi; }

    public boolean isPreserveInterwordSpaces() { return preserveInterwordSpaces; }
    public void setPreserveInterwordSpaces(boolean preserve) { this.preserveInterwordSpaces = preserve; }

    public String getEmpfaengerKeywords() { return empfaengerKeywords; }
    public void setEmpfaengerKeywords(String keywords) { this.empfaengerKeywords = keywords; }

    public String[] getEmpfaengerKeywordsArray() {
        if (empfaengerKeywords == null || empfaengerKeywords.trim().isEmpty()) {
            return new String[0];
        }
        String[] result = empfaengerKeywords.split(",");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }
        return result;
    }

    public String getVerwendungszweckKeywords() { return verwendungszweckKeywords; }
    public void setVerwendungszweckKeywords(String keywords) { this.verwendungszweckKeywords = keywords; }

    public String[] getVerwendungszweckKeywordsArray() {
        if (verwendungszweckKeywords == null || verwendungszweckKeywords.trim().isEmpty()) {
            return new String[0];
        }
        String[] result = verwendungszweckKeywords.split(",");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }
        return result;
    }

    // QR/Webcam getters and setters

    public String getWebcamSource() { return webcamSource; }
    public void setWebcamSource(String source) { this.webcamSource = source; }

    public int getLocalDeviceIndex() { return localDeviceIndex; }
    public void setLocalDeviceIndex(int index) { this.localDeviceIndex = index; }

    public String getIpProtocol() { return ipProtocol; }
    public void setIpProtocol(String protocol) { this.ipProtocol = protocol; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String address) { this.ipAddress = address; }

    public int getIpPort() { return ipPort; }
    public void setIpPort(int port) { this.ipPort = port; }

    public String getIpPath() { return ipPath; }
    public void setIpPath(String path) { this.ipPath = path; }

    public String getIpUsername() { return ipUsername; }
    public void setIpUsername(String username) { this.ipUsername = username; }

    public String getIpPassword() { return ipPassword; }
    public void setIpPassword(String password) { this.ipPassword = password; }

    public int getIpTimeout() { return ipTimeout; }
    public void setIpTimeout(int timeout) { this.ipTimeout = timeout; }

    public int getRotation() { return rotation; }
    public void setRotation(int rotation) { this.rotation = rotation; }

    /**
     * Build the full IP camera URL from settings.
     * @return URL string like "http://192.168.1.100:8080/video" or null if not configured
     */
    public String getIpCameraUrl() {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return null;
        }
        StringBuilder url = new StringBuilder();
        url.append(ipProtocol).append("://").append(ipAddress.trim());
        if (ipPort > 0 && ipPort != 80) {
            url.append(":").append(ipPort);
        }
        if (ipPath != null && !ipPath.trim().isEmpty()) {
            String path = ipPath.trim();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            url.append(path);
        }
        return url.toString();
    }
}
