# DataTransfer Plugin - Projekt-Prompt

## Projektübersicht

Kombiniertes Jameica/Hibiscus-Plugin zum Lesen von SEPA-Zahlungsdaten aus QR-Codes und OCR (Rechnungen). Automatische Erkennung des Quellentyps.

- Plugin-Name: `hbci.datatransfer`
- Package: `de.willuhn.jameica.hbci.datatransfer`
- Repository: https://github.com/istra711/DataTransfer (öffentlich)
- Jameica-Version: 2.10.0+
- Hibiscus-Version: 2.10.0+
- Java: 8+ (source/target)
- Aktuelle Version: v2.4.4

## Verzeichnisstruktur

```
DataTransfer/
├── plugin.xml                    # Jameica Plugin-Manifest
├── PROMPT.md                     # Dieses File
├── src/
│   ├── de/willuhn/jameica/hbci/datatransfer/
│   │   ├── DataTransferPlugin.java
│   │   ├── DataTransferIO.java
│   │   ├── DataTransferBaseImporter.java
│   │   ├── DataTransferFileImporter.java
│   │   ├── OcrSettings.java
│   │   ├── action/
│   │   │   ├── FileAction.java
│   │   │   ├── ClipboardAction.java
│   │   │   ├── WebcamAction.java
│   │   │   └── SettingsAction.java
│   │   ├── gui/
│   │   │   ├── InvoiceView.java
│   │   │   ├── QRCodeView.java
│   │   │   ├── InvoiceDebugView.java
│   │   │   └── SettingsView.java
│   │   ├── model/
│   │   │   ├── TransferData.java
│   │   │   └── TransferDataHolder.java
│   │   └── parser/
│   │       ├── SmartDetector.java
│   │       ├── OcrEngine.java
│   │       ├── InvoiceTextParser.java
│   │       ├── EpcParser.java
│   │       ├── EmvParser.java
│   │       └── QrCodeSelector.java
├── lang/
│   ├── hbci_datatransfer_messages_de_DE.properties
│   └── hbci_datatransfer_messages_en.properties
├── img/                          # Icons + Screenshots
├── lib/                          # Abhängigkeiten (JARs)
└── build.xml                     # Ant build file
```

## Wichtige Jameica-Regeln

### 0. Plugin-ZIP Struktur (KRITISCH!)

Die ZIP-Datei muss einer strengen Struktur folgen:

```
hbci.datatransfer/              ← Genau EIN Ordner auf oberster Ebene
├── plugin.xml                 ← Muss im Hauptordner liegen (NICHT in der JAR!)
├── datatransfer.jar           ← Thin-JAR (nur kompilierte Klassen, ~68KB!)
├── img/                       ← Muss im Hauptordner liegen (NICHT in der JAR!)
│   ├── icon.png
│   └── screenshots/
├── lang/                      ← Muss im Hauptordner liegen (NICHT in der JAR!)
│   ├── hbci_datatransfer_messages_de_DE.properties
│   └── hbci_datatransfer_messages_en.properties
└── lib/                       ← Abhängigkeiten als eigene JARs
    ├── tess4j-5.19.0.jar
    ├── pdfbox-3.0.7.jar
    └── ...
```

**KRITISCH: plugin.xml, lang/, img/ MÜSSEN auf der obersten Ebene des Plugin-Ordners liegen!**
- Jameica sucht `plugin.xml` auf dem Dateisystem, nicht in der JAR
- Jameica's I18N-System lädt `lang/` vom Dateisystem
- Jameica's Icon-System lädt `img/` vom Dateisystem
- Wenn diese Dateien NUR in der JAR sind → Jameica findet sie NICHT → Plugin funktioniert nicht!

**Die JAR darf KEINE verschachtelte JAR (nested JAR) enthalten!**
- Die `lib/`-JARs werden von Jameica's Plugin-ClassLoader automatisch geladen
- Eine innere `datatransfer.jar` innerhalb der Haupt-JAR führt zu Classloader-Konflikten

**Fehler die vermieden werden müssen:**
1. ❌ plugin.xml/lang/img NUR in der JAR → Jameica findet sie nicht!
2. ❌ Verschachtelte JARs innerhalb der Haupt-JAR → Classloader-Konflikte
3. ❌ Windows-Backslashes `\` im ZIP → Jameica prüft auf `/`
4. ❌ PowerShell `Compress-Archive` → Erstellt keine Verzeichnis-Einträge
5. ❌ `jar uf` auf Windows → Korrupt die JAR

### 0.1 macOS Webcam-Einrichtung (WICHTIG!)

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

### 0.1 Build-Prozess (KORREKT!)

```powershell
# 1. Kompilieren
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.0.1"
& "C:\Users\istra\apache-ant-1.10.17\bin\ant.bat" compile

# 2. Thin-JAR erstellen (NUR kompilierte Klassen!)
Remove-Item -Recurse -Force build_correct -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path build_correct | Out-Null
Copy-Item -Recurse -Force "build\classes\*" build_correct\
& "C:\Program Files\Java\jdk-17.0.0.1\bin\jar.exe" cf build_correct\datatransfer.jar -C build_correct .

# 3. Plugin-Ordner mit korrekter Struktur erstellen
# plugin.xml, lang/, img/ auf oberster Ebene!
Remove-Item -Recurse -Force win_dir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "win_dir\hbci.datatransfer" | Out-Null
Copy-Item "build_correct\datatransfer.jar" "win_dir\hbci.datatransfer\"
Copy-Item "plugin.xml" "win_dir\hbci.datatransfer\"                    # ← TOP LEVEL!
Copy-Item -Recurse "lang" "win_dir\hbci.datatransfer\lang"             # ← TOP LEVEL!
Copy-Item -Recurse "img" "win_dir\hbci.datatransfer\img"               # ← TOP LEVEL!
Copy-Item -Recurse "lib" "win_dir\hbci.datatransfer\lib"

# 4. PRÜFEN bevor ZIP erstellt wird!
Write-Host "=== VERIFIKATION ==="
Test-Path "win_dir\hbci.datatransfer\plugin.xml"        # MUSS True sein!
Test-Path "win_dir\hbci.datatransfer\lang"              # MUSS True sein!
Test-Path "win_dir\hbci.datatransfer\img"               # MUSS True sein!
Test-Path "win_dir\hbci.datatransfer\datatransfer.jar"  # MUSS True sein!
Test-Path "win_dir\hbci.datatransfer\lib"               # MUSS True sein!

# 5. ZIP erstellen (7-Zip, NICHT PowerShell!)
& "C:\Program Files\7-Zip\7z.exe" a -tzip "release\hbci.datatransfer-VERSION-windows.zip" "win_dir\hbci.datatransfer"
```

### 0.2 Verifikations-Checklist (VOR jedem Release!)

**VOR dem Upload auf GitHub PRÜFEN:**
```powershell
# Struktur prüfen
Get-ChildItem "win_dir\hbci.datatransfer" -Name
# Erwartet: img, lang, lib, datatransfer.jar, plugin.xml

# ZIP-Inhalt prüfen
& "C:\Program Files\7-Zip\7z.exe" l "release\hbci.datatransfer-VERSION-windows.zip" | Select-String "plugin.xml|lang/|img/"
# Erwartet: Einträge für plugin.xml, lang/, img/ auf oberster Ebene!
```

**FALLS plugin.xml/lang/img FEHLT:**
- Die Dateien wurden NUR in die JAR gepackt
- LOSE die Dateien aus der JAR und kopiere sie auf die oberste Ebene!

### 0.3 Classfinder in plugin.xml

Die `<classfinder>` Regex MUSS den tatsächlichen JAR-Namen matchen:

```xml
<classfinder>
  <!-- RICHTIG: Matcht den tatsächlichen JAR-Namen -->
  <include>hbci\.datatransfer\.jar</include>
  <include>.*\.class</include>
</classfinder>
```

```xml
<classfinder>
  <!-- FALSCH: Matcht eine innere JAR die es nicht mehr gibt -->
  <include>datatransfer\.jar</include>
</classfinder>
```

**Regel:** Der Name in `<include>` muss EXAKT zum Dateinamen der plugin-JAR passen.

### 1. Icon-Pfade in plugin.xml
```xml
<!-- FALSCH: img/ Prefix wird nicht aufgelöst -->
icon-close="img/datatransfer-icon.png"

<!-- RICHTIG: Nur Dateiname, Jameica sucht in Unterverzeichnissen -->
icon-close="datatransfer-icon.png"
```

### 2. Sprachdateien
- Benennung: `hbci_datatransfer_messages_de_DE.properties`
- Immer mit Plugin-Name als Prefix
- Umlaute als Escape-Sequenzen: `\u00FC` für ü, `\u00F6` für ö, `\u00E4` für ä

### 3. Plugin.xml Struktur
```xml
<plugin xmlns="http://www.willuhn.de/schema/jameica-plugin"
        name="hbci.datatransfer" version="X.Y.Z"
        class="de.willuhn.jameica.hbci.datatransfer.DataTransferPlugin">
  
  <requires jameica="2.10.0+">
    <import plugin="hibiscus" version="2.10.0+"/>
  </requires>
  
  <classfinder>
    <include>hbci\.datatransfer\.jar</include>
    <include>.*\.class</include>
  </classfinder>
  
  <extension point="jameica.extension">
    <class>de.willuhn.jameica.hbci.datatransfer.DataTransferIO</class>
  </extension>
</plugin>
```

**Wichtig:** `<requires><import>` verwenden, NICHT `<depends>`!

### 4. Kompilierung
- JDK 17 verwenden (`C:\Program Files\Java\jdk-17.0.0.1`)
- Ant: `C:\Users\istra\apache-ant-1.10.17\bin\ant.bat`
- JAVA_HOME muss gesetzt werden: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.0.1"`
- Classpath: `jameica.jar`, `hibiscus.jar`, `lib\*` (aus dem Produktions-Verzeichnis)

### 5. Jameica Portable
- Produktion: `G:\jameica_portable_V1\`
- Test: `G:\jameica_portable_test\`
- Config: `C:\Users\istra\.jameica.properties` (shared!)
- Log: `<jameica>\Data\jameica\jameica.log`
- **ACHTUNG:** Test-Version teilt sich Config mit Produktion!

## Webcam-Integration (JavaCV via Reflection)

### Architektur
Die Webcam nutzt JavaCV/OpenCV **über Reflection**, um keine direkte Compile-Abhängigkeit zu haben:

```java
// Globaler ClassLoader ist ESSENTIELL für Webcam!
private static ClassLoader getGlobalClassLoader() {
    return Application.getClassLoader();
}

private static Class<?> forName(String name) throws ClassNotFoundException {
    return Class.forName(name, true, getGlobalClassLoader());
}

// Verwendung:
Class<?> captureClass = forName("org.bytedeco.opencv.opencv_videoio.VideoCapture");
Object capture = captureClass.getConstructor().newInstance();
```

### KRITISCH: Globaler ClassLoader
- Jameica's Plugin-ClassLoader kann Webcam-JARs NICHT finden
- **Muss** `Application.getClassLoader()` verwenden
- Ohne das: `ClassNotFoundException: org.bytedeco.javacpp.indexer.Indexable`

### Webcam-Open mit Timeout
```java
// VideoCapture.open() kann blockieren!
FutureTask<Boolean> openTask = new FutureTask<>(() -> {
    cap.getClass().getMethod("open", int.class).invoke(cap, devIdx);
    return (Boolean) cap.getClass().getMethod("isOpened").invoke(cap);
});
Thread openThread = new Thread(openTask);
openThread.setDaemon(true);
openThread.start();

Boolean isOpened = openTask.get(20, TimeUnit.SECONDS);  // 20s Timeout!
```

### Error-Logging in WebcamAction
```java
// ALLE Fehlerpfade müssen loggen:
System.err.println("WEBCAM ERROR: " + message);  // Für Konsole
Logger.error("webcam: " + message, exception);    // Für Jameica-Log

// Ohne System.err.println erscheinen Fehler NICHT im Log!
```

### Webcam-JARs (in lib/)
Die Webcam benötigt spezifische JavaCV-JARs:
- javacpp-1.5.9.jar + javacpp-1.5.9-windows-x86_64.jar
- javacv-1.5.9.jar
- opencv-4.7.0-1.5.9.jar + opencv-4.7.0-1.5.9-windows-x86_64.jar
- openblas-0.3.23-1.5.9.jar + openblas-0.3.23-1.5.9-windows-x86_64.jar
- fftw-3.3.10-1.5.9.jar + fftw-3.3.10-1.5.9-windows-x86_64.jar

## Hibiscus Import-Dialog Integration

### Wie es funktioniert
1. Plugin registriert Importer via `<extension point="jameica.extension">`
2. ClassFinder in IORegistry findet Importer automatisch
3. Hibiscus Import-Dialog zeigt "Rechnungs-Datei(PDF/Image) - OCR/QR" an

### Benötigt: Gepatchter Hibiscus
- Commit `cbbce4ad` ändert `IORegistry.java`
- Nutzt `Application.getClassLoader().getClassFinder()` statt plugin-spezifischen ClassLoader
- Ohne Patch: Importer erscheint NICHT im Import-Dialog
- Menü-Funktionen funktionieren immer (auch ohne Patch)

### Importer-Architektur
```
DataTransferIO (implements IO)
  └── getIOFormats() → liefert IOFormat-Objekte
       └── DataTransferFileImporter (extends AbstractImporter)
            └── processInput(InputStream) → returns TransferData
                 └── doImport() → öffnet Review-Dialog (QRCodeView/InvoiceView)
```

### Achtung: Keine Clipboard/Webcam im Import-Dialog
- Import-Dialog nutzt IMMER File-Dialog (FileDialog)
- Clipboard und Webcam funktionieren NUR über Plugin-Menü
- Das ist ein Jameica-Design, nicht ein Bug

## Fehler-Vermeidungs-Checkliste

### Vor jedem Deploy prüfen:
1. ✅ Keine verschachtelte JAR in der Haupt-JAR
2. ✅ `<classfinder>` matcht den tatsächlichen JAR-Namen
3. ✅ `plugin.xml` ist IN der JAR UND im Plugin-Verzeichnis
4. ✅ `lang/` und `img/` sind im Plugin-Verzeichnis (nicht nur in der JAR)
5. ✅ `lib/` enthält alle Dependencies
6. ✅ WebcamAction nutzt `Application.getClassLoader()` für alle `Class.forName()`
7. ✅ Alle Fehlerpfade haben `System.err.println` + `Logger.error`

### Wenn Importer nicht erscheint:
1. Ist `plugin.xml` mit `<extension point="jameica.extension">` vorhanden?
2. Stimmt der `<classfinder>` Regex?
3. Ist der gepatchte Hibiscus installiert?
4. Ist die JAR-Korrupt? (Frisch bauen!)

### Wenn Webcam nicht funktioniert:
1. Sind ALLE 9 Webcam-JARs in `lib/`?
2. Nutzt WebcamAction `Application.getClassLoader()`?
3. Ist der Timeout auf 20s gesetzt?
4. Steht in `System.err.println` im catch-Block?

## Build-Prozess

### Sauberer Build (EMPFOHLEN)
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.0.1"
Set-Location "C:\Users\istra\Documents\claude_ps\DataTransfer"

# 1. Kompilieren
& "C:\Users\istra\apache-ant-1.10.17\bin\ant.bat" compile

# 2. Saubere JAR erstellen
$classesDir = "build\classes"
$srcDir = "C:\Users\istra\Documents\claude_ps\DataTransfer"
$releaseJar = "$env:TEMP\hbci.datatransfer.jar"
$tmpDir = "$env:TEMP\release_jar"

Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $tmpDir -Force | Out-Null

Copy-Item "$srcDir\$classesDir\*" $tmpDir -Recurse -Force
Copy-Item "$srcDir\lang" "$tmpDir\lang" -Recurse -Force
Copy-Item "$srcDir\img" "$tmpDir\img" -Recurse -Force
Copy-Item "$srcDir\plugin.xml" "$tmpDir\plugin.xml" -Force

Push-Location $tmpDir
& "C:\Program Files\Java\jdk-17.0.0.1\bin\jar.exe" cf $releaseJar *
Pop-Location

# 3. Plugin-ZIP erstellen
$zipDir = "$env:TEMP\release_zip\hbci.datatransfer"
Remove-Item -Recurse -Force "$env:TEMP\release_zip" -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $zipDir -Force | Out-Null

Copy-Item $releaseJar "$zipDir\hbci.datatransfer.jar"
Copy-Item "$srcDir\plugin.xml" "$zipDir\plugin.xml"
Copy-Item "$srcDir\lib" "$zipDir\lib" -Recurse
Copy-Item "$srcDir\lang" "$zipDir\lang" -Recurse
Copy-Item "$srcDir\img" "$zipDir\img" -Recurse

# ZIP mit 7-Zip (NICHT Compress-Archive!)
& "C:\Program Files\7-Zip\7z.exe" a -tzip "$srcDir\release\hbci.datatransfer-2.4.0.zip" "$zipDir\*"
```

### Quick-Deploy (nur plugin.xml geändert)
```powershell
# Wenn NUR plugin.xml geändert wurde:
$pluginDir = "G:\jameica_portable_test\jameica\plugins\hbci.datatransfer"
Copy-Item "C:\Users\istra\Documents\claude_ps\DataTransfer\plugin.xml" "$pluginDir\plugin.xml"
# Jameica neu starten
```

### ⚠️ NICHT verwenden:
- PowerShell `Compress-Archive` → Keine Verzeichnis-Einträge, nutzt `\`
- `jar uf` auf Windows → Korrupt die JAR
- Fat-JAR mit entpackten lib/-JARs → 90MB statt 400KB + lib/

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
HBCIDBService dbService = Settings.getDBService();
Ueberweisung u = dbService.createObject(Ueberweisung.class, null);
u.setGegenkontoNummer(iban);
u.setGegenkontoBLZ(bic);
u.setGegenkontoName(name);
u.setBetrag(betrag);
u.setZweck(zweck);
u.store();
```

## Test-Verzeichnis

- Pfad: `G:\jameica_portable_test\`
- Plugins: `G:\jameica_portable_test\jameica\plugins\`
- Log: `G:\jameica_portable_test\Data\jameica\jameica.log`
- Jameica EXE: `G:\jameica_portable_test\jameica\jameica-win64.exe`

## Versionshistorie

### v2.4.0
- Sauberes Build: Keine verschachtelte `datatransfer.jar` in der Haupt-JAR mehr
- Classfinder-Regex in plugin.xml korrigiert (matcht `hbci.datatransfer.jar`)
- Webcam: Timeout auf 20s erhöht, globaler ClassLoader, besseres Error-Logging

### v2.3.0
- Hibiscus Import-Dialog Integration
- Review-Dialog vor Überweisungsanlegung
- SmartDetector: detectFromStream() mit beliebigen Dateitypen
- TransferDataHolder für View-Übergabe

### v2.1.0
- Hilfe-Button, Doppelklick-Editor, case-insensitive Suche
- Multi-QR-Erkennung in PDFs

### v2.0.0
- Fusion von QRtransfer und OCRtransfer
