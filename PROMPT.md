# DataTransfer Plugin - Projekt-Prompt

## Projektübersicht

Kombiniertes Jameica/Hibiscus-Plugin zum Lesen von SEPA-Zahlungsdaten aus QR-Codes und OCR (Rechnungen). Automatische Erkennung des Quellentyps.

- Plugin-Name: `hbci.datatransfer`
- Package: `de.willuhn.jameica.hbci.datatransfer`
- Repository: https://github.com/istra711/DataTransfer (öffentlich)
- Jameica-Version: 2.10.0+
- Hibiscus-Version: 2.10.0+
- Java: 8+ (source/target)
- Aktuelle Version: v2.4.6

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

**KRITISCHE WARNUNG: ZIP-Build mit manuellen Scripts (PLOPP-WIEDERHOLUNG!)**

Wenn ZIPs manuell mit PowerShell/7-Zip gebaut werden (statt über build.xml), wird
IMMER ein temporäres Build-Verzeichnis wie `win_dir/`, `linux_dir/`, `macos_dir/` etc.
erstellt. Beim ZIP-Export muss `Push-Location` in dieses Verzeichnis gewechselt werden,
damit die ZIP **NICHT** den Build-Ordner-Namen als Präfix enthält!

**FALSCH** (tritt immer wieder auf!):
```
7z a release\plugin.zip win_dir\hbci.datatransfer    ← ERGEBNIS: win_dir\hbci.datatansfer\...
```

**RICHTIG**:
```
Push-Location win_dir
7z a ..\release\plugin.zip hbci.datatransfer           ← ERGEBNIS: hbci.datatransfer\...
Pop-Location
```

**VOR DEM UPLOAD AUF GITHUB MUSS DIE STRUKTUR VERIFIZIERT WERDEN:**
```powershell
7z l release\plugin-VERSION-windows.zip | Select-String "D\.\.\.\."
# Erwartet: hbci.datatransfer  (OHNE win_dir/, linux_dir/ etc.)
```

**FEHLERHAFTE ZIPs FÜREN DAZU, DASS DAS PLUGIN NICHT GELADEN WIRD!**
Jameica sucht `hbci.datatransfer/plugin.xml` direkt in der ZIP-obersten Ebene.
Wenn die Struktur `win_dir/hbci.datatransfer/plugin.xml` ist, findet Jameica
das Plugin NICHT!

**Fehler die vermieden werden müssen:**
1. ❌ plugin.xml/lang/img NUR in der JAR → Jameica findet sie nicht!
2. ❌ Verschachtelte JARs innerhalb der Haupt-JAR → Classloader-Konflikte
3. ❌ Windows-Backslashes `\` im ZIP → Jameica prüft auf `/`
4. ❌ PowerShell `Compress-Archive` → Erstellt keine Verzeichnis-Einträge
5. ❌ `jar uf` auf Windows → Korrupt die JAR
6. ❌ Build-Ordner-Name (win_dir/linux_dir etc.) als ZIP-Präfix → Jameica findet plugin.xml nicht!

### 0.2 OCR Tessdata (KRITISCH!)

Das Plugin benötigt Tesseract-Trainingsdateien für OCR. Diese müssen im Plugin-Verzeichnis liegen:

```
hbci.datatransfer/
├── tessdata/
│   └── deu.traineddata    ← Deutsche Trainingsdaten (~8.6MB)
├── lib/
│   └── tess4j-5.19.0.jar
└── ...
```

**Download der Trainingsdaten:**
```bash
mkdir tessdata
curl -L -o tessdata/deu.traineddata https://github.com/tesseract-ocr/tessdata_best/raw/main/deu.traineddata
```

**WICHTIG:** Der `tessdata`-Ordner MUSS in der Plugin-ZIP enthalten sein! Ohne Trainingsdateien liefert Tesseract immer `null` zurück.

### 0.2.1 macOS ARM (aarch64) - Tesseract Native Library (KRITISCH!)

tess4j 5.19.0 bundelt KEINE nativen Tesseract-Libraries für macOS ARM (aarch64).
Der User MUSS Tesseract via Homebrew installieren:

```bash
brew install tesseract
```

Dies installiert `libtesseract.dylib` nach `/opt/homebrew/lib/`.

Das Plugin setzt automatisch `jna.library.path` auf `/opt/homebrew/lib`,
damit JNA die native Library finden kann.

**Ohne Homebrew-Installation:** OCR funktioniert NICHT auf macOS ARM!
Fehler: `UnsatisfiedLinkError: Unable to load library 'tesseract'`

**Windows/Linux:** tess4j bundelt die nativen Libraries (JavaCV/OpenCV).
Nur macOS ARM benötigt die externe Installation.

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

### 0.1 Plattform-Referenz (STAND: v2.4.5)

> **MERKE:** Jede Plattform hat ihr eigenes lib-Verzeichnis. Die platform-abhängigen JARs sind bereits in jedem Verzeichnis enthalten. NIEMALS `lib/*.jar` für Linux/macOS verwenden!

| Plattform | ZIP-Name | lib-Verzeichnis | Plattform-JARs (Pattern) | ZIP-Größe | JARs in ZIP |
|-----------|----------|-----------------|--------------------------|-----------|-------------|
| **Windows** | `*-windows.zip` | `lib/` | `*-windows-x86_64.jar` | ~90-98 MB | 22 |
| **Linux** | `*-linux.zip` | `lib-linux/` | `*-linux-x86_64.jar` | ~80 MB | 22 |
| **macOS Intel** | `*-macos.zip` | `lib-macosx/` | `*-macosx-x86_64.jar` | ~78 MB | 22 |
| **macOS ARM** | `*-macos-arm64.zip` | `lib-macosx-arm64/` | `*-macosx-arm64.jar` | ~66 MB | 21 |

#### Plattform-spezifische JARs pro Verzeichnis

| JAR-Name | Windows | Linux | macOS Intel | macOS ARM |
|----------|---------|-------|-------------|-----------|
| `javacpp-1.5.9-*.jar` | ✅ windows-x86_64 | ✅ linux-x86_64 | ✅ macosx-x86_64 | ✅ macosx-arm64 |
| `opencv-4.7.0-1.5.9-*.jar` | ✅ windows-x86_64 (31 MB) | ✅ linux-x86_64 (28 MB) | ✅ macosx-x86_64 (25 MB) | ✅ macosx-arm64 (21 MB) |
| `openblas-0.3.23-1.5.9-*.jar` | ✅ windows-x86_64 (29 MB) | ✅ linux-x86_64 (14 MB) | ✅ macosx-x86_64 (15 MB) | ✅ macosx-arm64 (7 MB) |
| `fftw-3.3.10-1.5.9-*.jar` | ✅ windows-x86_64 (2 MB) | ✅ linux-x86_64 (2 MB) | ✅ macosx-x86_64 (1 MB) | ❌ nicht enthalten |

#### Gemeinsame JARs (in ALLEN Verzeichnissen)

```
commons-io-2.22.0.jar, commons-logging-1.3.3.jar, core-3.5.3.jar,
fontbox-3.0.3.jar, jai-imageio-core-1.4.0.jar, javacpp-1.5.9.jar,
javacv-1.5.9.jar, javase-3.5.3.jar, jna-5.18.1.jar, jna-platform-5.18.1.jar,
lept4j-1.24.0.jar, openblas-0.3.23-1.5.9.jar, opencv-4.7.0-1.5.9.jar,
pdfbox-3.0.7.jar, pdfbox-io-3.0.3.jar, slf4j-api-2.0.18.jar, tess4j-5.19.0.jar
```

#### ⚠️ Häufigste Fehler

1. **`lib/*.jar` für alle Plattformen verwendet** → Windows-JARs in Linux/macOS ZIP
2. **ARM64 JARs nicht gelöscht nach Build** → macOS Intel ZIP wird zu groß (>100 MB)
3. **Falsches lib-Verzeichnis angesprochen** → Plattform-JARs fehlen in ZIP

### 0.2 Build-Prozess (KORREKT!)

```powershell
# 1. Kompilieren
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.0.1"
& "C:\Users\istra\apache-ant-1.10.17\bin\ant.bat" compile

# 2. Thin-JAR erstellen (NUR kompilierte Klassen!)
Remove-Item -Recurse -Force build_correct -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path build_correct | Out-Null
Copy-Item -Recurse -Force "build\classes\*" build_correct\
& "C:\Program Files\Java\jdk-17.0.0.1\bin\jar.exe" cf build_correct\datatransfer.jar -C build_correct .

# 3. Plugin-Ordner pro Plattform erstellen
$platforms = @(
    @{Name="windows"; LibDir="lib"; Suffix="-windows"},
    @{Name="linux"; LibDir="lib-linux"; Suffix="-linux"},
    @{Name="macosx"; LibDir="lib-macosx"; Suffix="-macosx"},
    @{Name="macosx-arm64"; LibDir="lib-macosx-arm64"; Suffix="-macosx-arm64"}
)

foreach ($p in $platforms) {
    $tmpDir = "release\$($p.Name)_dir"
    $pluginDir = "$tmpDir\hbci.datatransfer"
    
    Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path "$pluginDir\lib" | Out-Null
    New-Item -ItemType Directory -Force -Path "$pluginDir\lang" | Out-Null
    New-Item -ItemType Directory -Force -Path "$pluginDir\tessdata" | Out-Null
    
    # Dateien kopieren
    Copy-Item "build_correct\datatransfer.jar" "$pluginDir\"
    Copy-Item "plugin.xml" "$pluginDir\"                    # ← TOP LEVEL!
    Copy-Item -Recurse "lang" "$pluginDir\lang"             # ← TOP LEVEL!
    Copy-Item -Recurse "bilder" "$pluginDir\img"            # ← TOP LEVEL!
    
    # ⚠️ KRITISCH: NUR das plattformspezifische lib-Verzeichnis!
    Copy-Item "$($p.LibDir)\*.jar" "$pluginDir\lib\"        # ← NICHT lib/*.jar!
    Copy-Item "tessdata\*.traineddata" "$pluginDir\tessdata\"
    
    # PRÜFEN
    $jarCount = (Get-ChildItem "$pluginDir\lib\*.jar").Count
    Write-Host "$($p.Name): $jarCount JARs aus $($p.LibDir)"
    
    # ZIP erstellen
    Push-Location $tmpDir
    & "C:\Program Files\7-Zip\7z.exe" a -tzip "..\hbci.datatransfer-VERSION$($p.Suffix).zip" "hbci.datatransfer"
    Pop-Location
    
    # GRÖSSE PRÜFEN
    $zipFile = Get-Item "release\hbci.datatransfer-VERSION$($p.Suffix).zip"
    $sizeMB = [math]::Round($zipFile.Length / 1MB, 1)
    Write-Host "  → $sizeMB MB"
    
    Remove-Item -Recurse -Force $tmpDir
}

# 4. VERIFIKATION (ALLE ZIPs!)
& "C:\Program Files\7-Zip\7z.exe" l "release\hbci.datatransfer-VERSION-windows.zip" | Select-String "D\.\.\.\."
# Erwartet: hbci.datatransfer  (OHNE win_dir/ Präfix!)
```

> ⚠️ **HÄUFIGER FEHLER:** `lang/` Vergessen! Die build.xml muss `lang/*.properties` in JEDE ZIP kopieren.
> Symptome: Alle Labels zeigen rohe Keys (z.B. "settings.title" statt "Einstellungen"), Hilfe-Dialog leer.
> Fix: In `dist`-Target UND `build-platform-zip`-Macro einen `<copy>` für `lang/` einfügen.

### 0.3 Verifikations-Checklist (VOR jedem Release!)

> Siehe Tabelle in **0.1** für lib-Verzeichnisse, JAR-Patterns und erwartete ZIP-Größen.

**VOR dem Upload auf GitHub PRÜFEN:**
```powershell
# 0. PLATTFORM-CHECK: Falsche JARs in ZIP suchen (HÄUFIGSTER FEHLER!)
function Verify-Zip {
    param([string]$zipPath, [string]$platform)
    
    $content = & "C:\Program Files\7-Zip\7z.exe" l $zipPath
    $wrongPlatform = switch ($platform) {
        "windows" { "linux|macosx" }
        "linux" { "windows|macosx" }
        "macosx" { "windows|linux" }
        "macosx-arm64" { "windows|linux" }
    }
    $wrongJars = $content | Select-String "\.jar" | Where-Object { $_ -match $wrongPlatform }
    
    if ($wrongJars) {
        Write-Host "FEHLER: Falsche plattformspezifische JARs in $zipPath!" -ForegroundColor Red
        $wrongJars | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
        return $false
    }
    
    # ⚠️ KRITISCH: lang/ MUSS in der ZIP sein (sonst keine Labels/Hilfe!)
    if (-not ($content | Select-String "lang\\hbci_datatransfer")) {
        Write-Host "FEHLER: lang/ Verzeichnis FEHLT in $zipPath!" -ForegroundColor Red
        Write-Host "  → Labels werden als rohe Keys angezeigt, Hilfe funktioniert nicht!" -ForegroundColor Red
        return $false
    }
    
    return $true
}

# Prüfe ALLE ZIPs
$zipFiles = Get-ChildItem "release\hbci.datatransfer-VERSION-*.zip"
$allOk = $true
foreach ($zip in $zipFiles) {
    $platform = if ($zip.Name -match "windows") { "windows" }
                elseif ($zip.Name -match "linux") { "linux" }
                elseif ($zip.Name -match "macosx-arm64") { "macosx-arm64" }
                elseif ($zip.Name -match "macosx") { "macosx" }
    if (-not (Verify-Zip $zip.FullName $platform)) { $allOk = $false }
}
if (-not $allOk) { Write-Host "ABBRUCH: Korrigiere ZIPs!" -ForegroundColor Red; exit 1 }

# 1. Struktur des Build-Verzeichnisses prüfen
Get-ChildItem "win_dir\hbci.datatransfer" -Name
# Erwartet: img, lang, lib, tessdata, datatransfer.jar, plugin.xml

# 2. ZIP-Inhalt prüfen - OBERSTE EBENE!
& "C:\Program Files\7-Zip\7z.exe" l "release\hbci.datatransfer-VERSION-windows.zip" | Select-String "D\.\.\.\."
# Erwartet: hbci.datatransfer  (ERSTE Zeile! OHNE win_dir/ etc.)

# 3. plugin.xml muss in der ZIP-obersten Ebene liegen
& "C:\Program Files\7-Zip\7z.exe" l "release\hbci.datatransfer-VERSION-windows.zip" | Select-String "plugin.xml"
# Erwartet: hbci.datatransfer\plugin.xml

# 4. tessdata muss enthalten sein
& "C:\Program Files\7-Zip\7z.exe" l "release\hbci.datatransfer-VERSION-windows.zip" | Select-String "tessdata"
# Erwartet: hbci.datatansfer\tessdata\deu.traineddata

# 5. ZIP-Größen prüfen (Vergleich mit Vorgängerversion!)
$zipFiles = Get-ChildItem "release\hbci.datatransfer-VERSION-*.zip"
foreach ($zip in $zipFiles) {
    $sizeMB = [math]::Round($zip.Length / 1MB, 1)
    $status = if ($sizeMB -ge 56 -and $sizeMB -le 93) { "OK" }
              elseif ($sizeMB -lt 56) { "ZU KLEIN - Dependencies fehlen!" }
              else { "ZU GROSS - Falsche JARs eingefügt?" }
    Write-Host "$($zip.Name): $sizeMB MB - $status"
}
# Erwartet: Windows ~90MB, Linux ~73MB, macOS ~71MB, macOS ARM ~58MB
# Bei >100MB: Wahrscheinlich plattfremde JARs enthalten!
```

**FALLS Die oberste Ebene `win_dir/` oder `linux_dir/` etc. ist:**
- ❌ Die ZIP wurde FALSCH erstellt! (Build-Ordner als Präfix)
- → ZIP löschen, mit `Push-Location` in Build-Ordner neu erstellen!

**FALLS plugin.xml/lang/img FEHLT:**
- Die Dateien wurden NUR in die JAR gepackt
- LOSE die Dateien aus der JAR und kopiere sie auf die oberste Ebene!

**FALLS ZIP > 100MB:**
- ❌ Falsche plattformspezifische JARs enthalten (z.B. Windows-JARs in Linux-ZIP)
- → Prüfe ob `lib/*.jar` (Windows) statt `lib-linux/*.jar` verwendet wurde!

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

### v2.4.5
- OCR-Fix: `tessdata/deu.traineddata` wird jetzt im Plugin ausgeliefert
- OCR: Tessdata-Pfad wird relativ zum Plugin-Verzeichnis aufgelöst (statt relativ zum CWD)
- Build: `tessdata/` Ordner wird in Plugin-ZIP einbezogen

### v2.4.4
- macOS Webcam: Diagnose-Logging für ARM-Mac hinzugefügt
- macOS Webcam: `NSCameraUsageDescription` in Info.plist erforderlich (TCC-Problem behoben)
- Fix-Script `fix-webcam-permission.sh` für macOS hinzugefügt

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

## Schnell-Verifikation (nach jedem Build)

```powershell
# Diese Funktion NACH dem Build aufrufen!
function Verify-AllZips {
    param([string]$version)
    
    $base = "C:\Users\istra\Documents\claude_ps\DataTransfer"
    $7z = "C:\Program Files\7-Zip\7z.exe"
    
    $checks = @(
        @{Pattern="windows"; WrongPattern="linux|macosx"; ExpectedMin=88; ExpectedMax=93},
        @{Pattern="linux"; WrongPattern="windows|macosx"; ExpectedMin=71; ExpectedMax=75},
        @{Pattern="macosx-arm64"; WrongPattern="windows|linux"; ExpectedMin=56; ExpectedMax=60},
        @{Pattern="macosx"; WrongPattern="windows|linux"; ExpectedMin=69; ExpectedMax=73}
    )
    
    $allOk = $true
    foreach ($check in $checks) {
        $zipFile = Get-ChildItem "$base\release\*-$version-$($check.Pattern).zip" | Select-Object -First 1
        if (-not $zipFile) { Write-Host "FEHLER: $($check.Pattern) ZIP nicht gefunden!" -ForegroundColor Red; continue }
        
        $content = & $7z l $zipFile.FullName
        
        # Check for wrong platform JARs
        $wrongJars = $content | Select-String "\.jar" | Where-Object { $_ -match $check.WrongPattern }
        if ($wrongJars) {
            Write-Host "FEHLER: Falsche JARs in $($check.Pattern)!" -ForegroundColor Red
            $wrongJars | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
            $allOk = $false
        }
        
        # Check size
        $sizeMB = [math]::Round($zipFile.Length / 1MB, 1)
        if ($sizeMB -lt $check.ExpectedMin -or $sizeMB -gt $check.ExpectedMax) {
            Write-Host "WARNUNG: $($check.Pattern) ist $sizeMB MB (erwartet $($check.ExpectedMin)-$($check.ExpectedMax) MB)" -ForegroundColor Yellow
        } else {
            Write-Host "OK: $($check.Pattern) = $sizeMB MB" -ForegroundColor Green
        }
    }
    
    if ($allOk) { Write-Host "`nALLE CHECKS BESTANDEN!" -ForegroundColor Green }
    else { Write-Host "`nFEHLER GEFUNDEN - ZIPs prüfen!" -ForegroundColor Red }
}

# Verwendung:
Verify-AllZips "2.4.5"
```
