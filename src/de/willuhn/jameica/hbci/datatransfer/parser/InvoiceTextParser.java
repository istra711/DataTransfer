package de.willuhn.jameica.hbci.datatransfer.parser;

import de.willuhn.jameica.hbci.datatransfer.OcrSettings;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData.Source;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-basiertes Parsen von OCR-Texten fuer SEPA-Ueberweisungsdaten.
 */
public class InvoiceTextParser {

    private static final Logger logger = Logger.getLogger(InvoiceTextParser.class.getName());

    private static final Pattern IBAN_PATTERN = Pattern.compile(
        "[A-Z]{2}[ ]?\\d{2}(?:[ \\-]?[A-Z\\d]{4}){4,7}[ \\-]?[A-Z\\d]{1,4}");

    private static final Pattern BIC_PATTERN = Pattern.compile(
        "(?:BIC|SWIFT)\\s*[:\\-]?\\s*([A-Za-z0-9 ]{8,15})");

    private static final Pattern BIC_FALLBACK = Pattern.compile(
        "[A-Za-z]{4}[ ]?[A-Za-z]{2}[ ]?[A-Za-z0-9]{2}(?:[ ]?[A-Za-z0-9]{1,4})?");

    private static final Pattern BETRAG_PATTERN = Pattern.compile(
        "(?:Summe|Gesamtbetrag|Total|Rechnungsbetrag|Betrag|Amount|Total amount)[^\\d]*(\\d{1,6}[.,]\\d{2})");

    private static final Pattern BETRAG_FALLBACK = Pattern.compile(
        "(\\d{1,6}[.,]\\d{2})");

    private static final Pattern ZWECK_PATTERN = Pattern.compile(
        "(?:Rechnung Nr\\.?|Invoice No\\.?|Kundennummer)[^\\d]*(\\d+)");

    public TransferData parse(String text) {
        TransferData data = new TransferData();
        data.setRawText(text);
        data.setSource(Source.OCR);

        data.setIban(extractIban(text));
        data.setBic(extractBic(text));
        data.setEmpfaengerName(extractEmpfaenger(text));
        data.setBetrag(extractBetrag(text));
        data.setVerwendungszweck(extractVerwendungszweck(text));

        return data;
    }

    private String extractIban(String text) {
        Matcher m = IBAN_PATTERN.matcher(text);
        if (m.find()) {
            String iban = m.group().replaceAll("[ \\-]", "").toUpperCase();
            if (iban.length() >= 15 && iban.length() <= 34) {
                return iban;
            }
        }
        return null;
    }

    private String extractBic(String text) {
        Matcher m = BIC_PATTERN.matcher(text);
        if (m.find()) {
            String bic = m.group(1).replaceAll("[ ]", "").toUpperCase();
            if (bic.length() == 8 || bic.length() == 11) {
                return bic;
            }
        }

        m = BIC_FALLBACK.matcher(text);
        while (m.find()) {
            String bic = m.group().replaceAll("[ ]", "").toUpperCase();
            if (bic.length() == 8 || bic.length() == 11) {
                String country = bic.substring(0, 2);
                if ("DE".equals(country) || "AT".equals(country) || "CH".equals(country)) {
                    return bic;
                }
            }
        }
        return null;
    }

    private String extractEmpfaenger(String text) {
        OcrSettings settings = OcrSettings.getInstance();
        String[] keywords = settings.getEmpfaengerKeywordsArray();

        for (String keyword : keywords) {
            int idx = text.indexOf(keyword);
            if (idx >= 0) {
                int start = idx + keyword.length();
                int end = text.indexOf("\n", start);
                if (end < 0) end = text.length();
                String name = text.substring(start, end).trim();
                if (name.length() >= 2 && name.length() <= 100) {
                    return name;
                }
            }
        }

        String[] lines = text.split("\\n");
        for (int i = 0; i < lines.length - 1; i++) {
            String line = lines[i].trim();
            for (String keyword : keywords) {
                if (line.contains(keyword)) {
                    String name = lines[i + 1].trim();
                    if (name.length() >= 2 && name.length() <= 100) {
                        return name;
                    }
                }
            }
        }

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Herr ") || trimmed.startsWith("Frau ") ||
                trimmed.startsWith("Firma ") || trimmed.startsWith("Dr. ")) {
                return trimmed;
            }
        }

        return null;
    }

    private double extractBetrag(String text) {
        Matcher m = BETRAG_PATTERN.matcher(text);
        if (m.find()) {
            String betragStr = m.group(1).replace(",", ".");
            try {
                return Double.parseDouble(betragStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        double maxBetrag = 0;
        m = BETRAG_FALLBACK.matcher(text);
        while (m.find()) {
            String betragStr = m.group(1).replace(",", ".");
            try {
                double betrag = Double.parseDouble(betragStr);
                if (betrag > maxBetrag && betrag < 1000000) {
                    maxBetrag = betrag;
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return maxBetrag;
    }

    private String extractVerwendungszweck(String text) {
        OcrSettings settings = OcrSettings.getInstance();
        String[] keywords = settings.getVerwendungszweckKeywordsArray();

        for (String keyword : keywords) {
            int idx = text.indexOf(keyword);
            if (idx >= 0) {
                int start = idx + keyword.length();
                int end = text.indexOf("\n", start);
                if (end < 0) end = text.length();
                String zweck = text.substring(start, end).trim();
                if (zweck.length() >= 2) {
                    return zweck;
                }
            }
        }

        Matcher m = ZWECK_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(0).trim();
        }

        return null;
    }
}
