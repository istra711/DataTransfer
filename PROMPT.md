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

### 0. Plugin-ZIP Struktur (KRITISCH!)

Die ZIP-Datei muss einer strengen Struktur folgen:

```
pluginname/                    ← Genau EIN Ordner auf oberster Ebene
├── plugin.xml                 ← Muss im Hauptordner liegen
├── datatransfer.jar           ← Fat-JAR (8MB, nicht Thin-JAR 75KB!)
├── img/                       ← Explizite Verzeichnis-Einträge nötig!
│   └── icon.png
├── lang/
│   └── messages.properties
└── lib/
    └── dependency.jar
```

**Fehler die vermieden werden müssen:**
1. ❌ Dateien direkt auf oberster Ebene → `contains invalid file`
2. ❌ Windows-Backslashes `\` im ZIP → Jameica prüft auf `/`
3. ❌ Fehlende explizite Verzeichnis-Einträge → `plugin zip-file empty`
4. ❌ Thin-JAR (nur Klassen) → Muss Fat-JAR sein

**Richtige ZIP-Erstellung mit Python:**
```python
import zipfile, os
src_dir = r'path/to/plugin_folder'
zip_path = r'output.zip'
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
    dirs_added = set()
    for root, dirs, files in os.walk(src_dir):
        for d in dirs:
            arcname = os.path.relpath(os.path.join(root, d), src_dir).replace(os.sep, '/') + '/'
            if arcname not in dirs_added:
                zipf.writestr(zipfile.ZipInfo(arcname), '')
                dirs_added.add(arcname)
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            file_path = os.path.join(root, file)
            arcname = os.path.relpath(file_path, src_dir).replace(os.sep, '/')
            zipf.write(file_path, arcname)
```

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

**Plugin mit Ant bauen (empfohlen):**
```bash
ant -f build.xml clean zip
```

Dies erstellt:
- `dist/datatransfer.jar` - Fat-JAR mit allen Klassen
- `dist/hbci.datatransfer-X.Y.Z.zip` - Installations-ZIP mit korrekter Struktur

**Ant's `zip`-Task erstellt automatisch:**
- Explizite Verzeichnis-Einträge
- Forward-Slashes (auch auf Windows)
- Korrekte Plugin-Struktur

**ZIP manuell erstellen (nur wenn nötig):**
```powershell
# NICHT verwenden: Compress-Archive (erstellt keine Verzeichnis-Einträge, nutzt \)
# Verwende stattdessen Ant oder 7-Zip mit korrekten Einstellungen
```

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
```

**Release erstellen:**
```bash
gh release create vX.Y.Z --title "vX.Y.Z" --notes "Release notes"
gh release upload vX.Y.Z hbci.datatransfer-X.Y.Z.zip
gh release upload vX.Y.Z hibiscus-patched.zip
```

**Repository sichtbarkeit:**
```bash
gh repo edit istra711/DataTransfer --visibility public --accept-visibility-change-consequences
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
- Importer-Integration (IORegistry) - funktioniert mit gepatchtem Hibiscus
- Hibiscus Import-Dialog Integration (v2.3.0)

### Nicht implementiert
- Webcam-Funktion (nur QR, kein OCR)

## Bekannte Probleme und Lösungen

### Problem: Plugin-ZIP wird nicht erkannt
**Ursache:** Falsche ZIP-Struktur (Dateien auf oberster Ebene, fehlende Verzeichnis-Einträge, Thin-JAR).
**Lösung:** Siehe "0. Plugin-ZIP Struktur" oben. Fat-JAR verwenden, explizite Verzeichnis-Einträge, Forward-Slashes.

### Problem: Importer wird nicht erkannt
**Ursache:** Jameica 2.12.0 verwendet ClassFinder nur innerhalb des Hibiscus-Plugins.
**Lösung:** Hibiscus-Commit `cbbce4a` nötig (globaler ClassFinder).
**Workaround:** Menu-Lösung unter `Zahlungsverkehr > Daten-Transfer` verwenden.

### Problem: NoClassDefFoundError bei DataTransferIO
**Ursache:** DataTransfer wird VOR Hibiscus geladen.
**Lösung:** `<requires><import plugin="hibiscus"/></requires>` in plugin.xml.

### Problem: Icons nicht sichtbar
**Ursache:** `img/` Prefix in plugin.xml Referenzen.
**Lösung:** Nur Dateinamen verwenden, Jameica sucht automatisch in Unterverzeichnissen.

### Problem: Umlaute in Dialogen kaputt
**Ursache:** Direkte Umlaute in Properties-Dateien.
**Lösung:** Escape-Sequenzen verwenden oder Properties als UTF-8 speichern.
