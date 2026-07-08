package de.willuhn.jameica.hbci.datatransfer.gui;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.util.Headline;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

/**
 * Debug-Ansicht fuer extrahierte Daten.
 */
public class InvoiceDebugView extends AbstractView {

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
    public void bind() throws Exception {
        final I18N i = getI18n();

        Object context = getCurrentObject();
        TransferData data = null;
        if (context instanceof TransferData) {
            data = (TransferData) context;
        }

        GUI.getView().setTitle(i.tr("debug.title"));

        if (data == null) {
            new Headline(getParent(), i.tr("error"));
            return;
        }

        SimpleContainer container = new SimpleContainer(getParent());
        container.addHeadline(i.tr("debug.title"));

        LabelGroup rawGroup = new LabelGroup(container.getComposite(), i.tr("debug.rawtext"));
        rawGroup.addLabelPair(i.tr("content"), new de.willuhn.jameica.gui.input.TextInput(
            data.getRawText() != null ? data.getRawText() : ""));

        LabelGroup ibanGroup = new LabelGroup(container.getComposite(), i.tr("debug.iban"));
        ibanGroup.addLabelPair("IBAN", new de.willuhn.jameica.gui.input.TextInput(
            data.getIban() != null ? data.getIban() : ""));
        ibanGroup.addLabelPair("BIC", new de.willuhn.jameica.gui.input.TextInput(
            data.getBic() != null ? data.getBic() : ""));

        LabelGroup empGroup = new LabelGroup(container.getComposite(), i.tr("debug.empfaenger"));
        empGroup.addLabelPair(i.tr("name"), new de.willuhn.jameica.gui.input.TextInput(
            data.getEmpfaengerName() != null ? data.getEmpfaengerName() : ""));
        empGroup.addLabelPair(i.tr("strasse"), new de.willuhn.jameica.gui.input.TextInput(
            data.getEmpfaengerStrasse() != null ? data.getEmpfaengerStrasse() : ""));
        empGroup.addLabelPair(i.tr("ort"), new de.willuhn.jameica.gui.input.TextInput(
            data.getEmpfaengerOrt() != null ? data.getEmpfaengerOrt() : ""));

        LabelGroup betragGroup = new LabelGroup(container.getComposite(), i.tr("debug.betrag"));
        String betragStr = "";
        if (data.getBetrag() > 0) {
            betragStr = String.format(java.util.Locale.GERMAN, "%.2f", data.getBetrag());
        }
        betragGroup.addLabelPair(i.tr("betrag"), new de.willuhn.jameica.gui.input.TextInput(betragStr));
        betragGroup.addLabelPair(i.tr("waehrung"), new de.willuhn.jameica.gui.input.TextInput(
            data.getWaehrung() != null ? data.getWaehrung() : ""));

        LabelGroup zweckGroup = new LabelGroup(container.getComposite(), i.tr("debug.verwendungszweck"));
        zweckGroup.addLabelPair(i.tr("verwendungszweck"), new de.willuhn.jameica.gui.input.TextInput(
            data.getVerwendungszweck() != null ? data.getVerwendungszweck() : ""));
        zweckGroup.addLabelPair(i.tr("betreff"), new de.willuhn.jameica.gui.input.TextInput(
            data.getBetreff() != null ? data.getBetreff() : ""));

        LabelGroup infoGroup = new LabelGroup(container.getComposite(), i.tr("debug.info"));
        infoGroup.addLabelPair(i.tr("quelle"), new de.willuhn.jameica.gui.input.TextInput(
            data.getQuelle() != null ? data.getQuelle() : ""));
        infoGroup.addLabelPair(i.tr("source"), new de.willuhn.jameica.gui.input.TextInput(
            data.getSource() != null ? data.getSource().toString() : ""));
        infoGroup.addLabelPair(i.tr("format"), new de.willuhn.jameica.gui.input.TextInput(
            data.getFormat() != null ? data.getFormat() : ""));
    }
}
