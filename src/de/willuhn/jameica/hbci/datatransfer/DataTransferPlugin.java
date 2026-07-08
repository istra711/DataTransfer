package de.willuhn.jameica.hbci.datatransfer;

import java.util.logging.Logger;

import de.willuhn.jameica.plugin.AbstractPlugin;
import de.willuhn.jameica.system.Application;

/**
 * DataTransfer Plugin fuer Jameica/Hibiscus.
 * Registriert den Importer per Reflection in der IORegistry.
 */
public class DataTransferPlugin extends AbstractPlugin {

    private static final Logger logger = Logger.getLogger(DataTransferPlugin.class.getName());

    @Override
    public void init() throws de.willuhn.util.ApplicationException {
        super.init();
        logger.info("DataTransfer Plugin initialisiert - starte Importer-Registrierung");
        
        // Importer-Registrierung in einem Background-Thread ausfuehren,
        // damit die IORegistry bereits initialisiert sein kann
        Thread registrationThread = new Thread(() -> {
            try {
                // Warten bis IORegistry geladen ist (max. 30 Sekunden)
                for (int i = 0; i < 60; i++) {
                    try {
                        Class<?> ioRegistryClass = Class.forName("de.willuhn.jameica.hbci.io.IORegistry");
                        Object importers = ioRegistryClass.getMethod("getImporters").invoke(null);
                        if (importers != null && ((Object[]) importers).length > 0) {
                            logger.info("IORegistry ist geladen, fuege DataTransfer Importer hinzu...");
                            registerImporter();
                            return;
                        }
                    } catch (ClassNotFoundException e) {
                        // IORegistry noch nicht geladen
                    }
                    Thread.sleep(500);
                }
                logger.warning("IORegistry konnte nicht innerhalb von 30s geladen werden");
            } catch (Exception e) {
                logger.warning("Fehler bei der Importer-Registrierung: " + e.getMessage());
            }
        }, "DataTransfer-Importer-Registration");
        registrationThread.setDaemon(true);
        registrationThread.start();
    }

    /**
     * Registriert unseren Importer in der IORegistry.
     */
    @SuppressWarnings("unchecked")
    private void registerImporter() {
        try {
            // IORegistry importers-Feld holen
            Class<?> ioRegistryClass = Class.forName("de.willuhn.jameica.hbci.io.IORegistry");
            java.lang.reflect.Field importersField = ioRegistryClass.getDeclaredField("importers");
            importersField.setAccessible(true);
            java.util.List<Object> importers = (java.util.List<Object>) importersField.get(null);
            
            if (importers == null) {
                logger.warning("IORegistry.importers ist null");
                return;
            }
            
            // Importer-Proxy erstellen
            Object importer = de.willuhn.jameica.hbci.datatransfer.io.DataTransferImporterProxy.createImporter();
            if (importer == null) {
                logger.warning("Konnte Importer-Proxy nicht erstellen");
                return;
            }
            
            // Pruefen ob bereits registriert
            String name = getImporterName(importer);
            for (Object existing : importers) {
                String existingName = getImporterName(existing);
                if (name != null && name.equals(existingName)) {
                    logger.info("DataTransferImporter bereits registriert: " + name);
                    return;
                }
            }
            
            // Importer hinzufuegen
            importers.add(importer);
            logger.info("DataTransferImporter in IORegistry registriert: " + name);
            
        } catch (Exception e) {
            logger.warning("Fehler beim Registrieren des Importers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ruft getName() auf einem Importer auf.
     */
    private String getImporterName(Object importer) {
        try {
            java.lang.reflect.Method getNameMethod = importer.getClass().getMethod("getName");
            Object result = getNameMethod.invoke(importer);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
