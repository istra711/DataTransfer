# DataTransfer Plugin - Projekt-Prompt

## Projektübersicht

Kombiniertes Jameica/Hibiscus-Plugin zum Lesen von SEPA-Zahlungsdaten aus QR-Codes und OCR (Rechnungen). Automatische Erkennung des Quellentyps.

- Plugin-Name: `hbci.datatransfer`
- Package: `de.willuhn.jameica.hbci.datatransfer`
- Repository: https://github.com/istra711/DataTransfer (privat)
- Jameica-Version: 2.12.0+
- Hibiscus-Version: 2.12.0+
- Java: 8+ (source/target)

## Verzeichnisstruktur

```
DataTransfer/
├── plugin.xml                    # Jameica Plugin-Manifest
├── src/
│   ├── de/willuhn/jameica/hbci/datatransfer/
│   │   ├── DataTransferPlugin.java      # Plugin-Einstiegspunkt
│   │   ├── DataTransferIO.java          # IO-Registry (Importer)
│   │   ├── OcrSettings.java             # OCR-Einstellungen
│   │   ├── action/
│   │   │   ├── FileAction.java          # Datei-Eingabe
│   │   │   ├── ClipboardAction.java     # Zwischenablage
│   │   │   ├── WebcamAction.java        # Webcam
│   │   │   └── SettingsAction.java      # Einstellungen
│   │   ├── gui/
│   │   │   ├── InvoiceView.java         # OCR-Ansicht
│   │   │   ├── QRCodeView.java          # QR-Code-Ansicht
│   │   │   ├── InvoiceDebugView.java    # Debug-Ansicht
│   │   │   └── SettingsView.java        # Einstellungen
│   │   ├── model/
│   │   │   └── TransferData.java        # Einheitliches Datenmodell
│   │   └── parser/
│   │       ├── SmartDetector.java       # Auto-Erkennung
│   │       ├── OcrEngine.java           # Tesseract-Wrapper
│   │       ├── InvoiceTextParser.java   # Regex-Parser
│   │       ├── EpcParser.java           # EPC (BCD) Parser
│   │       ├── EmvParser.java           # EMV (TLV) Parser
│   │       └── QrCodeSelector.java      # Multi-QR Auswahl
│   └── lang/
│       ├── hbci_datatransfer_messages_de_DE.properties
│       └── hbci_datatransfer_messages_en.properties
├── bilder/                       # Icons (Quelle)
├── lib/                          # Abhängigkeiten (JARs)
└── dist/                         # Build-Ausgabe
```

## Wichtige Jameica-Regeln

### 1. Icon-Pfade in plugin.xml
```xml
<!-- FALSCH: img/ Prefix wird nicht aufgelöst -->
icon-close="img/datatransfer-icon.png"

<!-- RICHTIG: Nur Dateiname, Jameica sucht in Unterverzeichnissen -->
icon-close="datatransfer-icon.png"
```
- Jameica sucht Icons im Plugin-Root UND in Unterverzeichnissen (wie `img/`)
- Der `img/` Prefix in Referenzen darf NICHT verwendet werden
- Icons können in `img/` Unterverzeichnis bleiben

### 2. Sprachdateien
- Benennung: `hbci_datatransfer_messages_de_DE.properties` (nicht `messages_de_DE`)
- Immer mit Plugin-Name als Prefix
- Nur Latein-Zeichen, keine Umlaute direkten in Properties (Escape: `\u00FC` für ü)

### 3. Plugin.xml Struktur
```xml
<plugin name="hbci.datatransfer" version="X.Y.Z"
        class="de.willuhn.jameica.hbci.datatransfer.DataTransferPlugin">
  
  <requires jameica="2.0+" hibiscus="2.0+" />
  <depends>hibiscus</depends>  <!-- WICHTIG: Abhängigkeit deklarieren -->
  
  <classfinder>
    <include>datatransfer\.jar</include>
  </classfinder>
  
  <navigation>
    <item id="hibiscus.navi.sepatransfer">
      <item name="navi.transfer" icon-close="datatransfer-icon.png" ...>
```

### 4. Kompilierung
- JDK 17 verwenden (`C:\Program Files\Java\jdk-17.0.0.1`)
- Classpath: `hibiscus.jar`, `jameica.jar`, `de_willuhn_ds.jar`, `de_willuhn_util.jar`, `swt.jar`, `lib\*`
- Compile-Befehl:
```powershell
& "C:\Program Files\Java\jdk-17.0.0.1\bin\javac.exe" -source 1.8 -target 1.8 `
  -cp "hibiscus.jar;jameica.jar;de_willuhn_ds.jar;de_willuhn_util.jar;swt.jar;lib\*" `
  -d build\classes -encoding UTF-8 `
  (Get-ChildItem src -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName)
```

### 5. Jameica Portable - Workdir
- Config-Datei: `C:\Users\istra\.jameica.properties`
- Workdir wird dort definiert: `dir=C\:\\Users\\istra\\.jameica`
- Log-Datei: Im workdir unter `Data\jameica\jameica.log`
- **ACHTUNG:** Test-Version teilt sich Config mit Produktion!

## Bekannte Probleme und Lösungen

### Problem: Importer wird nicht erkannt
**Ursache:** Jameica 2.12.0 verwendet ClassFinder nur innerhalb des Hibiscus-Plugins.
**Lösung:** Hibiscus-Commit `cbbce4a` nötig (globaler ClassFinder).
**Workaround:** Menu-Lösung unter `Zahlungsverkehr > Daten-Transfer` verwenden.

### Problem: NoClassDefFoundError bei DataTransferIO
**Ursache:** DataTransfer wird VOR Hibiscus geladen.
**Lösung:** `<depends>hibiscus</depends>` in plugin.xml hinzufügen.

### Problem: Icons nicht sichtbar
**Ursache:** `img/` Prefix in plugin.xml Referenzen.
**Lösung:** Nur Dateinamen verwenden, Jameica sucht automatisch in Unterverzeichnissen.

### Problem: Umlaute in Dialogen kaputt
**Ursache:** Direkte Umlaute in Properties-Dateien.
**Lösung:** Escape-Sequenzen verwenden oder Properties als UTF-8 speichern.

## Test-Verzeichnis

- Pfad: `G:\jameica_portable_test\`
- Plugins: `G:\jameica_portable_test\jameica\plugins\`
- Log: `G:\jameica_portable_test\Data\jameica\jameica.log`
- Config: `C:\Users\istra\.jameica.properties` (shared mit Produktion!)

## Build-Prozess

1. Java-Dateien kompilieren (siehe Kompilierung oben)
2. JAR erstellen:
```powershell
& "C:\Program Files\Java\jdk-17.0.0.1\bin\jar.exe" cf dist\hbci.datatransfer\datatransfer.jar `
  -C build\classes . -C build lang
```
3. Plugin-Ordner strukturieren:
   - `datatransfer.jar`
   - `plugin.xml`
   - `img/` (Icons)
   - `lang/` (Sprachdateien)
   - `lib/` (Abhängigkeiten)
4. ZIP erstellen für Distribution

## Abhängigkeiten (lib/)

- tess4j 5.19.0 - Tesseract OCR
- pdfbox 3.0.7 - PDF-Textextraktion
- zxing (core/javase) 3.5.3 - QR-Code
- jna 5.18.1 - Native Access
- slf4j 2.0.18 - Logging

## Git-Workflow

```bash
git add -A
git commit -m "Beschreibung"
git push
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
gh release create vX.Y.Z --title "vX.Y.Z" --notes "..."
gh release upload vX.Y.Z dist\hbci.datatransfer.zip
```

## Hibiscus API - Wichtige Klassen

- `de.willuhn.jameica.hbci.io.IO` - Interface für Import/Export
- `de.willuhn.jameica.hbci.io.Importer` - Importer-Interface
- `de.willuhn.jameica.hbci.io.AbstractImporter` - Basis-Klasse
- `de.willuhn.jameica.hbci.io.IOFormat` - Dateiformat-Interface
- `de.willuhn.jameica.hbci.io.IORegistry` - Registrierung
- `de.willuhn.jameica.hbci.rmi.Ueberweisung` - Überweisung-Interface
- `de.willuhn.jameica.hbci.rmi.Konto` - Konto-Interface
- `de.willuhn.jameica.hbci.Settings` - Hibiscus-Einstellungen
- `de.willuhn.jameica.hbci.rmi.HBCIDBService` - DB-Service

### Ueberweisung erstellen
```java
de.willuhn.jameica.hbci.rmi.HBCIDBService dbService = 
    de.willuhn.jameica.hbci.Settings.getDBService();
Ueberweisung u = dbService.createObject(Ueberweisung.class, null);
u.setGegenkontoNummer(iban);
u.setGegenkontoBLZ(bic);
u.setGegenkontoName(name);
u.setBetrag(betrag);
u.setZweck(zweck);
u.store();
```

## Aktueller Stand

### Funktioniert
- QR-Code-Erkennung (EPC/EMV)
- OCR mit Tesseract
- Multi-QR in PDFs
- Case-insensitive Schlüsselwortsuche
- Hilfe-Button in Einstellungen
- Doppelklick-Editor für Schlüsselwörter

### In Arbeit / Test
- Importer-Integration (IORegistry)
- Erfordert Hibiscus-Commit `cbbce4a`

### Nicht implementiert
- Webcam-Funktion (nur QR, kein OCR)
