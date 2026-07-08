package de.willuhn.jameica.hbci.datatransfer.action;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

/**
 * Aktion zum Oeffnen der Einstellungen.
 */
public class SettingsAction implements Action {

    @Override
    public void handleAction(Object context) throws ApplicationException {
        GUI.startView(de.willuhn.jameica.hbci.datatransfer.gui.SettingsView.class, null);
    }
}
