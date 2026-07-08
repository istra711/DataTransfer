package de.willuhn.jameica.hbci.datatransfer.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ProgressMonitor;

/**
 * Leichtgewichtiger Importer-Proxy.
 * Implementiert Importer zur Laufzeit via Reflection, damit die Klasse
 * auch geladen werden kann, wenn Hibiscus-Klassen noch nicht verfuegbar sind.
 */
public class DataTransferImporterProxy {

    private static final Logger logger = Logger.getLogger(DataTransferImporterProxy.class.getName());

    /**
     * Erstellt eine Instanz die das Importer-Interface zur Laufzeit implementiert.
     */
    public static Object createImporter() {
        try {
            // Importer-Klasse zur Laufzeit laden
            Class<?> importerClass = Class.forName("de.willuhn.jameica.hbci.io.Importer");
            Class<?> ioFormatClass = Class.forName("de.willuhn.jameica.hbci.io.IOFormat");
            
            // Dynamischer Proxy der Importer implementiert
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                DataTransferImporterProxy.class.getClassLoader(),
                new Class<?>[] { importerClass },
                (proxyObj, method, args) -> handleMethod(method, args)
            );
            
            return proxy;
        } catch (Exception e) {
            logger.warning("Konnte Importer-Proxy nicht erstellen: " + e.getMessage());
            return null;
        }
    }

    /**
     * Behandelt Methodenaufrufe des Importer-Interface.
     */
    private static Object handleMethod(Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        
        if ("getName".equals(methodName)) {
            return "OCR/QR-Code-Import (DataTransfer)";
        }
        
        if ("getIOFormats".equals(methodName)) {
            return getIOFormats((Class<?>) args[0]);
        }
        
        if ("doImport".equals(methodName)) {
            doImport(args[0], args[1], (InputStream) args[2], 
                     (ProgressMonitor) args[3], (BackgroundTask) args[4]);
            return null;
        }
        
        return null;
    }

    /**
     * Gibt die unterstuetzten Formate zurueck.
     */
    private static Object[] getIOFormats(Class<?> type) throws Exception {
        if (type == null) return new Object[0];
        
        Class<?> auslandsUeberweisungClass = Class.forName("de.willuhn.jameica.hbci.rmi.AuslandsUeberweisung");
        if (!auslandsUeberweisungClass.isAssignableFrom(type)) {
            return new Object[0];
        }
        
        Class<?> ioFormatClass = Class.forName("de.willuhn.jameica.hbci.io.IOFormat");
        Object format = java.lang.reflect.Proxy.newProxyInstance(
            DataTransferImporterProxy.class.getClassLoader(),
            new Class<?>[] { ioFormatClass },
            (p, m, a) -> {
                if ("getName".equals(m.getName())) return "PDF mit QR-Code oder OCR-Text";
                if ("getFileExtensions".equals(m.getName())) return new String[] { "*.pdf" };
                return null;
            }
        );
        
        return new Object[] { format };
    }

    /**
     * Fuehrt den Import durch.
     */
    private static void doImport(Object context, Object format, InputStream is,
                                 ProgressMonitor monitor, BackgroundTask task)
            throws RemoteException, ApplicationException {
        
        logger.info("DataTransfer-Import gestartet");
        
        if (monitor != null) {
            monitor.setStatusText("Analysiere Datei...");
        }
        
        try {
            File tempFile = File.createTempFile("datatransfer_", ".pdf");
            tempFile.deleteOnExit();
            
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            
            Class<?> smartDetectorClass = Class.forName("de.willuhn.jameica.hbci.datatransfer.parser.SmartDetector");
            Object data = smartDetectorClass.getMethod("detectFromFile", File.class).invoke(null, tempFile);
            
            tempFile.delete();
            
            if (data == null) {
                throw new ApplicationException(
                    "Kein gueltiger QR-Code oder OCR-Text in der Datei gefunden.");
            }
            
            logger.info("Daten erkannt: " + data);
            
            if (monitor != null) {
                monitor.setStatusText("Daten erkannt, oeffne Formular...");
            }
            
            Object source = data.getClass().getMethod("getSource").invoke(data);
            String sourceName = source.toString();
            
            String viewClassName;
            if (sourceName.contains("QR_EPC") || sourceName.contains("QR_EMV")) {
                viewClassName = "de.willuhn.jameica.hbci.datatransfer.gui.QRCodeView";
            } else {
                viewClassName = "de.willuhn.jameica.hbci.datatransfer.gui.InvoiceView";
            }
            
            Class<?> viewClazz = Class.forName(viewClassName);
            Class<?> guiClass = Class.forName("de.willuhn.jameica.gui.GUI");
            guiClass.getMethod("startView", Class.class, Object.class).invoke(null, viewClazz, data);
            
            if (monitor != null) {
                monitor.setPercentComplete(100);
                monitor.setStatus(ProgressMonitor.STATUS_DONE);
            }
            
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            logger.warning("Fehler beim Import: " + e.getMessage());
            throw new ApplicationException("Fehler beim Import: " + e.getMessage(), e);
        }
    }
}
