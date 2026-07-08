package de.willuhn.jameica.hbci.datatransfer.parser;

/**
 * Exception fuer Fehler beim Parsen von QR-Codes.
 */
public class ParserException extends Exception {

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
