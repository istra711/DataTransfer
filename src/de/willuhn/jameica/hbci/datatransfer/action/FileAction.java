package de.willuhn.jameica.hbci.datatransfer.action;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.hbci.datatransfer.gui.InvoiceView;
import de.willuhn.jameica.hbci.datatransfer.gui.QRCodeView;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.hbci.datatransfer.parser.SmartDetector;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

import java.io.File;
import java.util.logging.Logger;

/**
 * Action zum Laden einer Datei (PDF oder Bild) mit automatischer Erkennung.
 */
public class FileAction implements Action {

    private static final Logger logger = Logger.getLogger(FileAction.class.getName());
    private static I18N i18n;

    private static synchronized I18N getI18n() {
        if (i18n == null) {
            i18n = Application.getPluginLoader()
                .getPlugin("de.willuhn.jameica.hbci.datatransfer.DataTransferPlugin")
                .getResources()
                .getI18N();
        }
        return i18n;
    }

    @Override
    public void handleAction(Object context) throws ApplicationException {
        final I18N i = getI18n();
        try {
            org.eclipse.swt.widgets.FileDialog dialog =
                new org.eclipse.swt.widgets.FileDialog(GUI.getShell(), org.eclipse.swt.SWT.OPEN);
            dialog.setText(i.tr("select.file"));
            dialog.setFilterExtensions(new String[]{
                "*.pdf;*.png;*.jpg;*.jpeg;*.bmp;*.tiff",
                "*.pdf",
                "*.png;*.jpg;*.jpeg;*.bmp;*.tiff"
            });
            dialog.setFilterNames(new String[]{
                i.tr("filter.all.files"),
                i.tr("filter.pdf.files"),
                i.tr("filter.image.files")
            });

            String path = dialog.open();
            if (path == null || path.isEmpty()) return;

            File file = new File(path);
            if (!file.exists()) {
                throw new ApplicationException(i.tr("error.file.not.found", path));
            }

            TransferData data = SmartDetector.detectFromFile(file);

            if (data == null || data.getRawText() == null || data.getRawText().isEmpty()) {
                throw new ApplicationException(i.tr("error.no.text.extracted"));
            }

            logger.info("Daten erkannt: " + data.toString());

            if (data.getSource() == TransferData.Source.QR_EPC ||
                data.getSource() == TransferData.Source.QR_EMV) {
                GUI.startView(QRCodeView.class, data);
            } else {
                GUI.startView(InvoiceView.class, data);
            }

        } catch (ApplicationException e) {
            logger.warning("ApplicationException: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.warning("Exception: " + e.getClass().getName() + ": " + e.getMessage());
            throw new ApplicationException(
                i.tr("error.reading.file", e.getMessage()), e
            );
        }
    }
}
