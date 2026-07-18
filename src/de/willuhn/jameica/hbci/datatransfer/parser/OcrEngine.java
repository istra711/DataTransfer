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
        // On macOS ARM, tess4j needs the Tesseract native library installed via Homebrew
        // Set jna.library.path so JNA can find libtesseract.dylib
        String osName = System.getProperty("os.name", "").toLowerCase();
        String osArch = System.getProperty("os.arch", "").toLowerCase();
        if (osName.contains("mac") && osArch.contains("aarch64")) {
            String[] homebrewPaths = {"/opt/homebrew/lib", "/usr/local/lib"};
            String currentJnaPath = System.getProperty("jna.library.path", "");
            StringBuilder newPath = new StringBuilder(currentJnaPath);
            for (String path : homebrewPaths) {
                File libDir = new File(path);
                File tesseractLib = new File(libDir, "libtesseract.dylib");
                if (tesseractLib.exists()) {
                    logger.info("Found Tesseract native library: " + tesseractLib.getAbsolutePath());
                    if (newPath.length() > 0) newPath.append(File.pathSeparator);
                    newPath.append(path);
                }
            }
            if (newPath.length() > 0) {
                System.setProperty("jna.library.path", newPath.toString());
                logger.info("jna.library.path set to: " + newPath.toString());
            } else {
                logger.warning("Tesseract native library NOT FOUND! Please install: brew install tesseract");
                logger.warning("Searched in: /opt/homebrew/lib, /usr/local/lib");
            }
        }

        try {
            Class<?> tessClass = Class.forName("net.sourceforge.tess4j.Tesseract");
            tessInstance = tessClass.getConstructor().newInstance();
            available = true;
            logger.info("tess4j erfolgreich geladen");

            // Log native library info for diagnostics
            try {
                Class<?> utilsClass = Class.forName("net.sourceforge.tess4j.util.LibLoader");
                logger.info("tess4j LibLoader class found");
            } catch (ClassNotFoundException e) {
                logger.info("tess4j LibLoader class not found (using bundled natives)");
            }

            // Log OS/arch info
            logger.info("OS: " + osName + " " + osArch);
            logger.info("java.library.path: " + System.getProperty("java.library.path"));
            logger.info("jna.library.path: " + System.getProperty("jna.library.path"));
        } catch (Exception e) {
            logger.warning("tess4j nicht verfuegbar: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                logger.warning("  Ursache: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            available = false;
        }
    }

    public void init(String datapath, String language) {
        if (!available || tessInstance == null) return;

        try {
            // Resolve tessdata path relative to plugin directory if it's relative
            String resolvedPath = datapath;
            if (!new File(datapath).isAbsolute()) {
                String pluginDir = null;

                // Strategy 1: Use ProtectionDomain to find the JAR this class was loaded from
                try {
                    java.security.CodeSource cs = OcrEngine.class.getProtectionDomain().getCodeSource();
                    if (cs != null && cs.getLocation() != null) {
                        File jarFile = new File(cs.getLocation().toURI());
                        File parentDir = jarFile.getParentFile();
                        if (parentDir != null && parentDir.exists()) {
                            pluginDir = parentDir.getAbsolutePath();
                            logger.info("Resolved plugin dir via CodeSource: " + pluginDir);
                        }
                    }
                } catch (Exception e) {
                    logger.info("CodeSource resolution failed: " + e.getMessage());
                }

                // Strategy 2: Try Jameica's PluginLoader API via reflection
                if (pluginDir == null) {
                    try {
                        Class<?> appClass = Class.forName("de.willuhn.jameica.system.Application");
                        java.lang.reflect.Method getPluginLoader = appClass.getMethod("getPluginLoader");
                        Object pluginLoader = getPluginLoader.invoke(null);
                        java.lang.reflect.Method getPlugin = pluginLoader.getClass().getMethod("getPlugin", String.class);
                        Object plugin = getPlugin.invoke(pluginLoader, "de.willuhn.jameica.hbci.datatransfer.DataTransferPlugin");
                        java.lang.reflect.Method getManifest = plugin.getClass().getMethod("getManifest");
                        Object manifest = getManifest.invoke(plugin);
                        // Try multiple method names for Jameica's API
                        java.lang.reflect.Method getPathMethod = null;
                        for (String name : new String[]{"getInstallPath", "getPath", "getPluginDir"}) {
                            try {
                                getPathMethod = manifest.getClass().getMethod(name);
                                break;
                            } catch (NoSuchMethodException ignored) {}
                        }
                        if (getPathMethod != null) {
                            Object path = getPathMethod.invoke(manifest);
                            pluginDir = path.toString();
                            logger.info("Resolved plugin dir via Jameica API: " + pluginDir);
                        }
                    } catch (Exception e) {
                        logger.info("Jameica API resolution failed: " + e.getMessage());
                    }
                }

                // Strategy 3: Search known Jameica plugin directories
                if (pluginDir == null) {
                    String jameicaHome = System.getProperty("jameica.home",
                        System.getProperty("user.dir", "."));
                    // The plugin directory name uses the historical typo: datatansfer
                    String[] candidates = new String[]{
                        jameicaHome + "/plugins/hbci.datatansfer",
                        jameicaHome + "/plugins/hbci.datatransfer",
                        jameicaHome + "/Contents/Resources/Java/plugins/hbci.datatansfer",
                        jameicaHome + "/Contents/Resources/Java/plugins/hbci.datatransfer"
                    };
                    for (String candidate : candidates) {
                        File f = new File(candidate);
                        if (f.exists() && f.isDirectory()) {
                            pluginDir = candidate;
                            logger.info("Resolved plugin dir via candidate search: " + pluginDir);
                            break;
                        }
                    }
                }

                if (pluginDir != null) {
                    resolvedPath = pluginDir + File.separator + datapath;
                } else {
                    logger.warning("Could not resolve plugin directory, using datapath as-is: " + datapath);
                    resolvedPath = datapath;
                }
            }
            
            Method setDatapath = tessInstance.getClass().getMethod("setDatapath", String.class);
            setDatapath.invoke(tessInstance, resolvedPath);

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

            logger.info("Tesseract initialisiert: datapath=" + resolvedPath + ", language=" + language);
        } catch (Exception e) {
            logger.warning("Fehler bei Tesseract-Initialisierung: " + e.getMessage());
        }
    }

    public String doOCR(BufferedImage image) {
        if (!available || tessInstance == null) {
            logger.warning("OCR nicht verfügbar: available=" + available + ", tessInstance=" + (tessInstance != null));
            return null;
        }

        try {
            File tempFile = File.createTempFile("ocr_", ".png");
            javax.imageio.ImageIO.write(image, "png", tempFile);
            logger.info("OCR: Temp-Datei erstellt: " + tempFile.getAbsolutePath() + " (" + tempFile.length() + " bytes)");

            Method doOCR = tessInstance.getClass().getMethod("doOCR", File.class);
            String result = (String) doOCR.invoke(tessInstance, tempFile);

            tempFile.delete();
            logger.info("OCR: Ergebnis " + (result != null ? result.length() + " Zeichen" : "null"));
            return result;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            logger.warning("OCR fehlgeschlagen (InvocationTargetException): " + 
                (cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : e.getMessage()));
            if (cause != null && cause.getCause() != null) {
                logger.warning("  Ursache: " + cause.getCause().getClass().getName() + ": " + cause.getCause().getMessage());
            }
            if (cause != null) {
                cause.printStackTrace();
            }
            return null;
        } catch (Exception e) {
            logger.warning("OCR fehlgeschlagen: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                logger.warning("  Ursache: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            e.printStackTrace();
            return null;
        }
    }

    public String doOCR(File file) {
        if (!available || tessInstance == null) {
            logger.warning("OCR nicht verfügbar: available=" + available + ", tessInstance=" + (tessInstance != null));
            return null;
        }

        try {
            Method doOCR = tessInstance.getClass().getMethod("doOCR", File.class);
            return (String) doOCR.invoke(tessInstance, file);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            logger.warning("OCR fehlgeschlagen (InvocationTargetException): " + 
                (cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : e.getMessage()));
            if (cause != null && cause.getCause() != null) {
                logger.warning("  Ursache: " + cause.getCause().getClass().getName() + ": " + cause.getCause().getMessage());
            }
            if (cause != null) {
                cause.printStackTrace();
            }
            return null;
        } catch (Exception e) {
            logger.warning("OCR fehlgeschlagen: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                logger.warning("  Ursache: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
            }
            e.printStackTrace();
            return null;
        }
    }

    public boolean isAvailable() {
        return available;
    }
}
