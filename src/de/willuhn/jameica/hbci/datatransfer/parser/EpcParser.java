package de.willuhn.jameica.hbci.datatransfer.parser;

import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData.Source;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

public class EpcParser implements QrCodeParser {

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
    public boolean canParse(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.trim().startsWith("BCD");
    }

    @Override
    public TransferData parse(String text) throws ParserException {
        final I18N i = getI18n();
        if (text == null || text.isEmpty()) {
            throw new ParserException(i.tr("emv.text.empty"));
        }

        String[] lines = text.split("\\r?\\n");
        for (int ii = 0; ii < lines.length; ii++) {
            lines[ii] = lines[ii].trim();
        }

        if (lines.length < 7) {
            throw new ParserException(i.tr("epc.too.few.lines"));
        }

        TransferData data = new TransferData();
        data.setSource(Source.QR_EPC);
        data.setFormat("EPC (BCD)");

        if (!"SCT".equals(lines[3].trim())) {
            throw new ParserException(i.tr("epc.non.sepa", lines[3]));
        }

        data.setBic(getOrNull(lines, 4));
        data.setEmpfaengerName(getOrNull(lines, 5));
        data.setIban(getOrNull(lines, 6));

        int pos = 7;

        if (pos < lines.length) {
            String line7 = lines[pos].trim();
            if (looksLikeAmount(line7)) {
                data.setBetrag(parseBetrag(line7));
                pos++;
            }
        }

        if (pos < lines.length) {
            String next = lines[pos].trim();
            if (next.startsWith("//")) {
                data.setBetreff(next.substring(2));
                pos++;
            }
        }

        StringBuilder zweck = new StringBuilder();
        while (pos < lines.length) {
            String line = lines[pos].trim();
            if (!line.isEmpty()) {
                if (zweck.length() > 0) zweck.append("\n");
                zweck.append(line);
            }
            pos++;
        }
        if (zweck.length() > 0) {
            data.setVerwendungszweck(zweck.toString());
        }

        if (!data.isValid()) {
            throw new ParserException(i.tr("epc.incomplete"));
        }

        return data;
    }

    private boolean looksLikeAmount(String line) {
        if (line.isEmpty()) return false;
        String cleaned = line.replaceAll("^[A-Z]{3}", "").trim();
        return cleaned.matches("\\d+[.,]?\\d*");
    }

    private String parseBetrag(String line) {
        String cleaned = line.replaceAll("^[A-Z]{3}", "").trim();
        cleaned = cleaned.replace(",", ".");
        return cleaned;
    }

    private String getOrNull(String[] lines, int index) {
        if (index < lines.length) {
            String v = lines[index].trim();
            return v.isEmpty() ? null : v;
        }
        return null;
    }
}
