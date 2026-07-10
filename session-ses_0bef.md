# Jameica-Plugin-Zusammenführung

**Session ID:** ses_0befdc331ffelWNxF1Z7kC93Ib
**Created:** 8.7.2026, 11:14:53
**Updated:** 10.7.2026, 15:00:00

---

## Wichtigste Erkenntnisse

### Jameica Plugin-ZIP Struktur (KRITISCH!)

Die ZIP-Datei für Jameica-Plugins muss einer **strengen Struktur** folgen:

```
pluginname/                    ← Genau EIN Ordner auf oberster Ebene
├── plugin.xml                 ← Muss im Hauptordner liegen
├── datatransfer.jar           ← Fat-JAR mit allen Klassen
├── img/                       ← Verzeichnisse mit EXPLIZITEN Einträgen
│   ├── icon1.png
│   └── icon2.png
├── lang/                      ← Sprachdateien
│   ├── messages_de_DE.properties
│   └── messages_en.properties
└── lib/                       ← Abhängigkeiten
    ├── dependency1.jar
    └── dependency2.jar
```

**Fehler die vermieden werden müssen:**
1. ❌ Dateien direkt auf oberster Ebene (ohne Ordner) → `contains invalid file`
2. ❌ Windows-Backslashes `\` im ZIP → Jameica prüft auf `/`
3. ❌ Fehlende explizite Verzeichnis-Einträge → `plugin zip-file empty`
4. ❌ Thin-JAR (nur Klassen) → Muss Fat-JAR sein mit allen gebündelten Klassen

**Richtige Erstellung mit Python:**
```python
import zipfile
import os

src_dir = r'path/to/plugin_folder'
zip_path = r'output.zip'

with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
    # 1. Explizite Verzeichnis-Einträge hinzufügen
    dirs_added = set()
    for root, dirs, files in os.walk(src_dir):
        for d in dirs:
            dir_path = os.path.join(root, d)
            arcname = os.path.relpath(dir_path, src_dir).replace(os.sep, '/') + '/'
            if arcname not in dirs_added:
                zipf.writestr(zipfile.ZipInfo(arcname), '')
                dirs_added.add(arcname)
    
    # 2. Dateien hinzufügen mit Forward-Slashes
    for root, dirs, files in os.walk(src_dir):
        for file in files:
            file_path = os.path.join(root, file)
            arcname = os.path.relpath(file_path, src_dir).replace(os.sep, '/')
            zipf.write(file_path, arcname)
```

### Jameica ZippedPlugin.java Validierung

Aus der Jameica-Quellcode (`de.willuhn.jameica.plugin.ZippedPlugin`):

```java
// Jeder Name muss einen Slash enthalten
if (!name.contains("/"))
{
    Logger.error(zip + " contains invalid file " + name);
    throw ae;
}

// Dateien in Unterordnern sind erlaubt
if (name.matches(".{1,}?/.{1,}"))
    continue;

// Muss ein Verzeichnis sein (der Hauptordner)
if (!entry.isDirectory())
{
    Logger.error("plugin zip-file must contain only one folder");
    throw ae;
}
```

### Fat-JAR vs Thin-JAR

- **Fat-JAR** (~8MB): Enthält alle Klassen des Plugins + Abhängigkeiten gebündelt
- **Thin-JAR** (~75KB): Enthält nur die Plugin-Klassen, Abhängigkeiten separat in `lib/`
- Jameica erwartet eine **Fat-JAR** als Haupt-JAR

### Build-Prozess

Das Projekt verwendet Ant (`build.xml`):
```bash
ant -f build.xml clean dist-all
```

Der Build erstellt:
- `dist/datatransfer.jar` - Fat-JAR
- `dist/hbci.datatransfer/` - Plugin-Verzeichnisstruktur
- `dist/hbci.datatransfer-VERSION.zip` - Installations-ZIP

### GitHub Release

Releases werden mit `gh` CLI erstellt:
```bash
gh release create v2.3.0 hbci.datatransfer-2.3.0.zip hibiscus-patched.zip \
  --title "v2.3.0" --notes "Release notes"
```

---

## Projektstruktur

```
C:\Users\istra\Documents\claude_ps\DataTransfer\
├── src/                        # Java-Quellcode
│   └── de/willuhn/jameica/hbci/datatransfer/
├── bilder/                     # Icons (PNG)
├── img/screenshots/            # Screenshots für README
├── lib/                        # Abhängigkeiten (JARs)
├── lang/                       # Sprachdateien (nicht vorhanden, in src/lang/)
├── dist/                       # Build-Ausgabe
├── build.xml                   # Ant-Build-Datei
├── plugin.xml                  # Jameica Plugin-Manifest
├── README.md                   # Englische Dokumentation
├── README_de.md                # Deutsche Dokumentation
└── PROMPT.md                   # Project Knowledge für AI-Sessions
```

## Pfade

- **Produktion:** `G:\jameica_portable_V1\jameica\plugins\hbci.datatransfer\`
- **Test:** `G:\jameica_portable_test\jameica\plugins\`
- **GitHub:** https://github.com/istra711/DataTransfer
- **Ant:** `C:\Users\istra\apache-ant-1.10.17\bin\ant.bat`
- **JDK:** `C:\Program Files\Java\jdk-17.0.0.1`

## Hibiscus ClassFinder Patch

Der Patch wurde vom Hibiscus-Entwickler vorgeschlagen:
- Commit: https://github.com/willuhn/hibiscus/commit/cbbce4ad6abafc652011e5c777338cc74b786d38
- Ändert `IORegistry.java` für globalen ClassFinder
- Erforderlich für Import-Dialog-Integration
- Optional - Plugin funktioniert auch ohne (nur Menü)

---

## Session-Verlauf (gekürzt)

### Durchgeführte Änderungen

1. **README aktualisiert:**
   - Hinweis auf ClassFinder-Patch hinzugefügt
   - Link zum spezifischen Hibiscus-Commit
   - Klarstellung dass Patch optional ist

2. **Screenshots hinzugefügt:**
   - 7 Screenshots aus `BilderGUI_DataTransfer`
   - In `img/screenshots/` kopiert
   - In beide READMEs eingefügt

3. **Gepatchtes Hibiscus bereitgestellt:**
   - Aus `hibiscus` Repo gebaut
   - `hibiscus-patched.zip` zum Release hinzugefügt

4. **Plugin-ZIP korrigiert:**
   - Von funktionierender Installation kopiert
   - Nur `plugin.xml` durch neue Version ersetzt
   - Explizite Verzeichnis-Einträge beibehalten

### Beseitigte Fehler

1. `contains invalid file de\willuhn\` → .class Dateien entfernt
2. `contains invalid file img\clipboard-icon.png` → Ordner-Struktur korrigiert
3. `contains invalid file hbci.datatransfer.jar` → Fat-JAR verwendet
4. `plugin zip-file empty` → Explizite Verzeichnis-Einträge hinzugefügt
