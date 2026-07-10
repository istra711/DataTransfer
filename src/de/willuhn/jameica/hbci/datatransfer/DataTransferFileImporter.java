package de.willuhn.jameica.hbci.datatransfer;

import java.io.InputStream;

import de.willuhn.util.ProgressMonitor;
import de.willuhn.jameica.hbci.datatransfer.parser.SmartDetector;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData;

/**
 * Importer fuer Datei-Import.
 */
public class DataTransferFileImporter extends DataTransferBaseImporter {

    @Override
    protected String getMode() {
        return "file";
    }

    @Override
    protected String getDisplayName() {
        return "Rechnungs-Datei(PDF/Image) - OCR/QR";
    }

    @Override
    protected TransferData processInput(InputStream is, ProgressMonitor monitor, SmartDetector detector) throws Exception {
        return detector.detectFromStream(is, monitor);
    }
}
