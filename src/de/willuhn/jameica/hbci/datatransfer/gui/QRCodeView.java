package de.willuhn.jameica.hbci.datatransfer.gui;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.Headline;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.hbci.rmi.AuslandsUeberweisung;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

import java.util.Date;

/**
 * QR-Code-Ansicht mit Datenanzeige und Ueberweisungsbutton.
 * Basierend auf QRCodeView aus QRtransfer.
 */
public class QRCodeView extends AbstractView {

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

    private TransferData transferData;

    @Override
    public void bind() throws Exception {
        final I18N i = getI18n();
        this.transferData = (TransferData) getCurrentObject();

        if (transferData == null) {
            new Headline(getParent(), i.tr("error"));
            return;
        }

        GUI.getView().setTitle(i.tr("sepa.qrtransfer.title"));

        SimpleContainer container = new SimpleContainer(getParent());
        container.addHeadline(i.tr("qrcode.data.read.success"));

        LabelGroup formatGroup = new LabelGroup(container.getComposite(), i.tr("recognized.format"));
        String format = transferData.getFormat() != null ? transferData.getFormat() : i.tr("unknown");
        formatGroup.addLabelPair(i.tr("format"), new de.willuhn.jameica.gui.input.TextInput(format));

        LabelGroup ibanGroup = new LabelGroup(container.getComposite(), i.tr("recipient.account"));
        ibanGroup.addLabelPair("IBAN", new de.willuhn.jameica.gui.input.TextInput(
            transferData.getIban() != null ? transferData.getIban() : ""));
        ibanGroup.addLabelPair("BIC", new de.willuhn.jameica.gui.input.TextInput(
            transferData.getBic() != null ? transferData.getBic() : ""));

        LabelGroup empfaengerGroup = new LabelGroup(container.getComposite(), i.tr("recipient"));
        empfaengerGroup.addLabelPair(i.tr("name"), new de.willuhn.jameica.gui.input.TextInput(
            transferData.getEmpfaengerName() != null ? transferData.getEmpfaengerName() : ""));

        LabelGroup betragGroup = new LabelGroup(container.getComposite(), i.tr("amount"));
        String betragStr = "";
        if (transferData.getBetrag() > 0) {
            betragStr = String.format(java.util.Locale.GERMAN, "%.2f", transferData.getBetrag());
        }
        betragGroup.addLabelPair(i.tr("amount"), new de.willuhn.jameica.gui.input.TextInput(betragStr));
        betragGroup.addLabelPair(i.tr("currency"), new de.willuhn.jameica.gui.input.TextInput(
            transferData.getWaehrung() != null ? transferData.getWaehrung() : "EUR"));

        LabelGroup zweckGroup = new LabelGroup(container.getComposite(), i.tr("purpose"));
        String verwendungszweck = transferData.getVerwendungszweck();
        String betreff = transferData.getBetreff();

        if (verwendungszweck != null && !verwendungszweck.isEmpty()) {
            zweckGroup.addLabelPair(i.tr("purpose"), new de.willuhn.jameica.gui.input.TextInput(verwendungszweck));
        }
        if (betreff != null && !betreff.isEmpty()) {
            zweckGroup.addLabelPair(i.tr("subject"), new de.willuhn.jameica.gui.input.TextInput(betreff));
        }
        if ((verwendungszweck == null || verwendungszweck.isEmpty()) &&
            (betreff == null || betreff.isEmpty())) {
            zweckGroup.addLabelPair(i.tr("purpose"), new de.willuhn.jameica.gui.input.TextInput(i.tr("none")));
        }

        String rawText = transferData.getRawText();
        if (rawText != null && !rawText.isEmpty()) {
            LabelGroup rawGroup = new LabelGroup(container.getComposite(), i.tr("qrcode.rawdata"));
            rawGroup.addLabelPair(i.tr("content"), new de.willuhn.jameica.gui.input.TextInput(rawText));
        }

        new Button(i.tr("create.transfer"), new Action() {
            @Override
            public void handleAction(Object context) throws ApplicationException {
                try {
                    createTransfer(transferData);
                } catch (Exception e) {
                    throw new ApplicationException(i.tr("error.param", e.getMessage()), e);
                }
            }
        }, null, true).paint(container.getComposite());
    }

    private void createTransfer(TransferData data) throws Exception {
        if (data == null || !data.isValid()) {
            throw new ApplicationException(getI18n().tr("sepa.data.incomplete"));
        }

        AuslandsUeberweisung u = (AuslandsUeberweisung) Settings.getDBService()
            .createObject(AuslandsUeberweisung.class, null);

        u.setGegenkontoNummer(data.getIban());
        if (data.getBic() != null && !data.getBic().isEmpty()) {
            u.setGegenkontoBLZ(data.getBic());
        }
        u.setGegenkontoName(data.getEmpfaengerName());

        if (data.getBetrag() > 0) {
            u.setBetrag(data.getBetrag());
        }

        String zweck = data.getZweckFeld();
        if (zweck != null && !zweck.isEmpty()) {
            u.setZweck(zweck);
        }

        u.setTermin(new Date());

        if (u.isNewObject()) {
            try {
                java.lang.reflect.Method m = u.getClass().getMethod("setInstantPayment", boolean.class);
                m.invoke(u, true);
            } catch (NoSuchMethodException e) {
                // Hibiscus 2.10.4 oder frueher
            }
        }

        GUI.startView(de.willuhn.jameica.hbci.gui.views.AuslandsUeberweisungNew.class, u);
    }
}
