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

        LabelGroup empKeywordsGroup = new LabelGroup(container.getComposite(), i.tr("settings.keywords.empfaenger"));
        final de.willuhn.jameica.gui.input.TextInput empKeywordsInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getEmpfaengerKeywords());
        empKeywordsGroup.addLabelPair(i.tr("settings.keywords.list"), empKeywordsInput);

        LabelGroup zweckKeywordsGroup = new LabelGroup(container.getComposite(), i.tr("settings.keywords.verwendungszweck"));
        final de.willuhn.jameica.gui.input.TextInput zweckKeywordsInput =
            new de.willuhn.jameica.gui.input.TextInput(settings.getVerwendungszweckKeywords());
        zweckKeywordsGroup.addLabelPair(i.tr("settings.keywords.list"), zweckKeywordsInput);

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
}
