# SEPA Data Transfer Plugin for Jameica/Hibiscus

A combined Jameica/Hibiscus plugin that reads SEPA payment data from QR codes and OCR (invoices), with automatic detection of the source type.

## Features

- **Automatic Detection**: Automatically determines if data comes from a QR code (EPC/EMV) or needs OCR processing
- **Multiple Input Methods**:
  - **File** - Load PDF invoices or image files (PNG, JPG, BMP, TIFF)
  - **Clipboard** - Read images directly from clipboard (e.g., screenshots)
  - **Webcam** - Scan QR codes in real-time using your webcam
- **QR Code Support**:
  - EPC (BCD) format - Standard European Payment Council format
  - EMV (TLV) format - EMV standard used in payment terminals
- **OCR Support**:
  - Tesseract 5.5.2 (via tess4j 5.19.0)
  - PDF text extraction (direct and OCR fallback)
  - Configurable OCR settings (OEM, PSM, DPI, language)
- **SEPA Transfer**: Direct creation of transfer drafts in Hibiscus
- **Internationalization**: Full English and German language support

## Requirements

- Jameica 2.0+
- Hibiscus 2.0+
- Java 8+
- Tesseract OCR (included)

## Installation

1. Download the latest release from the [Releases](https://github.com/istra711/DataTransfer/releases) page
2. Open Jameica
3. Go to **Datei > Plugins online suchen... > Plugin manuell installieren...**
4. Select the downloaded ZIP file
5. Restart Jameica

## Usage

1. In Hibiscus, navigate to **Zahlungsverkehr > Daten-Transfer**
2. Choose one of the input methods:
   - **Datei (PDF/Bild)** - Opens a file dialog for PDF or image files
   - **Zwischenablage** - Reads image from clipboard
   - **Webcam (QR)** - Opens webcam for real-time QR code scanning
3. The plugin automatically detects the source type:
   - If a QR code is found, it opens the QR Code Transfer view
   - If no QR code is found, it performs OCR and opens the OCR Transfer view
4. Review and correct the extracted data if needed
5. Click **Create Transfer** to create a transfer draft in Hibiscus

### Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| File (PDF/Image) | `Ctrl+Shift+D` |
| Clipboard | `Ctrl+Shift+V` |
| Webcam (QR) | `Ctrl+Shift+W` |

## Technical Details

### Architecture

```
src/de/willuhn/jameica/hbci/datatransfer/
├── DataTransferPlugin.java          # Plugin entry point
├── OcrSettings.java                 # OCR settings management
├── action/
│   ├── FileAction.java              # File input (PDF/Image) with auto-detection
│   ├── ClipboardAction.java         # Clipboard input with auto-detection
│   ├── WebcamAction.java            # Webcam QR code scanning
│   └── SettingsAction.java          # Open settings view
├── gui/
│   ├── InvoiceView.java             # OCR view with raw text panel
│   ├── QRCodeView.java              # QR code data view
│   ├── InvoiceDebugView.java        # Debug view for extracted data
│   └── SettingsView.java            # Settings view
├── model/
│   └── TransferData.java            # Unified data model
└── parser/
    ├── SmartDetector.java           # Auto-detection (QR vs OCR)
    ├── OcrEngine.java               # Tesseract wrapper
    ├── InvoiceTextParser.java       # Regex parser for OCR text
    ├── QrCodeParser.java            # Parser interface
    ├── EpcParser.java               # EPC (BCD) format parser
    ├── EmvParser.java               # EMV (TLV) format parser
    └── QrCodeSelector.java          # Multi-QR detection and selection
```

### Dependencies

- **tess4j** 5.19.0 - Java wrapper for Tesseract 5.5.2 OCR
- **PDFBox** 3.0.7 - PDF text extraction and rendering
- **ZXing** 3.5.3 - QR code decoding (from Jameica/Hibiscus)
- **JavaCV** 1.5.9 - Webcam access (from Jameica/Hibiscus)
- **JNA** 5.18.1 - Native library access
- **SLF4J** 2.0.18 - Logging API

### Smart Detection Logic

The plugin uses a smart detection algorithm:

1. **File Input**:
   - For PDF files: Try QR code first → Text extraction → OCR fallback
   - For image files: Try QR code first → OCR fallback

2. **Clipboard Input**:
   - Try QR code first → OCR fallback

3. **Webcam Input**:
   - QR code only (no OCR)

### Build with Ant

```bash
# Set JAVA_HOME (adjust path as needed)
export JAVA_HOME="/path/to/jdk"

# Build
ant dist

# The plugin will be in dist/hbci.datatransfer/
```

## Version History

### v2.0.0

- Initial release of combined plugin
- Merged OCRtransfer and QRtransfer functionality
- Automatic QR/OCR detection
- Unified settings view
- Full i18n support (English and German)

## License

GPL v3 - See [LICENSE](LICENSE) for details.
