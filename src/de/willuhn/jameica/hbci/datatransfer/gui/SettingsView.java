package de.willuhn.jameica.hbci.datatransfer.gui;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.Headline;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.hbci.datatransfer.OcrSettings;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

import java.util.logging.Logger;

/**
 * Konfigurationsansicht fuer die OCR-Einstellungen.
 */
public class SettingsView extends AbstractView {

    private static final Logger logger = Logger.getLogger(SettingsView.class.getName());
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
        final OcrSettings settings = OcrSettings.getInstance();

        GUI.getView().setTitle(i.tr("settings.title"));

        SimpleContainer container = new SimpleContainer(getParent());

        LabelGroup langGroup = new LabelGroup(container.getComposite(), i.tr("settings.language"));
        final de.willuhn.jameica.gui.input.TextInput langInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getLanguage());
        langGroup.addLabelPair(i.tr("settings.language.code"), langInput);

        LabelGroup pathGroup = new LabelGroup(container.getComposite(), i.tr("settings.tessdata"));
        final de.willuhn.jameica.gui.input.TextInput pathInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getTessdataPath());
        pathGroup.addLabelPair(i.tr("settings.tessdata.path"), pathInput);

        LabelGroup oemGroup = new LabelGroup(container.getComposite(), i.tr("settings.oem"));
        final de.willuhn.jameica.gui.input.TextInput oemInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getOcrEngineMode());
        oemGroup.addLabelPair(i.tr("settings.oem.mode") + " (0-3)", oemInput);

        LabelGroup psmGroup = new LabelGroup(container.getComposite(), i.tr("settings.psm"));
        final de.willuhn.jameica.gui.input.TextInput psmInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getPageSegmentationMode());
        psmGroup.addLabelPair(i.tr("settings.psm.mode") + " (0-13)", psmInput);

        LabelGroup dpiGroup = new LabelGroup(container.getComposite(), i.tr("settings.dpi"));
        final de.willuhn.jameica.gui.input.TextInput dpiInput =
            new de.willuhn.jameica.gui.input.TextInput(String.valueOf(settings.getDpi()));
        dpiGroup.addLabelPair(i.tr("settings.dpi.value"), dpiInput);

        LabelGroup spaceGroup = new LabelGroup(container.getComposite(), i.tr("settings.spaces"));
        final de.willuhn.jameica.gui.input.CheckboxInput spaceInput =
            new de.willuhn.jameica.gui.input.CheckboxInput(settings.isPreserveInterwordSpaces());
        spaceGroup.addLabelPair(i.tr("settings.spaces.preserve"), spaceInput);

        LabelGroup charGroup = new LabelGroup(container.getComposite(), i.tr("settings.chars"));
        final de.willuhn.jameica.gui.input.TextInput whitelistInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getWhitelist());
        charGroup.addLabelPair(i.tr("settings.chars.whitelist"), whitelistInput);

        final de.willuhn.jameica.gui.input.TextInput blacklistInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getBlacklist());
        charGroup.addLabelPair(i.tr("settings.chars.blacklist"), blacklistInput);

        LabelGroup keywordsGroup = new LabelGroup(container.getComposite(), i.tr("settings.keywords"));
        final de.willuhn.jameica.gui.input.TextInput empKeywordsInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getEmpfaengerKeywords());
        keywordsGroup.addLabelPair(i.tr("settings.keywords.empfaenger"), empKeywordsInput);
        addKeywordEditor(empKeywordsInput, i.tr("settings.keywords.empfaenger"));

        final de.willuhn.jameica.gui.input.TextInput zweckKeywordsInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getVerwendungszweckKeywords());
        keywordsGroup.addLabelPair(i.tr("settings.keywords.verwendungszweck"), zweckKeywordsInput);
        addKeywordEditor(zweckKeywordsInput, i.tr("settings.keywords.verwendungszweck"));

        new Button(i.tr("settings.save"), new Action() {
            @Override
            public void handleAction(Object context) throws de.willuhn.util.ApplicationException {
                try {
                    settings.setLanguage((String) langInput.getValue());
                    settings.setTessdataPath((String) pathInput.getValue());

                    String oemVal = (String) oemInput.getValue();
                    if (oemVal != null && oemVal.trim().matches("[0-3]")) {
                        settings.setOcrEngineMode(oemVal.trim());
                    }

                    String psmVal = (String) psmInput.getValue();
                    if (psmVal != null && psmVal.trim().matches("\\d{1,2}")) {
                        settings.setPageSegmentationMode(psmVal.trim());
                    }

                    String dpiStr = (String) dpiInput.getValue();
                    if (dpiStr != null && !dpiStr.trim().isEmpty()) {
                        try {
                            settings.setDpi(Integer.parseInt(dpiStr.trim()));
                        } catch (NumberFormatException ex) {
                            // ignore
                        }
                    }

                    settings.setPreserveInterwordSpaces((Boolean) spaceInput.getValue());
                    settings.setWhitelist((String) whitelistInput.getValue());
                    settings.setBlacklist((String) blacklistInput.getValue());
                    settings.setEmpfaengerKeywords((String) empKeywordsInput.getValue());
                    settings.setVerwendungszweckKeywords((String) zweckKeywordsInput.getValue());

                    settings.save();
                    GUI.getStatusBar().setSuccessText(i.tr("settings.saved"));
                    logger.info("Einstellungen gespeichert");
                } catch (Exception e) {
                    logger.warning("Fehler beim Speichern: " + e.getMessage());
                    throw new de.willuhn.util.ApplicationException(e.getMessage(), e);
                }
            }
        }, null, true).paint(container.getComposite());
    }

    private void addKeywordEditor(final de.willuhn.jameica.gui.input.TextInput input, final String title) {
        org.eclipse.swt.widgets.Control control = input.getControl();
        if (control == null) return;

        control.addMouseListener(new org.eclipse.swt.events.MouseListener() {
            public void mouseDoubleClick(org.eclipse.swt.events.MouseEvent e) {
                openKeywordEditor(input, title);
            }
            public void mouseDown(org.eclipse.swt.events.MouseEvent e) {}
            public void mouseUp(org.eclipse.swt.events.MouseEvent e) {}
        });
    }

    private void openKeywordEditor(final de.willuhn.jameica.gui.input.TextInput input, final String title) {
        final I18N i = getI18n();
        org.eclipse.swt.widgets.Shell parentShell = GUI.getShell();
        if (parentShell == null || parentShell.isDisposed()) return;

        final org.eclipse.swt.widgets.Shell dialog = new org.eclipse.swt.widgets.Shell(parentShell,
            org.eclipse.swt.SWT.APPLICATION_MODAL | org.eclipse.swt.SWT.TITLE | org.eclipse.swt.SWT.CLOSE | org.eclipse.swt.SWT.RESIZE);
        dialog.setText(title);
        dialog.setSize(500, 400);
        dialog.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));

        org.eclipse.swt.widgets.Label hint = new org.eclipse.swt.widgets.Label(dialog, org.eclipse.swt.SWT.WRAP);
        hint.setText(i.tr("editor.hint"));
        hint.setLayoutData(new org.eclipse.swt.layout.GridData(
            org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false));

        final org.eclipse.swt.widgets.Text editor = new org.eclipse.swt.widgets.Text(dialog,
            org.eclipse.swt.SWT.MULTI | org.eclipse.swt.SWT.V_SCROLL | org.eclipse.swt.SWT.H_SCROLL | org.eclipse.swt.SWT.BORDER);
        editor.setLayoutData(new org.eclipse.swt.layout.GridData(
            org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.FILL, true, true));
        editor.setBackground(parentShell.getDisplay().getSystemColor(org.eclipse.swt.SWT.COLOR_WHITE));

        String currentValue = (String) input.getValue();
        if (currentValue != null && !currentValue.isEmpty()) {
            editor.setText(currentValue.replace(",", "\n"));
        }

        org.eclipse.swt.widgets.Composite buttons = new org.eclipse.swt.widgets.Composite(dialog, org.eclipse.swt.SWT.NONE);
        buttons.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false));
        buttons.setLayout(new org.eclipse.swt.layout.GridLayout(2, true));

        org.eclipse.swt.widgets.Button okButton = new org.eclipse.swt.widgets.Button(buttons, org.eclipse.swt.SWT.PUSH);
        okButton.setText("OK");
        okButton.setLayoutData(new org.eclipse.swt.layout.GridData(
            org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false));
        okButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                String text = editor.getText();
                String[] lines = text.split("\\n");
                StringBuilder sb = new StringBuilder();
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        if (sb.length() > 0) sb.append(",");
                        sb.append(trimmed);
                    }
                }
                input.setValue(sb.toString());
                dialog.dispose();
            }
        });

        org.eclipse.swt.widgets.Button cancelButton = new org.eclipse.swt.widgets.Button(buttons, org.eclipse.swt.SWT.PUSH);
        cancelButton.setText(i.tr("editor.cancel"));
        cancelButton.setLayoutData(new org.eclipse.swt.layout.GridData(
            org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false));
        cancelButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                dialog.dispose();
            }
        });

        dialog.open();
    }
}
