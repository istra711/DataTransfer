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
- Tesseract OCR (im Plugin enthalten)

### Hibiscus ClassFinder-Patch

Dieses Plugin erfordert eine gepatchte Version von Hibiscus, die einen globalen ClassFinder für die Plugin-Erkennung verwendet. Das Standard-Hibiscus findet nur Plugins, die vom eigenen ClassLoader geladen werden, was verhindert, dass externe Plugins wie DataTransfer im Import-Dialog erscheinen.

**Download des gepatchten Hibiscus:** [hibiscus-patched.zip](https://github.com/istra711/DataTransfer/releases/download/v2.3.0/hibiscus-patched.zip)

**Installation:**
1. Sichern Sie Ihre bestehende `hibiscus.jar` aus `jameica/plugins/hibiscus/`
2. Entpacken Sie die heruntergeladene `hibiscus-patched.zip`
3. Ersetzen Sie `hibiscus.jar` in `jameica/plugins/hibiscus/` durch die gepatchte Version
4. Starten Sie Jameica neu

Der Patch ändert `IORegistry.java`, um `Application.getClassLoader().getClassFinder()` statt des plugin-spezifischen ClassLoaders zu verwenden, sodass alle installierten Plugins gefunden werden können.

**Hinweis:** Die gepatchte Hibiscus-Version ist **nicht zwingend erforderlich**. Das Plugin funktioniert auch mit dem derzeitigen offiziellen Hibiscus-Release, aber die Importer-Funktionen (z.B. "Rechnungs-Datei(PDF/Image) - OCR/QR" im Import-Dialog) werden nicht angezeigt. Sie können das Plugin weiterhin über das Menü verwenden (Datei > Rechnungsdatei laden, etc.).

Die erforderlichen Änderungen wurden vom Hibiscus-Entwickler vorgeschlagen (siehe [Hibiscus Commit cbbce4ad](https://github.com/willuhn/hibiscus/commit/cbbce4ad6abafc652011e5c777338cc74b786d38)) und werden in einer zukünftigen offiziellen Hibiscus-Version enthalten sein. Sobald diese Version verfügbar ist, wird die gepatchte Hibiscus-Version nicht mehr benötigt.

## Installation

1. Die richtige Version für Ihre Plattform von der [Releases](https://github.com/istra711/DataTransfer) Seite herunterladen:
   - **Windows**: `hbci.datatransfer-2.4.0.zip`
   - **Linux**: `hbci.datatransfer-2.4.0-linux.zip`
   - **macOS**: `hbci.datatransfer-2.4.0-macos.zip`
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

### v2.4.2

- **macOS Webcam-Fix**: Webcam läuft jetzt in separatem Thread, vermeidet den SWT/Swing-Threading-Konflikt, der auf macOS zum Absturz von Jameica geführt hat
- Vereinfachter Geraete-Auswahl-Dialog (manuelle Eingabe 0, 1, 2...)

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
