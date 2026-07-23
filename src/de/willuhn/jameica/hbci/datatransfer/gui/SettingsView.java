package de.willuhn.jameica.hbci.datatransfer.gui;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.Headline;
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

        // Hilfe Button oben
        org.eclipse.swt.widgets.Composite topBar = new org.eclipse.swt.widgets.Composite(getParent(), org.eclipse.swt.SWT.NONE);
        topBar.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false));
        org.eclipse.swt.layout.GridLayout topLayout = new org.eclipse.swt.layout.GridLayout(2, false);
        topLayout.marginWidth = 0;
        topLayout.marginHeight = 0;
        topBar.setLayout(topLayout);

        new Headline(topBar, i.tr("settings.title"));

        new Button(i.tr("settings.help"), new Action() {
            @Override
            public void handleAction(Object context) throws de.willuhn.util.ApplicationException {
                showHelpDialog();
            }
        }, null, false).paint(topBar);

        // Eigenes ScrolledComposite fuer den Settings-Inhalt
        org.eclipse.swt.custom.ScrolledComposite sc = new org.eclipse.swt.custom.ScrolledComposite(getParent(),
            org.eclipse.swt.SWT.V_SCROLL | org.eclipse.swt.SWT.BORDER);
        sc.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.FILL, true, true));
        sc.setExpandHorizontal(true);
        sc.setExpandVertical(false);

        org.eclipse.swt.widgets.Composite content = new org.eclipse.swt.widgets.Composite(sc, org.eclipse.swt.SWT.NONE);
        content.setLayout(new org.eclipse.swt.layout.GridLayout(2, false));
        org.eclipse.swt.layout.GridData contentGd = new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false);
        content.setLayoutData(contentGd);

        // ═══════════════════════════════════════════════════════════════
        // OCR-Einstellungen
        // ═══════════════════════════════════════════════════════════════
        org.eclipse.swt.widgets.Label ocrHeadline = new org.eclipse.swt.widgets.Label(content, org.eclipse.swt.SWT.NONE);
        ocrHeadline.setText(i.tr("settings.ocr.section"));
        ocrHeadline.setFont(new org.eclipse.swt.graphics.Font(content.getDisplay(), new org.eclipse.swt.graphics.FontData("Sans", 11, org.eclipse.swt.SWT.BOLD)));
        ocrHeadline.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false, 2, 1));

        LabelGroup langGroup = new LabelGroup(content, i.tr("settings.language"));
        final de.willuhn.jameica.gui.input.TextInput langInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getLanguage());
        langGroup.addLabelPair(i.tr("settings.language.code"), langInput);

        LabelGroup pathGroup = new LabelGroup(content, i.tr("settings.tessdata"));
        final de.willuhn.jameica.gui.input.TextInput pathInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getTessdataPath());
        pathGroup.addLabelPair(i.tr("settings.tessdata.path"), pathInput);

        LabelGroup oemGroup = new LabelGroup(content, i.tr("settings.oem"));
        final de.willuhn.jameica.gui.input.TextInput oemInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getOcrEngineMode());
        oemGroup.addLabelPair(i.tr("settings.oem.mode") + " (0-3)", oemInput);

        LabelGroup psmGroup = new LabelGroup(content, i.tr("settings.psm"));
        final de.willuhn.jameica.gui.input.TextInput psmInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getPageSegmentationMode());
        psmGroup.addLabelPair(i.tr("settings.psm.mode") + " (0-13)", psmInput);

        LabelGroup dpiGroup = new LabelGroup(content, i.tr("settings.dpi"));
        final de.willuhn.jameica.gui.input.TextInput dpiInput =
            new de.willuhn.jameica.gui.input.TextInput(String.valueOf(settings.getDpi()));
        dpiGroup.addLabelPair(i.tr("settings.dpi.value"), dpiInput);

        LabelGroup spaceGroup = new LabelGroup(content, i.tr("settings.spaces"));
        final de.willuhn.jameica.gui.input.CheckboxInput spaceInput =
            new de.willuhn.jameica.gui.input.CheckboxInput(settings.isPreserveInterwordSpaces());
        spaceGroup.addLabelPair(i.tr("settings.spaces.preserve"), spaceInput);

        LabelGroup whitelistGroup = new LabelGroup(content, i.tr("settings.chars.whitelist"));
        final de.willuhn.jameica.gui.input.TextInput whitelistInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getWhitelist());
        whitelistGroup.addLabelPair(i.tr("settings.chars.whitelist"), whitelistInput);

        LabelGroup blacklistGroup = new LabelGroup(content, i.tr("settings.chars.blacklist"));
        final de.willuhn.jameica.gui.input.TextInput blacklistInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getBlacklist());
        blacklistGroup.addLabelPair(i.tr("settings.chars.blacklist"), blacklistInput);

        LabelGroup empKeywordsGroup = new LabelGroup(content, i.tr("settings.keywords.empfaenger"));
        final de.willuhn.jameica.gui.input.TextInput empKeywordsInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getEmpfaengerKeywords());
        empKeywordsGroup.addLabelPair(i.tr("settings.keywords.empfaenger"), empKeywordsInput);
        addKeywordEditor(empKeywordsInput, i.tr("settings.keywords.empfaenger"));

        LabelGroup zweckKeywordsGroup = new LabelGroup(content, i.tr("settings.keywords.verwendungszweck"));
        final de.willuhn.jameica.gui.input.TextInput zweckKeywordsInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getVerwendungszweckKeywords());
        zweckKeywordsGroup.addLabelPair(i.tr("settings.keywords.verwendungszweck"), zweckKeywordsInput);
        addKeywordEditor(zweckKeywordsInput, i.tr("settings.keywords.verwendungszweck"));

        // ═══════════════════════════════════════════════════════════════
        // Web/IP-Cam-Einstellungen (getrennt durch Trennlinie)
        // ═══════════════════════════════════════════════════════════════
        org.eclipse.swt.widgets.Label separator = new org.eclipse.swt.widgets.Label(content, org.eclipse.swt.SWT.SEPARATOR | org.eclipse.swt.SWT.HORIZONTAL);
        separator.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false, 2, 1));

        org.eclipse.swt.widgets.Label camHeadline = new org.eclipse.swt.widgets.Label(content, org.eclipse.swt.SWT.NONE);
        camHeadline.setText(i.tr("settings.webcam.section"));
        camHeadline.setFont(new org.eclipse.swt.graphics.Font(content.getDisplay(), new org.eclipse.swt.graphics.FontData("Sans", 11, org.eclipse.swt.SWT.BOLD)));
        camHeadline.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.TOP, true, false, 2, 1));

        LabelGroup sourceGroup = new LabelGroup(content, i.tr("settings.webcam.source"));
        final de.willuhn.jameica.gui.input.SelectInput sourceInput =
            new de.willuhn.jameica.gui.input.SelectInput(
                new String[]{i.tr("settings.webcam.source.local"), i.tr("settings.webcam.source.ip")},
                settings.getWebcamSource().equals("local") ? i.tr("settings.webcam.source.local") : i.tr("settings.webcam.source.ip")
            );
        sourceGroup.addLabelPair(i.tr("settings.webcam.source.type"), sourceInput);

        LabelGroup localGroup = new LabelGroup(content, i.tr("settings.webcam.local"));
        final de.willuhn.jameica.gui.input.TextInput deviceIndexInput =
            new de.willuhn.jameica.gui.input.TextInput(String.valueOf(settings.getLocalDeviceIndex()));
        localGroup.addLabelPair(i.tr("settings.webcam.local.device"), deviceIndexInput);

        LabelGroup protocolGroup = new LabelGroup(content, i.tr("settings.webcam.ip.protocol"));
        final de.willuhn.jameica.gui.input.SelectInput protocolInput =
            new de.willuhn.jameica.gui.input.SelectInput(
                new String[]{"http", "https", "rtsp"},
                settings.getIpProtocol()
            );
        protocolGroup.addLabelPair(i.tr("settings.webcam.ip.protocol"), protocolInput);

        LabelGroup ipAddrGroup = new LabelGroup(content, i.tr("settings.webcam.ip.address"));
        final de.willuhn.jameica.gui.input.TextInput ipInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getIpAddress());
        ipAddrGroup.addLabelPair(i.tr("settings.webcam.ip.address"), ipInput);

        LabelGroup portGroup = new LabelGroup(content, i.tr("settings.webcam.ip.port"));
        final de.willuhn.jameica.gui.input.TextInput portInput =
            new de.willuhn.jameica.gui.input.TextInput(String.valueOf(settings.getIpPort()));
        portGroup.addLabelPair(i.tr("settings.webcam.ip.port"), portInput);

        LabelGroup pathGroup2 = new LabelGroup(content, i.tr("settings.webcam.ip.path"));
        final de.willuhn.jameica.gui.input.TextInput urlPathInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getIpPath());
        pathGroup2.addLabelPair(i.tr("settings.webcam.ip.path"), urlPathInput);

        LabelGroup userGroup = new LabelGroup(content, i.tr("settings.webcam.ip.username"));
        final de.willuhn.jameica.gui.input.TextInput usernameInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getIpUsername());
        userGroup.addLabelPair(i.tr("settings.webcam.ip.username"), usernameInput);

        LabelGroup passGroup = new LabelGroup(content, i.tr("settings.webcam.ip.password"));
        final de.willuhn.jameica.gui.input.TextInput passwordInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getIpPassword());
        passGroup.addLabelPair(i.tr("settings.webcam.ip.password"), passwordInput);

        LabelGroup timeoutGroup = new LabelGroup(content, i.tr("settings.webcam.ip.timeout"));
        final de.willuhn.jameica.gui.input.TextInput timeoutInput =
            new de.willuhn.jameica.gui.input.TextInput(String.valueOf(settings.getIpTimeout()));
        timeoutGroup.addLabelPair(i.tr("settings.webcam.ip.timeout"), timeoutInput);

        LabelGroup rotationGroup = new LabelGroup(content, i.tr("settings.webcam.rotation"));
        final de.willuhn.jameica.gui.input.SelectInput rotationInput =
            new de.willuhn.jameica.gui.input.SelectInput(
                new String[]{"0", "90", "180", "270"},
                String.valueOf(settings.getRotation())
            );
        rotationGroup.addLabelPair(i.tr("settings.webcam.rotation degrees"), rotationInput);

        // ═══════════════════════════════════════════════════════════════
        // Speichern-Button
        // ═══════════════════════════════════════════════════════════════
        new Button(i.tr("settings.save"), new Action() {
            @Override
            public void handleAction(Object context) throws de.willuhn.util.ApplicationException {
                try {
                    // OCR settings
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

                    // QR/Webcam settings
                    String selectedSource = (String) sourceInput.getValue();
                    settings.setWebcamSource(selectedSource.contains("IP") ? "ip" : "local");

                    String devIdxStr = (String) deviceIndexInput.getValue();
                    if (devIdxStr != null && !devIdxStr.trim().isEmpty()) {
                        try {
                            settings.setLocalDeviceIndex(Integer.parseInt(devIdxStr.trim()));
                        } catch (NumberFormatException ex) {
                            // ignore
                        }
                    }

                    settings.setIpProtocol((String) protocolInput.getValue());
                    settings.setIpAddress((String) ipInput.getValue());

                    String portStr = (String) portInput.getValue();
                    if (portStr != null && !portStr.trim().isEmpty()) {
                        try {
                            settings.setIpPort(Integer.parseInt(portStr.trim()));
                        } catch (NumberFormatException ex) {
                            // ignore
                        }
                    }

                    settings.setIpPath((String) urlPathInput.getValue());
                    settings.setIpUsername((String) usernameInput.getValue());
                    settings.setIpPassword((String) passwordInput.getValue());

                    String timeoutStr = (String) timeoutInput.getValue();
                    if (timeoutStr != null && !timeoutStr.trim().isEmpty()) {
                        try {
                            settings.setIpTimeout(Integer.parseInt(timeoutStr.trim()));
                        } catch (NumberFormatException ex) {
                            // ignore
                        }
                    }

                    String rotStr = (String) rotationInput.getValue();
                    if (rotStr != null && !rotStr.trim().isEmpty()) {
                        try {
                            settings.setRotation(Integer.parseInt(rotStr.trim()));
                        } catch (NumberFormatException ex) {
                            // ignore
                        }
                    }

                    settings.save();
                    GUI.getStatusBar().setSuccessText(i.tr("settings.saved"));
                    logger.info("Einstellungen gespeichert");
                } catch (Exception e) {
                    logger.warning("Fehler beim Speichern: " + e.getMessage());
                    throw new de.willuhn.util.ApplicationException(e.getMessage(), e);
                }
            }
        }, null, true).paint(content);

        // ScrolledComposite konfigurieren
        sc.setContent(content);
        content.pack(true);
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

    private void showHelpDialog() {
        final I18N i = getI18n();
        org.eclipse.swt.widgets.Shell shell = GUI.getShell();
        if (shell == null || shell.isDisposed()) return;

        org.eclipse.swt.widgets.Shell dialogShell = new org.eclipse.swt.widgets.Shell(shell,
            org.eclipse.swt.SWT.APPLICATION_MODAL | org.eclipse.swt.SWT.TITLE | org.eclipse.swt.SWT.CLOSE | org.eclipse.swt.SWT.RESIZE);
        dialogShell.setText(i.tr("settings.help.title"));
        dialogShell.setSize(600, 650);
        dialogShell.setLayout(new org.eclipse.swt.layout.GridLayout(1, false));

        org.eclipse.swt.widgets.Text helpText = new org.eclipse.swt.widgets.Text(dialogShell,
            org.eclipse.swt.SWT.MULTI | org.eclipse.swt.SWT.READ_ONLY | org.eclipse.swt.SWT.V_SCROLL | org.eclipse.swt.SWT.WRAP);
        helpText.setLayoutData(new org.eclipse.swt.layout.GridData(
            org.eclipse.swt.SWT.FILL, org.eclipse.swt.SWT.FILL, true, true));
        helpText.setBackground(shell.getDisplay().getSystemColor(org.eclipse.swt.SWT.COLOR_WHITE));
        helpText.setText(i.tr("settings.help.content"));

        org.eclipse.swt.widgets.Button closeButton = new org.eclipse.swt.widgets.Button(dialogShell, org.eclipse.swt.SWT.PUSH);
        closeButton.setText(i.tr("abbrechen"));
        closeButton.setLayoutData(new org.eclipse.swt.layout.GridData(
            org.eclipse.swt.SWT.END, org.eclipse.swt.SWT.TOP, false, false));
        closeButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                dialogShell.dispose();
            }
        });

        dialogShell.open();
    }
}
