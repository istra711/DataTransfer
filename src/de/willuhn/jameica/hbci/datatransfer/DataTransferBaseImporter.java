package de.willuhn.jameica.hbci.datatransfer;

import java.io.InputStream;
import java.rmi.RemoteException;

import de.willuhn.jameica.hbci.gui.dialogs.KontoAuswahlDialog;
import de.willuhn.jameica.hbci.io.Importer;
import de.willuhn.jameica.hbci.io.IOFormat;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.Ueberweisung;
import de.willuhn.logging.Logger;
import de.willuhn.util.ProgressMonitor;
import de.willuhn.jameica.system.BackgroundTask;
import de.willuhn.util.ApplicationException;

/**
 * Basis-Importer fuer DataTransfer.
 */
public abstract class DataTransferBaseImporter implements Importer {

    protected abstract String getMode();
    protected abstract String getDisplayName();

    @Override
    public String getName() {
        return getDisplayName();
    }

    @Override
    public IOFormat[] getIOFormats(Class type) {
        if (type == null) {
            return new IOFormat[0];
        }
        if (!Ueberweisung.class.isAssignableFrom(type) && !de.willuhn.jameica.hbci.rmi.AuslandsUeberweisung.class.isAssignableFrom(type)) {
            return new IOFormat[0];
        }

        return new IOFormat[] { new IOFormat() {
            @Override
            public String getName() {
                return getDisplayName();
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
            monitor.setStatusText("Konto wird ausgewaehlt...");
            monitor.setPercentComplete(5);

            Konto konto = null;

            if (target != null && target instanceof Konto) {
                konto = (Konto) target;
            } else {
                KontoAuswahlDialog d = new KontoAuswahlDialog(KontoAuswahlDialog.POSITION_CENTER);
                d.setText("Bitte waehlen Sie das Konto fuer den Import aus.");
                konto = (Konto) d.open();
            }

            if (konto == null) {
                throw new ApplicationException("Kein Konto ausgewaehlt");
            }

            Logger.info("Daten-Transfer Import (" + getMode() + "): Konto " + konto.getKontonummer());

            monitor.setStatusText("Daten werden erkannt...");
            monitor.setPercentComplete(20);

            de.willuhn.jameica.hbci.datatransfer.parser.SmartDetector detector =
                new de.willuhn.jameica.hbci.datatransfer.parser.SmartDetector();

            de.willuhn.jameica.hbci.datatransfer.model.TransferData transferData = processInput(is, monitor, detector);

            if (transferData == null) {
                throw new ApplicationException("Keine gueltigen Zahlungsdaten gefunden");
            }

            boolean isQr = transferData.getSource() == de.willuhn.jameica.hbci.datatransfer.model.TransferData.Source.QR_EPC
                       || transferData.getSource() == de.willuhn.jameica.hbci.datatransfer.model.TransferData.Source.QR_EMV;
            Logger.info("Daten-Transfer: " + (isQr ? "QR-Code" : "OCR") + " erkannt");

            monitor.setStatusText("Vorschau wird geoeffnet...");
            monitor.setPercentComplete(90);

            final de.willuhn.jameica.hbci.datatransfer.model.TransferDataHolder holder =
                new de.willuhn.jameica.hbci.datatransfer.model.TransferDataHolder(transferData, konto);

            if (isQr) {
                de.willuhn.jameica.gui.GUI.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        try {
                            de.willuhn.jameica.gui.GUI.startView(
                                de.willuhn.jameica.hbci.datatransfer.gui.QRCodeView.class, holder);
                        } catch (Exception e) {
                            Logger.error("Fehler beim Oeffnen der QR-Code-Ansicht", e);
                        }
                    }
                });
            } else {
                de.willuhn.jameica.gui.GUI.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        try {
                            de.willuhn.jameica.gui.GUI.startView(
                                de.willuhn.jameica.hbci.datatransfer.gui.InvoiceView.class, holder);
                        } catch (Exception e) {
                            Logger.error("Fehler beim Oeffnen der OCR-Ansicht", e);
                        }
                    }
                });
            }

            monitor.setStatusText("Fertig");
            monitor.setPercentComplete(100);

        } catch (RemoteException e) {
            throw e;
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            Logger.error("Fehler beim Import", e);
            throw new ApplicationException("Fehler beim Import: " + e.getMessage());
        }
    }

    protected abstract de.willuhn.jameica.hbci.datatransfer.model.TransferData processInput(InputStream is, ProgressMonitor monitor, de.willuhn.jameica.hbci.datatransfer.parser.SmartDetector detector) throws Exception;
}
