package de.willuhn.jameica.hbci.datatransfer.parser;

import de.willuhn.jameica.hbci.datatransfer.model.TransferData;

/**
 * Interface fuer SEPA-QR-Code-Parser.
 */
public interface QrCodeParser {

    /**
     * Prueft, ob der gegebene Text von diesem Parser verarbeitet werden kann.
     * @param text der geparste QR-Code-Text
     * @return true, wenn das Format erkannt wurde
     */
    boolean canParse(String text);

    /**
     * Parst den QR-Code-Text und gibt die TransferData zurueck.
     * @param text der geparste QR-Code-Text
     * @return die geparsten Daten
     * @throws ParserException wenn das Parsen fehlschlaegt
     */
    TransferData parse(String text) throws ParserException;
}
