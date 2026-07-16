#!/bin/bash
# fix-webcam-permission.sh
# Automatically adds NSCameraUsageDescription to Jameica's Info.plist
# Required for webcam functionality on macOS Big Sur and later

PLIST="/Applications/jameica.app/Info.plist"
DESCRIPTION="Jameica benötigt Zugriff auf die Webcam, um QR-Codes zu scannen."

echo "=== Jameica Webcam Permission Fix ==="
echo ""

# Check if Jameica is installed
if [ ! -d "/Applications/jameica.app" ]; then
    echo "Fehler: Jameica.app nicht in /Applications gefunden."
    echo "Bitte Passen Sie den Pfad in diesem Script an."
    exit 1
fi

# Check if Info.plist exists
if [ ! -f "$PLIST" ]; then
    echo "Fehler: Info.plist nicht gefunden unter $PLIST"
    exit 1
fi

# Check if NSCameraUsageDescription already exists
if grep -q "NSCameraUsageDescription" "$PLIST"; then
    echo "Info: NSCameraUsageDescription ist bereits vorhanden."
    echo "Keine Änderungen erforderlich."
    exit 0
fi

# Add NSCameraUsageDescription
echo "Füge NSCameraUsageDescription hinzu..."
/usr/libexec/PlistBuddy -c "Add :NSCameraUsageDescription string '$DESCRIPTION'" "$PLIST"

if [ $? -eq 0 ]; then
    echo "Erfolg! NSCameraUsageDescription wurde hinzugefügt."
    echo ""
    echo "Bitte starten Sie Jameica neu."
    echo "Beim nächsten Webcam-Versuch erscheint ein Berechtigungsdialog."
else
    echo "Fehler: Konnte NSCameraUsageDescription nicht hinzufügen."
    echo "Möglicherweise benötigen Sie root-Rechte."
    echo "Versuchen Sie: sudo $0"
    exit 1
fi
