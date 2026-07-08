package de.willuhn.jameica.hbci.datatransfer.action;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.hbci.datatransfer.gui.QRCodeView;
import de.willuhn.jameica.hbci.datatransfer.model.TransferData;
import de.willuhn.jameica.hbci.datatransfer.parser.EmvParser;
import de.willuhn.jameica.hbci.datatransfer.parser.EpcParser;
import de.willuhn.jameica.hbci.datatransfer.parser.ParserException;
import de.willuhn.jameica.hbci.datatransfer.parser.QrCodeParser;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebcamAction implements Action {

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

    private final QrCodeParser[] parsers = {
        new EpcParser(),
        new EmvParser()
    };

    @Override
    public void handleAction(Object context) throws ApplicationException {
        final I18N i = getI18n();
        final AtomicBoolean found = new AtomicBoolean(false);
        final String[] qrText = {null};

        int deviceIndex = selectDevice(i);
        if (deviceIndex < 0) {
            return;
        }

        Object capture;
        try {
            Class<?> captureClass = Class.forName("org.bytedeco.opencv.opencv_videoio.VideoCapture");
            capture = captureClass.getConstructor().newInstance();

            final Object cap = capture;
            final int devIdx = deviceIndex;
            java.util.concurrent.FutureTask<Boolean> openTask = new java.util.concurrent.FutureTask<>(() -> {
                cap.getClass().getMethod("open", int.class).invoke(cap, devIdx);
                return (Boolean) cap.getClass().getMethod("isOpened").invoke(cap);
            });
            Thread openThread = new Thread(openTask);
            openThread.setDaemon(true);
            openThread.start();

            Boolean isOpened;
            try {
                isOpened = openTask.get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                JOptionPane.showMessageDialog(null,
                    i.tr("webcam.cannot.start", "open() timed out after 5s"),
                    i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!isOpened) {
                JOptionPane.showMessageDialog(null,
                    i.tr("webcam.cannot.start", "isOpened() returned false"),
                    i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (Throwable t) {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName();
            JOptionPane.showMessageDialog(null,
                i.tr("webcam.cannot.start", msg),
                i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFrame scanFrame = new JFrame(i.tr("qrcode.scan.title"));
        scanFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scanFrame.add(imageLabel, BorderLayout.CENTER);

        JLabel status = new JLabel(i.tr("qrcode.scan.hold"));
        status.setHorizontalAlignment(SwingConstants.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scanFrame.add(status, BorderLayout.SOUTH);

        imageLabel.setPreferredSize(new java.awt.Dimension(320, 240));
        scanFrame.pack();
        scanFrame.setLocationRelativeTo(null);

        scanFrame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        scanFrame.getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                scanFrame.dispose();
            }
        });

        scanFrame.setVisible(true);

        final Object captureRef = capture;

        Thread scanThread = new Thread(() -> {
            try {
                Class<?> matClass = Class.forName("org.bytedeco.opencv.opencv_core.Mat");

                Method readMethod = captureRef.getClass().getMethod("read", matClass);
                Method releaseCaptureMethod = captureRef.getClass().getMethod("release");

                Object mat = matClass.getConstructor().newInstance();
                Method releaseMatMethod = matClass.getMethod("release");
                Method emptyMethod = matClass.getMethod("empty");
                Method rowsMethod = matClass.getMethod("rows");
                Method colsMethod = matClass.getMethod("cols");

                Class<?> toMatConverterClass = Class.forName("org.bytedeco.javacv.OpenCVFrameConverter$ToMat");
                Class<?> frameClass = Class.forName("org.bytedeco.javacv.Frame");
                Class<?> java2dConverterClass = Class.forName("org.bytedeco.javacv.Java2DFrameConverter");

                Object toMatConverter = toMatConverterClass.getConstructor().newInstance();
                Object java2dConverter = java2dConverterClass.getConstructor().newInstance();

                Method convertToFrame = toMatConverterClass.getMethod("convert", matClass);
                Method convertToImage = java2dConverterClass.getMethod("convert", frameClass);

                Map<DecodeHintType, Object> hints = new HashMap<>();
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));

                int frameCount = 0;

                while (scanFrame.isDisplayable() && !found.get()) {
                    Boolean success = (Boolean) readMethod.invoke(captureRef, mat);
                    if (success == null || !success) {
                        try { Thread.sleep(100); } catch (InterruptedException ie) { break; }
                        continue;
                    }

                    Boolean isEmpty = (Boolean) emptyMethod.invoke(mat);
                    if (isEmpty != null && isEmpty) {
                        try { Thread.sleep(100); } catch (InterruptedException ie) { break; }
                        continue;
                    }

                    int rows = (int) rowsMethod.invoke(mat);
                    int cols = (int) colsMethod.invoke(mat);
                    if (rows <= 0 || cols <= 0) {
                        try { Thread.sleep(100); } catch (InterruptedException ie) { break; }
                        continue;
                    }

                    BufferedImage image = null;
                    try {
                        Object frame = convertToFrame.invoke(toMatConverter, mat);
                        if (frame != null) {
                            Object img = convertToImage.invoke(java2dConverter, frame);
                            if (img instanceof BufferedImage) {
                                image = (BufferedImage) img;
                            }
                        }
                    } catch (Exception e) {
                        // Converter fehlgeschlagen
                    }

                    if (image != null) {
                        final BufferedImage displayImage = image;
                        SwingUtilities.invokeLater(() -> {
                            imageLabel.setIcon(new ImageIcon(
                                displayImage.getScaledInstance(320, 240, Image.SCALE_FAST)));
                        });

                        frameCount++;
                        if (frameCount % 3 == 0) {
                            try {
                                String decoded = decodeQrCode(image);
                                if (decoded != null) {
                                    qrText[0] = decoded;
                                    found.set(true);
                                    SwingUtilities.invokeLater(() -> {
                                        status.setText(i.tr("qrcode.detected"));
                                        scanFrame.dispose();
                                    });
                                }
                            } catch (Exception e) {
                                // Fehler ignorieren
                            }
                        }
                    }

                    try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                }

                releaseMatMethod.invoke(mat);
                releaseCaptureMethod.invoke(captureRef);

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(scanFrame,
                        i.tr("webcam.init.failed", e.getMessage()),
                        i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
                    scanFrame.dispose();
                });
            }

            if (found.get() && qrText[0] != null) {
                try {
                    TransferData data = parseQrText(qrText[0]);
                    GUI.getDisplay().asyncExec(() -> {
                        try {
                            GUI.startView(QRCodeView.class, data);
                        } catch (Exception ex) {
                            GUI.getView().setErrorText(i.tr("error.param", ex.getMessage()));
                        }
                    });
                } catch (ParserException e) {
                    GUI.getDisplay().asyncExec(() -> {
                        GUI.getView().setErrorText(i.tr("error.param", e.getMessage()));
                    });
                }
            }
        }, "QR-Scanner");

        scanThread.setDaemon(true);
        scanThread.start();
    }

    private String decodeQrCode(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    private int selectDevice(I18N i18n) {
        try {
            Class<?> grabberClass = Class.forName("org.bytedeco.javacv.FrameGrabber");
            Method listMethod = grabberClass.getMethod("getDeviceDescriptions");
            String[] devices = (String[]) listMethod.invoke(null);

            if (devices != null && devices.length > 0) {
                if (devices.length == 1) {
                    return 0;
                }

                String selected = (String) JOptionPane.showInputDialog(null,
                    i18n.tr("webcam.select.device"),
                    i18n.tr("qrcode.scan.title"),
                    JOptionPane.QUESTION_MESSAGE,
                    null, devices, devices[0]);

                if (selected == null) {
                    return -1;
                }

                for (int idx = 0; idx < devices.length; idx++) {
                    if (devices[idx].equals(selected)) {
                        return idx;
                    }
                }
            }
        } catch (Exception e) {
            // Ignorieren
        }

        String input = (String) JOptionPane.showInputDialog(null,
            i18n.tr("webcam.enter.device"),
            i18n.tr("qrcode.scan.title"),
            JOptionPane.QUESTION_MESSAGE,
            null, null, "0");

        if (input == null) {
            return -1;
        }

        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private TransferData parseQrText(String qrText) throws ParserException {
        for (QrCodeParser parser : parsers) {
            if (parser.canParse(qrText)) {
                TransferData data = parser.parse(qrText);
                data.setRawText(qrText);
                return data;
            }
        }
        throw new ParserException(
            getI18n().tr("error.no.parser")
        );
    }
}
