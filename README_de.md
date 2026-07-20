# SEPA Data Transfer Plugin für Jameica/Hibiscus

**Nachfolger und Fusion des QRtransfer-Plugins und OCRtransfer-Plugins.** Dieses Plugin ersetzt beide Einzel-Plugins und bietet eine einheitliche Oberfläche für alle Zahlungsdaten-Imports.

Kombiniertes Jameica/Hibiscus-Plugin zum Lesen von SEPA-Zahlungsdaten aus QR-Codes und OCR (Rechnungen), mit automatischer Erkennung des Quellentyps.

## Was dieses Plugin ersetzt

| Altes Plugin | Funktion | Status |
|-------------|----------|--------|
| **QRtransfer** | QR-Code-Import (EPC/EMV) | ❌ Nicht mehr nötig |
| **OCRtransfer** | OCR-Import (Rechnungen) | ❌ Nicht mehr nötig |
| **DataTransfer** | Beides in einem + Import-Dialog | ✅ Dieses Plugin |

## Features

### Was dieses Plugin besser macht als die Einzel-Plugins

- **Ein Plugin statt zwei**: Installiert einmal, statt QRtransfer UND OCRtransfer separat zu installieren
- **Automatische Erkennung**: Keine manuelle Auswahl nötig - das Plugin erkennt automatisch ob QR-Code oder OCR
- **Hibiscus Import-Dialog**: Erscheint direkt im Import-Menü von Hibiscus ("Rechnungs-Datei(PDF/Image) - OCR/QR")
- **Review-Dialog**: Daten können vor dem Speichern geprüft und korrigiert werden
- **Einheitliche Einstellungen**: Keyword-Suche für beide Modi in einer Einstellungsansicht

![Navigation](img/screenshots/navigation.png)

### Eingabemethoden

- **Datei** - PDF-Rechnungen oder Bilddateien laden (PNG, JPG, BMP, TIFF)
- **Zwischenablage** - Bilder direkt aus der Zwischenablage einlesen
- **Webcam** - QR-Codes live mit der Kamera scannen

![Plugin-Menü](img/screenshots/menu.png)

### QR-Code-Unterstützung

- EPC (BCD) Format - Europäisches Payment Council Standardformat
- EMV (TLV) Format - EMV-Standard aus Zahlungsterminalen
- Multi-QR-Erkennung: Mehrere QR-Codes in einer PDF automatisch finden und auswählen

![QR-Code-Auswahl](img/screenshots/qr-selector.png)

### OCR-Unterstützung

- Tesseract 5.5.2 (via tess4j 5.19.0)
- PDF-Textextraktion (direkt und OCR-Fallback)
- Konfigurierbare OCR-Einstellungen (OEM, PSM, DPI, Sprache, Whitelist/Blacklist)

![OCR-Einstellungen](img/screenshots/ocr-settings.png)

### Schlüsselwörter

- Empfänger-Keywords: Automatische Erkennung des Empfängernamens
- Verwendungszweck-Keywords: Automatische Erkennung des Verwendungszwecks
- Case-insensitive Suche
- Erweiterter Editor mit Doppelklick (zeilenweise Eingabe)

![Keyword-Editor](img/screenshots/keyword-editor.png)

### SEPA-Überweisung

Direkte Erstellung von Überweisungsentwürfen in Hibiscus

![OCR-Überweisungsansicht](img/screenshots/ocr-transfer.png)

### Weitere Features

- **Einstellungen**: Hilfe-Button mit detaillierter Erläuterung aller Optionen
- **Internationalisierung**: Vollständige deutsche und englische Sprachunterstützung

## Voraussetzungen

- Jameica 2.10.0+
- Hibiscus 2.10.0+ (**mit ClassFinder-Patch** - siehe unten)
- Java 8+
- Tesseract OCR (im Plugin enthalten, erfordert aber `tessdata/deu.traineddata` - siehe unten)

### OCR Tessdata

Das Plugin enthält Tesseract OCR, benötigt aber Sprach-Trainingsdaten für die Funktion. Die Datei `tessdata/deu.traineddata` (deutsche Sprache, ~8.6MB) muss in der Plugin-ZIP enthalten sein.

Wenn OCR mit "Kein Text erkannt" fehlschlägt, fehlt möglicherweise die Tessdata-Datei. Download unter:
```bash
mkdir tessdata
curl -L -o tessdata/deu.traineddata https://github.com/tesseract-ocr/tessdata_best/raw/main/deu.traineddata
```

### macOS: Tesseract-Installation (OCR)

Unter macOS muss Tesseract zusätzlich installiert werden. Das Plugin nutzt tess4j, das für Windows und Linux die nativen Tesseract-Libraries mitliefert, für macOS aber nicht.

**Installation mit Homebrew:**
```bash
brew install tesseract
brew install tesseract-lang-deu
```

`brew install tesseract-lang-deu` installiert die deutsche Sprachunterstützung. Die Trainingsdateien werden dann nach `/opt/homebrew/share/tessdata/` installiert.

**Falls Homebrew nicht installiert ist:**
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install tesseract
brew install tesseract-lang-deu
```

**Hinweis:** Ohne diese Installation erscheint der Fehler `UnsatisfiedLinkError: Unable to load library 'tesseract'` im Jameica-Log.

### macOS Webcam-Einrichtung

Unter macOS erfordert die Webcam den `NSCameraUsageDescription`-Schlüssel in der `Info.plist` von Jameica. Ohne diesen Eintrag stürzt macOS Jameica sofort ab, wenn auf die Kamera zugegriffen wird (es erscheint kein Berechtigungsdialog).

**Warum das nötig ist:** Seit macOS Big Sur (11.0) hat Apple das TCC-Datenschutzsystem (Transparency, Consent, and Control) verschärft. Apps ohne Kamera-Berechtigungsbeschreibung werden sofort mit `SIGABRT` beendet, anstatt einen Berechtigungsdialog anzuzeigen.

**Schnell-Lösung (Empfohlen):**

Das mitgelieferte Script im Terminal ausführen:
```bash
chmod +x fix-webcam-permission.sh
./fix-webcam-permission.sh
```

**Manuelle Lösung:**

1. Finder öffnen und zu `/Applications/jameica.app` navigieren
2. Rechtsklick auf `jameica.app` und "Paketinhalt anzeigen" wählen
3. `Contents/Info.plist` mit einem Texteditor öffnen
4. Folgenden Eintrag vor dem schließenden `</dict>`-Tag einfügen:
   ```xml
   <key>NSCameraUsageDescription</key>
   <string>Jameica benötigt Zugriff auf die Webcam, um QR-Codes zu scannen.</string>
   ```
5. Datei speichern und Jameica neu starten

**Alternative (Terminal):**
```bash
/usr/libexec/PlistBuddy -c "Add :NSCameraUsageDescription string 'Jameica benötigt Zugriff auf die Webcam, um QR-Codes zu scannen.'" /Applications/jameica.app/Contents/Info.plist
```

Nach dieser Änderung zeigt macOS beim ersten Webcam-Versuch in Jameica einen Berechtigungsdialog an.

### Hibiscus ClassFinder-Patch

Dieses Plugin erfordert eine gepatchte Version von Hibiscus, die einen globalen ClassFinder für die Plugin-Erkennung verwendet. Das Standard-Hibiscus findet nur Plugins, die vom eigenen ClassLoader geladen werden, was verhindert, dass externe Plugins wie DataTransfer im Import-Dialog erscheinen.

**Empfohlen: Nightly-Build verwenden**

Die einfachste Lösung ist der Wechsel zum aktuellen Nightly-Build von Hibiscus. Dieser enthält bereits die erforderlichen Änderungen:
👉 [Nightly-Builds nutzen](https://www.willuhn.de/wiki/doku.php?id=downloads_nutzen)

**Alternative: Geflickte Hibiscus-Version**

Falls Sie nicht zum Nightly-Build wechseln möchten, steht auch eine gepatchte Version zur Verfügung:
[hibiscus-patched.zip](https://github.com/istra711/DataTransfer/releases/download/v2.3.0/hibiscus-patched.zip)

**Installation:**
1. Sichern Sie Ihre bestehende `hibiscus.jar` aus `jameica/plugins/hibiscus/`
2. Entpacken Sie die heruntergeladene `hibiscus-patched.zip`
3. Ersetzen Sie `hibiscus.jar` in `jameica/plugins/hibiscus/` durch die gepatchte Version
4. Starten Sie Jameica neu

**Hinweis:** Das Plugin funktioniert auch mit dem derzeitigen offiziellen Hibiscus-Release, aber die Importer-Funktionen (z.B. "Rechnungs-Datei(PDF/Image) - OCR/QR" im Import-Dialog) werden nicht angezeigt. Sie können das Plugin weiterhin über das Menü verwenden (Datei > Rechnungsdatei laden, etc.).

## Installation

1. Die richtige Version für Ihre Plattform von der [Releases](https://github.com/istra711/DataTransfer) Seite herunterladen:
   - **Windows**: `hbci.datatransfer-2.4.5-windows.zip`
   - **Linux**: `hbci.datatransfer-2.4.5-linux.zip`
   - **macOS Intel**: `hbci.datatransfer-2.4.5-macosx.zip` (x86_64)
   - **macOS Apple Silicon**: `hbci.datatransfer-2.4.5-macosx-arm64.zip` (M1/M2/M3/M4)
2. Jameica starten
3. Zu **Datei > Plugins online suchen... > Plugin manuell installieren...** navigieren
4. Die heruntergeladene ZIP-Datei auswählen
5. Jameica neu starten

## Benutzung

### Über Hibiscus Import-Dialog

1. In Hibiscus zu **Überweisungen > Import** navigieren
2. **"Rechnungs-Datei(PDF/Image) - OCR/QR"** auswählen

![Import-Dialog](img/screenshots/import-dialog.png)

3. Eine PDF- oder Bilddatei auswählen
4. Konto auswählen (falls nicht bereits gesetzt)
5. Daten werden erkannt und im Review-Dialog angezeigt
6. Daten prüfen und bei Bedarf korrigieren
7. Auf **Überweisung erstellen** klicken

### Über Plugin-Menü

1. Im Jameica-Menü **Daten-Transfer** wählen
2. Eine der Eingabemethoden wählen:
   - **Datei** - Öffnet Dateidialog für PDF- oder Bilddateien
   - **Zwischenablage** - Liest Bild aus der Zwischenablage
   - **Webcam** - Öffnet die Kamera zum QR-Code-Scanning
3. Das Plugin erkennt automatisch den Quellentyp:
   - QR-Code gefunden → QR-Code-Review-Ansicht wird geöffnet
   - Kein QR-Code → OCR-Review-Ansicht wird geöffnet
4. Die erkannten Daten überprüfen und bei Bedarf korrigieren
5. Auf **Überweisung erstellen** klicken

### Tastenkürzel

| Aktion | Kürzel |
|--------|--------|
| Datei (PDF/Bild) | `Ctrl+Shift+D` |
| Zwischenablage | `Ctrl+Shift+V` |
| Webcam (QR) | `Ctrl+Shift+W` |

## Technische Details

### Architektur

```
src/de/willuhn/jameica/hbci/datatransfer/
├── DataTransferPlugin.java          # Plugin-Einstiegspunkt
├── DataTransferIO.java              # IORegistry-Registrierung (nur Datei-Importer)
├── DataTransferBaseImporter.java    # Basis-Importer mit Review-Dialog
├── DataTransferFileImporter.java    # Datei-Importer
├── OcrSettings.java                 # OCR-Einstellungen
├── action/
│   ├── FileAction.java              # Datei-Eingabe (PDF/Bild) mit Auto-Erkennung
│   ├── ClipboardAction.java         # Zwischenablage-Eingabe mit Auto-Erkennung
│   ├── WebcamAction.java            # Webcam QR-Code-Scanning
│   └── SettingsAction.java          # Einstellungsansicht öffnen
├── gui/
│   ├── InvoiceView.java             # OCR-Review-Ansicht mit Rohtext-Panel
│   ├── QRCodeView.java              # QR-Code-Review-Ansicht
│   ├── InvoiceDebugView.java        # Debug-Ansicht für erkannte Daten
│   └── SettingsView.java            # Einstellungsansicht mit Hilfe
├── model/
│   ├── TransferData.java            # Einheitliches Datenmodell
│   └── TransferDataHolder.java      # Hält TransferData + Konto für Views
└── parser/
    ├── SmartDetector.java           # Auto-Erkennung (QR vs OCR)
    ├── OcrEngine.java               # Tesseract-Wrapper
    ├── InvoiceTextParser.java       # Regex-Parser für OCR-Text
    ├── QrCodeParser.java            # Parser-Interface
    ├── EpcParser.java               # EPC (BCD) Format-Parser
    ├── EmvParser.java               # EMV (TLV) Format-Parser
    └── QrCodeSelector.java          # Multi-QR-Erkennung und Auswahl
```

### Abhängigkeiten

- **tess4j** 5.19.0 - Java-Wrapper für Tesseract 5.5.2 OCR
- **PDFBox** 3.0.7 - PDF-Textextraktion und Rendering
- **ZXing** 3.5.3 - QR-Code-Dekodierung (aus Jameica/Hibiscus)
- **JNA** 5.18.1 - Native Library-Zugriff
- **SLF4J** 2.0.18 - Logging API

### Smart Detection Logik

Das Plugin verwendet einen intelligenten Erkennungsalgorithmus:

1. **Datei-Eingabe**:
   - PDF-Dateien: Zuerst QR-Code → Textextraktion → OCR-Fallback
   - Bilddateien: Zuerst QR-Code → OCR-Fallback

2. **Zwischenablage-Eingabe**:
   - Zuerst QR-Code → OCR-Fallback

3. **Webcam-Eingabe**:
   - Nur QR-Code (kein OCR)

## Versionshistorie

### v2.4.5

- **Fix für OCR auf macOS**: `tessdata/deu.traineddata` (deutsche Sprach-Trainingsdaten) wird jetzt in der Plugin-ZIP ausgeliefert - OCR schlug fehl, da Tesseract keine Sprachdaten hatte
- **Fix für Tessdata-Pfad**: Tessdata-Pfad wird jetzt relativ zum Plugin-Verzeichnis aufgelöst (statt relativ zum CWD)

### v2.4.4

- **Fix für macOS ARM Webcam-Absturz**: Fehlende `opencv-4.7.0-1.5.9.jar` (plattformunabhängig) zur macOS ARM ZIP hinzugefügt, die auf Apple Silicon Macs `ClassNotFoundException: org.bytedeco.opencv.opencv_videoio.VideoCapture` verursacht hat
- **macOS Webcam-Einrichtung**: Dokumentation hinzugefügt, die die Anforderung des `NSCameraUsageDescription`-Schlüssels in der Info.plist von Jameica unter macOS erklärt

### v2.4.2

- **macOS Webcam-Fix**: Webcam läuft jetzt in separatem Thread, vermeidet den SWT/Swing-Threading-Konflikt, der auf macOS zum Absturz von Jameica geführt hat
- **macOS ARM-Unterstützung**: Separater Download für Apple Silicon (M1/M2/M3/M4) Macs
- Vereinfachter Geraete-Auswahl-Dialog (manuelle Eingabe 0, 1, 2...)
- Korrekte Plugin-Struktur: `plugin.xml`, `lang/`, `img/` jetzt auf oberster Ebene der ZIP (nicht in der JAR)

### v2.4.1

- **Classfinder-Regex korrigiert**: `plugin.xml` matched jetzt den tatsächlichen JAR-Namen (`datatransfer.jar`) statt des alten Namens
- Webcam-Geraeteliste: Nutzt `FrameGrabber.list` zur Geraeteauswahl mit Fallback auf manuelle Eingabe

### v2.4.0

- **Sauberes Build**: Verschachtelte `datatransfer.jar` innerhalb der Haupt-JAR entfernt — behebt Classloader-Konflikte, bei denen Jameica alte Klassen aus der inneren JAR geladen hat
- **Classfinder-Regex korrigiert**: `plugin.xml` matched jetzt den tatsächlichen JAR-Namen (`hbci.datatransfer.jar`) statt des alten inneren JAR-Namens
- **Webcam-Zuverlässigkeit**: Open-Timeout auf 20 Sekunden erhöht, globaler ClassLoader (`Application.getClassLoader()`) für Webcam-JavaCV-Klassen, verbessertes Error-Logging mit `System.err.println` + `Logger.error` in allen Fehlernpfaden

### v2.3.0

- Hibiscus Import-Dialog Integration: "Rechnungs-Datei(PDF/Image) - OCR/QR" erscheint im Import-Dropdown
- Review-Dialog vor Überweisungsanlegung: Daten können vor dem Speichern geprüft und korrigiert werden
- Fortschrittsbalken mit Status-Text während der Verarbeitung
- Clipboard und Webcam nur noch über Plugin-Menü (nicht über Import-Dialog)
- SmartDetector: `detectFromStream()` funktioniert jetzt mit beliebigen Dateitypen (keine .tmp-Erkennungs-Fehler mehr)
- Neue TransferDataHolder-Klasse zur Übergabe von TransferData + Konto an die Views

### v2.2.0-test (Entwicklung)

- Importer-Integration für IORegistry (Test-Version)
- Erfordert aktuelle Hibiscus-Entwicklungsversion
- Details: https://github.com/willuhn/hibiscus/commit/cbbce4ad6abafc652011e5c777338cc74b786d38

### v2.1.0

- Hilfe-Button und Hilfetext-Dialog in den Einstellungen
- Doppelklick-Editor für Schlüsselwörter (zeilenweise Eingabe)
- Case-insensitive Schlüsselwortsuche
- Multi-QR-Erkennung in PDFs (alle Seiten scannen)
- i18n-Fallback für QR-Auswahldialog
- OCR-Ansicht: "IBAN/BIC" zu "Empfänger-Konto" geändert
- OCR-Ansicht: Label "Empfänger" zu "Name" geändert
- QR-Code-Ansicht: Stadt-Eingabefeld entfernt
- Einstellungen: Schlüsselwörter untereinander angeordnet

### v2.0.0

- **Fusion von QRtransfer und OCRtransfer** zu einem Plugin
- Automatische QR/OCR-Erkennung
- Einheitliche Einstellungsansicht
- Vollständige i18n-Unterstützung (Deutsch und Englisch)

## Lizenz

GPL v3 - Siehe [LICENSE](LICENSE) für Details.
