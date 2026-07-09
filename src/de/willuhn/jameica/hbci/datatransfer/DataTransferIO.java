package de.willuhn.jameica.hbci.datatransfer;

import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Map;

import de.willuhn.jameica.hbci.io.Exporter;
import de.willuhn.jameica.hbci.io.Importer;
import de.willuhn.jameica.hbci.io.IOFormat;
import de.willuhn.jameica.hbci.io.IORegistry;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.Ueberweisung;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ProgressMonitor;
import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.util.ApplicationException;

/**
 * IO-Klasse fuer den DataTransfer Importer.
 * Registriert den Importer bei Jameica/Hibiscus.
 */
public class DataTransferIO implements de.willuhn.jameica.hbci.io.IO {

    private final Importer importer = new DataTransferImporter();

    @Override
    public String getName() {
        return "Daten-Transfer";
    }

    @Override
    public IOFormat[] getIOFormats(Class type) {
        if (type != null && type.equals(Ueberweisung.class)) {
            return new IOFormat[] { new IOFormat() {
                @Override
                public String getName() {
                    return "PDF Rechnung / QR Code / OCR";
                }

                @Override
                public String[] getFileExtensions() {
                    return new String[] { "pdf", "png", "jpg", "jpeg", "bmp", "tiff", "tif" };
                }
            } };
        }
        return new IOFormat[0];
    }

    /**
     * Liefert den Importer zurueck.
     * @return der Importer.
     */
    public Importer[] getImporter() {
        return new Importer[] { importer };
    }

    /**
     * Liefert die Exporter zurueck (leer - wir sind nur Importer).
     * @return leeres Array.
     */
    public Exporter[] getExporter() {
        return new Exporter[0];
    }
}

/**
 * Importer fuer PDF-Rechnungen mit QR-Code oder OCR.
 * Verarbeitet PDFs ueber SmartDetector und erstellt SEPA-Ueberweisungen.
 */
class DataTransferImporter implements Importer {

    @Override
    public String getName() {
        return "Daten-Transfer (PDF/QR/OCR)";
    }

    @Override
    public IOFormat[] getIOFormats(Class type) {
        if (type == null || !type.equals(Ueberweisung.class)) {
            return new IOFormat[0];
        }

        return new IOFormat[] { new IOFormat() {
            @Override
            public String getName() {
                return "PDF Rechnung / QR Code / OCR";
            }

            @Override
            public String[] getFileExtensions() {
                return new String[] { "pdf", "png", "jpg", "jpeg", "bmp", "tiff", "tif" };
            }
        } };
    }

    @Override
    public void doImport(Object target, IOFormat format, InputStream is, ProgressMonitor monitor, BackgroundTask task) throws RemoteException, ApplicationException {
        try {
            // target ist das Konto
            if (target == null) {
                throw new ApplicationException("Kein Konto ausgewaehlt");
            }

            Konto konto = (Konto) target;
            Logger.info("Daten-Transfer Import: Konto " + konto.getKontonummer());

            // SmartDetector starten
            de.willuhn.jameica.hbci.datatransfer.parser.SmartDetector detector =
                new de.willuhn.jameica.hbci.datatransfer.parser.SmartDetector();

            de.willuhn.jameica.hbci.datatransfer.model.TransferData transferData = detector.detectFromStream(is, monitor);

            if (transferData == null) {
                throw new ApplicationException("Keine gueltigen Zahlungsdaten in der Datei gefunden");
            }

            boolean isQr = transferData.getSource() == de.willuhn.jameica.hbci.datatransfer.model.TransferData.Source.QR_EPC
                       || transferData.getSource() == de.willuhn.jameica.hbci.datatransfer.model.TransferData.Source.QR_EMV;
            Logger.info("Daten-Transfer: " + (isQr ? "QR-Code" : "OCR") + " erkannt");

            // Ueberweisung erstellen
            de.willuhn.jameica.hbci.rmi.HBCIDBService dbService = de.willuhn.jameica.hbci.Settings.getDBService();
            Ueberweisung ueberweisung = dbService.createObject(Ueberweisung.class, null);

            if (transferData.getIban() != null) {
                ueberweisung.setGegenkontoNummer(transferData.getIban());
            }
            if (transferData.getBic() != null) {
                ueberweisung.setGegenkontoBLZ(transferData.getBic());
            }
            if (transferData.getEmpfaengerName() != null) {
                ueberweisung.setGegenkontoName(transferData.getEmpfaengerName());
            }
            if (transferData.getBetrag() > 0) {
                ueberweisung.setBetrag(transferData.getBetrag());
            }
            if (transferData.getVerwendungszweck() != null) {
                ueberweisung.setZweck(transferData.getVerwendungszweck());
            }

            ueberweisung.store();
            Logger.info("Daten-Transfer: Ueberweisung gespeichert fuer " + ueberweisung.getGegenkontoName());

        } catch (RemoteException e) {
            throw e;
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            Logger.error("Fehler beim Import", e);
            throw new ApplicationException("Fehler beim Import: " + e.getMessage());
        }
    }
}
