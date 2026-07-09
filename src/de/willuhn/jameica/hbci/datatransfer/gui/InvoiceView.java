package de.willuhn.jameica.hbci.datatransfer.gui;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.Headline;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import java.util.logging.Logger;

/**
 * OCR-Ansicht mit Raw-Text-Panel und Text-Picker.
 * Basierend auf InvoiceView aus OCRtransfer.
 */
public class InvoiceView extends AbstractView {

    private static final Logger logger = Logger.getLogger(InvoiceView.class.getName());
    private static I18N i18n;

    private TransferData transferData;

    private de.willuhn.jameica.gui.input.TextInput lastFocusedInput;

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
        if (context instanceof TransferData) {
            this.transferData = (TransferData) context;
        } else {
            this.transferData = new TransferData();
        }

        GUI.getView().setTitle(i.tr("invoice.ocr.title"));

        if (transferData.getRawText() == null || transferData.getRawText().isEmpty()) {
            new Headline(getParent(), i.tr("error.no.text.extracted"));
            return;
        }

        getParent().setLayout(new GridLayout(1, false));

        Composite formComposite = new Composite(getParent(), SWT.NONE);
        formComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        formComposite.setLayout(new GridLayout(1, false));
        SimpleContainer container = new SimpleContainer(formComposite, false);

        container.addHeadline(i.tr("invoice.ocr.title"));

        LabelGroup ibanGroup = new LabelGroup(container.getComposite(), i.tr("section.empfaenger.konto"));
        final de.willuhn.jameica.gui.input.TextInput ibanInput =
            new de.willuhn.jameica.gui.input.TextInput(
                transferData.getIban() != null ? transferData.getIban() : "");
        ibanGroup.addLabelPair("IBAN", ibanInput);
        trackFocus(ibanInput);

        final de.willuhn.jameica.gui.input.TextInput bicInput =
            new de.willuhn.jameica.gui.input.TextInput(
                transferData.getBic() != null ? transferData.getBic() : "");
        ibanGroup.addLabelPair("BIC", bicInput);
        trackFocus(bicInput);

        LabelGroup empGroup = new LabelGroup(container.getComposite(), i.tr("empfaenger"));
        final de.willuhn.jameica.gui.input.TextInput nameInput =
            new de.willuhn.jameica.gui.input.TextInput(
                transferData.getEmpfaengerName() != null ? transferData.getEmpfaengerName() : "");
        empGroup.addLabelPair(i.tr("name"), nameInput);
        trackFocus(nameInput);

        LabelGroup betragGroup = new LabelGroup(container.getComposite(), i.tr("betrag"));
        String betragStr = "";
        if (transferData.getBetrag() > 0) {
            betragStr = String.format(java.util.Locale.GERMAN, "%.2f", transferData.getBetrag());
        }
        final de.willuhn.jameica.gui.input.TextInput betragInput =
            new de.willuhn.jameica.gui.input.TextInput(betragStr);
        betragGroup.addLabelPair(i.tr("betrag"), betragInput);
        trackFocus(betragInput);

        final de.willuhn.jameica.gui.input.TextInput waehrungInput =
            new de.willuhn.jameica.gui.input.TextInput(
                transferData.getWaehrung() != null ? transferData.getWaehrung() : "EUR");
        betragGroup.addLabelPair(i.tr("waehrung"), waehrungInput);
        trackFocus(waehrungInput);

        LabelGroup zweckGroup = new LabelGroup(container.getComposite(), i.tr("verwendungszweck"));
        String verwendungszweck = transferData.getVerwendungszweck();
        final de.willuhn.jameica.gui.input.TextInput zweckInput =
            new de.willuhn.jameica.gui.input.TextInput(
                verwendungszweck != null ? verwendungszweck : "");
        zweckGroup.addLabelPair(i.tr("verwendungszweck"), zweckInput);
        trackFocus(zweckInput);

        Composite actionButtons = new Composite(getParent(), SWT.NONE);
        actionButtons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        actionButtons.setLayout(new GridLayout(2, true));

        new Button(i.tr("ueberweisung.erstellen"), new Action() {
            @Override
            public void handleAction(Object context) throws de.willuhn.util.ApplicationException {
                logger.info("Button 'Ueberweisung erstellen' geklickt");
                try {
                    createTransfer(ibanInput, bicInput, nameInput, betragInput, waehrungInput, zweckInput);
                } catch (Exception e) {
                    logger.warning("Fehler beim Erstellen der Ueberweisung: " + e.getMessage());
                    throw new de.willuhn.util.ApplicationException(e.getMessage(), e);
                }
            }
        }, null, true).paint(actionButtons);

        new Button(i.tr("debug.show"), new Action() {
            @Override
            public void handleAction(Object context) throws de.willuhn.util.ApplicationException {
                try {
                    GUI.startView(InvoiceDebugView.class, InvoiceView.this.transferData);
                } catch (Exception e) {
                    throw new de.willuhn.util.ApplicationException(e.getMessage(), e);
                }
            }
        }, null, true).paint(actionButtons);

        Composite rawPanel = new Composite(getParent(), SWT.NONE);
        rawPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        rawPanel.setLayout(new GridLayout(1, false));

        Label hint = new Label(rawPanel, SWT.WRAP);
        hint.setText(i.tr("pick.hint"));
        hint.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        final Text rawText = new Text(rawPanel, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.WRAP);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.minimumHeight = 150;
        rawText.setLayoutData(gd);
        rawText.setBackground(getParent().getDisplay().getSystemColor(SWT.COLOR_WHITE));
        if (transferData.getRawText() != null) {
            rawText.setText(transferData.getRawText());
        }

        Composite pickButtons = new Composite(rawPanel, SWT.NONE);
        pickButtons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        pickButtons.setLayout(new GridLayout(2, true));

        org.eclipse.swt.widgets.Button addBtn = new org.eclipse.swt.widgets.Button(pickButtons, SWT.PUSH);
        addBtn.setText(i.tr("pick.ok"));
        addBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        addBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (lastFocusedInput == null) return;
                String sel = rawText.getSelectionText();
                if (sel == null || sel.isEmpty()) return;
                String current = (String) lastFocusedInput.getValue();
                if (current == null) current = "";
                lastFocusedInput.setValue(current.isEmpty() ? sel : current + ", " + sel);
            }
        });

        org.eclipse.swt.widgets.Button owBtn = new org.eclipse.swt.widgets.Button(pickButtons, SWT.PUSH);
        owBtn.setText(i.tr("pick.overwrite"));
        owBtn.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        owBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (lastFocusedInput == null) return;
                String sel = rawText.getSelectionText();
                if (sel == null || sel.isEmpty()) return;
                lastFocusedInput.setValue(sel);
            }
        });
    }

    private void trackFocus(final de.willuhn.jameica.gui.input.TextInput input) {
        Control control = input.getControl();
        if (control == null) return;
        control.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                lastFocusedInput = input;
            }
        });
    }

    private void createTransfer(
            de.willuhn.jameica.gui.input.TextInput ibanInput,
            de.willuhn.jameica.gui.input.TextInput bicInput,
            de.willuhn.jameica.gui.input.TextInput nameInput,
            de.willuhn.jameica.gui.input.TextInput betragInput,
            de.willuhn.jameica.gui.input.TextInput waehrungInput,
            de.willuhn.jameica.gui.input.TextInput zweckInput
    ) throws Exception {
        final I18N i = getI18n();

        String iban = (String) ibanInput.getValue();
        String bic = (String) bicInput.getValue();
        String name = (String) nameInput.getValue();
        String betragStr = (String) betragInput.getValue();
        String zweck = (String) zweckInput.getValue();

        if (iban == null || iban.trim().isEmpty()) {
            throw new de.willuhn.util.ApplicationException(i.tr("error.iban.required"));
        }
        if (name == null || name.trim().isEmpty()) {
            throw new de.willuhn.util.ApplicationException(i.tr("error.empfaenger.required"));
        }
        if (betragStr == null || betragStr.trim().isEmpty()) {
            throw new de.willuhn.util.ApplicationException(i.tr("error.betrag.required"));
        }

        double betrag;
        try {
            String normalized = betragStr.trim().replace(",", ".");
            betrag = Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            throw new de.willuhn.util.ApplicationException(i.tr("error.betrag.invalid"));
        }

        de.willuhn.jameica.hbci.rmi.AuslandsUeberweisung u =
            (de.willuhn.jameica.hbci.rmi.AuslandsUeberweisung)
            de.willuhn.jameica.hbci.Settings.getDBService()
                .createObject(de.willuhn.jameica.hbci.rmi.AuslandsUeberweisung.class, null);

        u.setGegenkontoNummer(iban.trim());
        if (bic != null && !bic.trim().isEmpty()) {
            u.setGegenkontoBLZ(bic.trim());
        }
        u.setGegenkontoName(name.trim());
        u.setBetrag(betrag);
        if (zweck != null && !zweck.trim().isEmpty()) {
            u.setZweck(zweck.trim());
        }
        u.setTermin(new java.util.Date());

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
