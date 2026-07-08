package de.willuhn.jameica.hbci.datatransfer.model;

/**
 * Vereinheitlichtes Datenmodell fuer SEPA-Ueberweisungsdaten.
 * Zusammenfuehrung aus InvoiceData (OCRtransfer) und SepaData (QRtransfer).
 */
public class TransferData {

    public enum Source { QR_EPC, QR_EMV, OCR, UNKNOWN }

    private String iban;
    private String bic;
    private String empfaengerName;
    private String empfaengerStrasse;
    private String empfaengerOrt;
    private double betrag;
    private String waehrung;
    private String betreff;
    private String verwendungszweck;
    private String rechnungsnummer;
    private String kunde;
    private String rawText;
    private String quelle;
    private boolean ocrVerwendet;
    private Source source;
    private String format;

    public TransferData() {
        this.waehrung = "EUR";
        this.source = Source.UNKNOWN;
    }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban != null ? iban.trim().toUpperCase() : null; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic != null ? bic.trim().toUpperCase() : null; }

    public String getEmpfaengerName() { return empfaengerName; }
    public void setEmpfaengerName(String empfaengerName) { this.empfaengerName = empfaengerName; }

    public String getEmpfaengerStrasse() { return empfaengerStrasse; }
    public void setEmpfaengerStrasse(String empfaengerStrasse) { this.empfaengerStrasse = empfaengerStrasse; }

    public String getEmpfaengerOrt() { return empfaengerOrt; }
    public void setEmpfaengerOrt(String empfaengerOrt) { this.empfaengerOrt = empfaengerOrt; }

    public double getBetrag() { return betrag; }
    public void setBetrag(double betrag) { this.betrag = betrag; }
    public void setBetrag(String betragStr) {
        if (betragStr != null && !betragStr.isEmpty()) {
            try {
                this.betrag = Double.parseDouble(betragStr.replace(",", "."));
            } catch (NumberFormatException e) {
                this.betrag = 0.0;
            }
        }
    }

    public String getWaehrung() { return waehrung; }
    public void setWaehrung(String waehrung) { this.waehrung = waehrung; }

    public String getBetreff() { return betreff; }
    public void setBetreff(String betreff) { this.betreff = betreff; }

    public String getVerwendungszweck() { return verwendungszweck; }
    public void setVerwendungszweck(String verwendungszweck) { this.verwendungszweck = verwendungszweck; }

    public String getRechnungsnummer() { return rechnungsnummer; }
    public void setRechnungsnummer(String rechnungsnummer) { this.rechnungsnummer = rechnungsnummer; }

    public String getKunde() { return kunde; }
    public void setKunde(String kunde) { this.kunde = kunde; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getQuelle() { return quelle; }
    public void setQuelle(String quelle) { this.quelle = quelle; }

    public boolean isOcrVerwendet() { return ocrVerwendet; }
    public void setOcrVerwendet(boolean ocrVerwendet) { this.ocrVerwendet = ocrVerwendet; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    /**
     * Gibt den kombinierten Verwendungszweck zurueck.
     * Verwendet Verwendungszweck oder Betreff (was verfuegbar ist).
     */
    public String getZweckFeld() {
        StringBuilder sb = new StringBuilder();
        if (rechnungsnummer != null && !rechnungsnummer.isEmpty()) {
            sb.append("Rechnung Nr. ").append(rechnungsnummer);
        }
        if (kunde != null && !kunde.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(kunde);
        }
        if (verwendungszweck != null && !verwendungszweck.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(verwendungszweck);
        }
        if (sb.length() > 0) return sb.toString();
        if (betreff != null && !betreff.isEmpty()) return betreff;
        return null;
    }

    /**
     * Prueft, ob die Mindestdaten fuer eine Ueberweisung vorhanden sind.
     */
    public boolean isValid() {
        return iban != null && !iban.isEmpty()
            && empfaengerName != null && !empfaengerName.isEmpty();
    }

    @Override
    public String toString() {
        return "TransferData{" +
            "iban='" + iban + '\'' +
            ", bic='" + bic + '\'' +
            ", empfaengerName='" + empfaengerName + '\'' +
            ", betrag=" + betrag +
            ", waehrung='" + waehrung + '\'' +
            ", verwendungszweck='" + getZweckFeld() + '\'' +
            ", source=" + source +
            ", format='" + format + '\'' +
            '}';
    }
}
