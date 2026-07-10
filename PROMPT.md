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
5. ❌ PowerShell `Compress-Archive` → Erstellt keine Verzeichnis-Einträge, nutzt `\`

**Richtige ZIP-Erstellung mit Ant (empfohlen):**
```bash
ant -f build.xml clean zip
```
Ant's `zip`-Task erstellt automatisch:
- Explizite Verzeichnis-Einträge
- Forward-Slashes (auch auf Windows)
- Korrekte Plugin-Struktur

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
<?xml version="1.0" encoding="UTF-8"?>
<plugin xmlns="http://www.willuhn.de/schema/jameica-plugin"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.willuhn.de/schema/jameica-plugin http://www.willuhn.de/schema/jameica-plugin-1.5.xsd"
        name="hbci.datatransfer" version="X.Y.Z"
        class="de.willuhn.jameica.hbci.datatransfer.DataTransferPlugin">
  
  <requires jameica="2.10.0+">
    <import plugin="hibiscus" version="2.10.0+"/>
  </requires>
  
  <classfinder>
    <include>datatransfer\.jar</include>
  </classfinder>
  
  <navigation>
    <item id="datatransfer.navi" name="Daten-Transfer" icon-close="datatransfer-icon.png" icon-open="datatransfer-icon.png">
      <item id="datatransfer.navi.file" name="Datei (PDF/Bild)" icon-close="file-icon.png" icon-open="file-icon.png"
            action="de.willuhn.jameica.hbci.datatransfer.action.FileAction" />
      ...
    </item>
  </navigation>
  
  <extension point="jameica.extension">
    <class>de.willuhn.jameica.hbci.datatransfer.DataTransferIO</class>
  </extension>
</plugin>
```

**Wichtig:** `<depends>` NICHT verwenden → `<requires><import>` verwenden!

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

## Funktionierende Vorlage

Es gibt eine funktionierende Plugin-ZIP im Repo:
- `hbci.datatransfer.zip` (im Projekt-Root)
- Enthält die alte Version (v2.0.0) mit `datatransfer.jar` (Fat-JAR, 8MB)
- Kann als Vorlage dienen: ZIP entpacken, `plugin.xml` ersetzen, neu ZIPpen

**Achtung:** Die JAR in dieser ZIP heißt `datatransfer.jar` (nicht `hbci.datatransfer.jar`)

## Build-Prozess

### Schnell-Build (empfohlen für kleine Änderungen)

Von der funktionierenden Installation im Produktions-Verzeichnis kopieren:
```powershell
# 1. Funktionierende Installation als Vorlage
$src = "G:\jameica_portable_V1\jameica\plugins\hbci.datatransfer"
$dst = "C:\Users\istra\Documents\claude_ps\DataTransfer\release"

# 2. Kopieren
Copy-Item $src $dst -Recurse

# 3. Nur plugin.xml ersetzen (falls geändert)
Copy-Item "C:\Users\istra\Documents\claude_ps\DataTransfer\plugin.xml" "$dst\plugin.xml"

# 4. ZIP mit Ant erstellen (NICHT Compress-Archive!)
# Oder: Die funktionierende ZIP aus dem Repo nehmen und plugin.xml ersetzen
```

### Voll-Build mit Ant

```bash
# Ant muss konfiguriert sein (jameica.home in build.xml prüfen!)
ant -f build.xml clean zip
```

**Voraussetzung:** `build.xml` muss `jameica.home` auf das richtige Jameica-Verzeichnis zeigen.

### Was Ant erstellt:
- `dist/datatransfer.jar` - Fat-JAR mit allen Klassen
- `dist/hbci.datatransfer-X.Y.Z.zip` - Installations-ZIP mit korrekter Struktur

### ⚠️ NICHT verwenden:
- PowerShell `Compress-Archive` → Erstellt keine Verzeichnis-Einträge, nutzt `\`
- Manuelle ZIP-Erstellung ohne explizite Verzeichnis-Einträge

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
