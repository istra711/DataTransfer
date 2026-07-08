package de.willuhn.jameica.hbci.datatransfer.parser;

import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData.Source;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;
import java.util.LinkedHashMap;
import java.util.Map;

public class EmvParser implements QrCodeParser {

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

    private static final String SEPA_GUI = "sepa";

    @Override
    public boolean canParse(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.startsWith("000201");
    }

    @Override
    public TransferData parse(String text) throws ParserException {
        final I18N i = getI18n();
        if (text == null || text.isEmpty()) {
            throw new ParserException(i.tr("emv.text.empty"));
        }

        TransferData data = new TransferData();
        data.setSource(Source.QR_EMV);
        data.setFormat("EMV (TLV)");

        try {
            Map<String, java.util.List<TlvEntry>> parsed = parseTlv(text);

            String format = getValue(parsed, "00");
            if (!"01".equals(format)) {
                throw new ParserException(i.tr("emv.invalid.format", format));
            }

            String merchantInfo = getValue(parsed, "26");
            if (merchantInfo == null) {
                throw new ParserException(i.tr("emv.no.merchant.info"));
            }

            Map<String, TlvEntry> subTags = parseSubTlv(merchantInfo);

            TlvEntry gui = subTags.get("00");
            if (gui == null || !SEPA_GUI.equalsIgnoreCase(gui.value)) {
                throw new ParserException(i.tr("emv.not.sepa",
                    gui != null ? gui.value : "fehlt"));
            }

            TlvEntry iban = subTags.get("01");
            if (iban == null) {
                throw new ParserException(i.tr("emv.iban.missing"));
            }
            data.setIban(iban.value);

            TlvEntry bic = subTags.get("02");
            if (bic != null) {
                data.setBic(bic.value);
            }

            TlvEntry empfaengerName = subTags.get("04");

            String waehrung = getValue(parsed, "53");
            if (waehrung != null) {
                data.setWaehrung(convertCurrency(waehrung));
            }

            String betrag = getValue(parsed, "54");
            if (betrag != null) {
                data.setBetrag(betrag);
            }

            if (empfaengerName != null) {
                data.setEmpfaengerName(empfaengerName.value);
            } else {
                String name59 = getValue(parsed, "59");
                if (name59 != null) {
                    data.setEmpfaengerName(name59);
                }
            }

            String ort = getValue(parsed, "60");
            if (ort != null) {
                data.setEmpfaengerOrt(ort);
            }

            String additionalData = getValue(parsed, "62");
            if (additionalData != null) {
                Map<String, TlvEntry> additionalSubTags = parseSubTlv(additionalData);
                TlvEntry refNumber = additionalSubTags.get("02");
                if (refNumber != null) {
                    data.setVerwendungszweck(refNumber.value);
                }
                TlvEntry additionalRef = additionalSubTags.get("03");
                if (additionalRef != null) {
                    data.setBetreff(additionalRef.value);
                }
            }

            if (!data.isValid()) {
                throw new ParserException(i.tr("emv.incomplete.data"));
            }

        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException(i.tr("emv.parse.error", e.getMessage()), e);
        }

        return data;
    }

    private Map<String, java.util.List<TlvEntry>> parseTlv(String payload) {
        Map<String, java.util.List<TlvEntry>> result = new LinkedHashMap<>();
        int pos = 0;
        while (pos + 4 <= payload.length()) {
            String tag = payload.substring(pos, pos + 2);
            String lengthStr = payload.substring(pos + 2, pos + 4);
            int length;
            try {
                length = Integer.parseInt(lengthStr);
            } catch (NumberFormatException e) {
                break;
            }
            if (pos + 4 + length > payload.length()) break;
            String value = payload.substring(pos + 4, pos + 4 + length);
            pos += 4 + length;
            result.computeIfAbsent(tag, k -> new java.util.ArrayList<>())
                  .add(new TlvEntry(tag, value, length));
        }
        return result;
    }

    private Map<String, TlvEntry> parseSubTlv(String containerValue) {
        Map<String, TlvEntry> result = new LinkedHashMap<>();
        int pos = 0;
        while (pos + 4 <= containerValue.length()) {
            String subTag = containerValue.substring(pos, pos + 2);
            String lengthStr = containerValue.substring(pos + 2, pos + 4);
            int length;
            try {
                length = Integer.parseInt(lengthStr);
            } catch (NumberFormatException e) {
                break;
            }
            if (pos + 4 + length > containerValue.length()) break;
            String value = containerValue.substring(pos + 4, pos + 4 + length);
            pos += 4 + length;
            result.put(subTag, new TlvEntry(subTag, value, length));
        }
        return result;
    }

    private String getValue(Map<String, java.util.List<TlvEntry>> parsed, String tag) {
        java.util.List<TlvEntry> entries = parsed.get(tag);
        if (entries == null || entries.isEmpty()) return null;
        return entries.get(0).value;
    }

    private String convertCurrency(String code) {
        switch (code) {
            case "978": return "EUR";
            case "840": return "USD";
            case "826": return "GBP";
            case "756": return "CHF";
            default: return code;
        }
    }

    static class TlvEntry {
        final String tag;
        final String value;
        final int length;
        TlvEntry(String tag, String value, int length) {
            this.tag = tag;
            this.value = value;
            this.length = length;
        }
    }
}
