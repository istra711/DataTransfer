# SEPA Data Transfer Plugin for Jameica/Hibiscus

Kombiniertes Jameica/Hibiscus-Plugin zum Lesen von SEPA-Zahlungsdaten aus QR-Codes und OCR (Rechnungen), mit automatischer Erkennung des Quellentyps.

## Features

- **Automatische Erkennung**: Erkennt automatisch QR-Code (EPC/EMV) oder OCR-Text
- **Eingabemethoden**:
  - **Datei** - PDF-Rechnungen oder Bilddateien laden (PNG, JPG, BMP, TIFF)
  - **Zwischenablage** - Bilder direkt aus der Zwischenablage einlesen
  - **Webcam** - QR-Codes live mit der Kamera scannen
- **QR-Code-Unterstützung**:
  - EPC (BCD) Format - Europäisches Payment Council Standardformat
  - EMV (TLV) Format - EMV-Standard aus Zahlungsterminalen
  - Multi-QR-Erkennung: Mehrere QR-Codes in einer PDF automatisch finden und auswählen
- **OCR-Unterstützung**:
  - Tesseract 5.5.2 (via tess4j 5.19.0)
  - PDF-Textextraktion (direkt und OCR-Fallback)
  - Konfigurierbare OCR-Einstellungen (OEM, PSM, DPI, Sprache, Whitelist/Blacklist)
- **Schlüsselwörter**:
  - Empfänger-Keywords: Automatische Erkennung des Empfängernamens
  - Verwendungszweck-Keywords: Automatische Erkennung des Verwendungszwecks
  - Case-insensitive Suche
  - Erweiterter Editor mit Doppelklick (zeilenweise Eingabe)
- **SEPA-Überweisung**: Direkte Erstellung von Überweisungsentwürfen in Hibiscus
- **Einstellungen**: Hilfe-Button mit detaillierter Erläuterung aller Optionen
- **Internationalisierung**: Vollständige deutsche und englische Sprachunterstützung

## Voraussetzungen

- Jameica 2.12.0+
- Hibiscus 2.12.0+
- Java 8+
- Tesseract OCR (im Plugin enthalten)

## Installation

1. Die neueste Version von der [Releases](https://github.com/istra711/DataTransfer/releases) Seite herunterladen
2. Jameica starten
3. Zu **Datei > Plugins online suchen... > Plugin manuell installieren...** navigieren
4. Die heruntergeladene ZIP-Datei auswählen
5. Jameica neu starten

## Benutzung

1. In Hibiscus zu **Zahlungsverkehr > Daten-Transfer** navigieren
2. Eine der Eingabemethoden wählen:
   - **Datei (PDF/Bild)** - Öffnet einen Dateidialog für PDF- oder Bilddateien
   - **Zwischenablage** - Liest Bild aus der Zwischenablage
   - **Webcam (QR)** - Öffnet die Kamera zum QR-Code-Scanning
3. Das Plugin erkennt automatisch den Quellentyp:
   - QR-Code gefunden → QR-Code-Überweisungsansicht wird geöffnet
   - Kein QR-Code → OCR wird durchgeführt und die OCR-Überweisungsansicht wird geöffnet
4. Die erkannten Daten überprüfen und bei Bedarf korrigieren
5. Auf **Überweisung anlegen** klicken, um einen Überweisungsentwurf in Hibiscus zu erstellen

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
├── OcrSettings.java                 # OCR-Einstellungen
├── action/
│   ├── FileAction.java              # Datei-Eingabe (PDF/Bild) mit Auto-Erkennung
│   ├── ClipboardAction.java         # Zwischenablage-Eingabe mit Auto-Erkennung
│   ├── WebcamAction.java            # Webcam QR-Code-Scanning
│   └── SettingsAction.java          # Einstellungsansicht öffnen
├── gui/
│   ├── InvoiceView.java             # OCR-Ansicht mit Rohtext-Panel
│   ├── QRCodeView.java              # QR-Code-Datenansicht
│   ├── InvoiceDebugView.java        # Debug-Ansicht für erkannte Daten
│   └── SettingsView.java            # Einstellungsansicht mit Hilfe
├── model/
│   └── TransferData.java            # Einheitliches Datenmodell
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

- Erstveröffentlichung des kombinierten Plugins
- Zusammenführung von OCRtransfer und QRtransfer
- Automatische QR/OCR-Erkennung
- Einheitliche Einstellungsansicht
- Vollständige i18n-Unterstützung (Deutsch und Englisch)

## Lizenz

GPL v3 - Siehe [LICENSE](LICENSE) für Details.
