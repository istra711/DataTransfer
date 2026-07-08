package de.willuhn.jameica.hbci.datatransfer.action;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.hbci.datatransfer.gui.InvoiceView;
import de.willuhn.jameica.hbci.datatransfer.gui.QRCodeView;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.hbci.datatransfer.parser.SmartDetector;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Action zum Lesen aus der Zwischenablage mit automatischer Erkennung.
 */
public class ClipboardAction implements Action {

    private static final Logger logger = Logger.getLogger(ClipboardAction.class.getName());
    private static I18N i18n;

    private static synchronized I18N getI18n() {
        if (i18n == null) {
            i18n = Application.getPluginLoader()
                .getPlugin("de.willuhn.jameica.hbci.datatransfer.DataTransferPlugin")
                .getResources()
                .getI18N();
        }
        return i18n;
    }

    @Override
    public void handleAction(Object context) throws ApplicationException {
        final I18N i = getI18n();
        try {
            Display display = GUI.getDisplay();
            Clipboard clipboard = new Clipboard(display);

            try {
                Transfer transfer = ImageTransfer.getInstance();
                Object data = clipboard.getContents(transfer);

                if (data == null) {
                    throw new ApplicationException(i.tr("error.no.image.in.clipboard"));
                }

                ImageData imageData = (ImageData) data;
                logger.info("Bild aus Zwischenablage: " + imageData.width + "x" + imageData.height);

                BufferedImage bufferedImage = convertToBufferedImage(imageData);

                if (bufferedImage == null) {
                    throw new ApplicationException(i.tr("error.image.conversion.failed"));
                }

                TransferData result = SmartDetector.detectFromImage(bufferedImage, "Zwischenablage");

                if (result == null || result.getRawText() == null || result.getRawText().isEmpty()) {
                    throw new ApplicationException(i.tr("error.no.text.recognized"));
                }

                logger.info("Daten erkannt: " + result.toString());

                if (result.getSource() == TransferData.Source.QR_EPC ||
                    result.getSource() == TransferData.Source.QR_EMV) {
                    GUI.startView(QRCodeView.class, result);
                } else {
                    GUI.startView(InvoiceView.class, result);
                }

            } finally {
                clipboard.dispose();
            }

        } catch (ApplicationException e) {
            logger.warning("ApplicationException: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.warning("Exception: " + e.getClass().getName() + ": " + e.getMessage());
            throw new ApplicationException(i.tr("error.ocr.failed", e.getMessage()), e);
        }
    }

    private BufferedImage convertToBufferedImage(ImageData swtImageData) {
        try {
            org.eclipse.swt.graphics.ImageLoader loader = new org.eclipse.swt.graphics.ImageLoader();
            loader.data = new ImageData[] { swtImageData };

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            loader.save(baos, org.eclipse.swt.SWT.IMAGE_PNG);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return ImageIO.read(bais);
        } catch (Exception e) {
            logger.warning("Fehler bei der Bildkonvertierung: " + e.getMessage());
            return null;
        }
    }
}
