package de.willuhn.jameica.hbci.datatransfer;

import de.willuhn.jameica.hbci.io.Importer;
import de.willuhn.jameica.hbci.io.IOFormat;
import de.willuhn.jameica.hbci.rmi.Ueberweisung;

/**
 * IO-Klasse fuer den DataTransfer Importer.
 * Registriert nur den Datei-Importer bei Jameica/Hibiscus.
 * Clipboard und Webcam werden ueber eigene Actions/Menu-Eintraege bedient.
 */
public class DataTransferIO implements de.willuhn.jameica.hbci.io.IO {

    private final Importer fileImporter = new DataTransferFileImporter();

    @Override
    public String getName() {
        return "Daten-Transfer";
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
                return "Rechnungs-Datei(PDF/Image) - OCR/QR";
            }

            @Override
            public String[] getFileExtensions() {
                return new String[] { "pdf", "png", "jpg", "jpeg", "bmp", "tiff", "tif" };
            }
        } };
    }
}
