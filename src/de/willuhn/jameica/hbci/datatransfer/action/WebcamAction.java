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
import de.willuhn.jameica.hbci.datatransfer.OcrSettings;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
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

    private static ClassLoader getGlobalClassLoader() {
        return Application.getClassLoader();
    }

    private static Class<?> forName(String name) throws ClassNotFoundException {
        return Class.forName(name, true, getGlobalClassLoader());
    }

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
        Thread webcamThread = new Thread(() -> runWebcam(), "WebcamAction");
        webcamThread.setDaemon(true);
        webcamThread.start();
    }

    private void runWebcam() {
        final I18N i;
        try {
            i = getI18n();
        } catch (Exception e) {
            System.err.println("WEBCAM ERROR: cannot get I18N: " + e.getMessage());
            return;
        }

        final OcrSettings settings = OcrSettings.getInstance();
        final AtomicBoolean found = new AtomicBoolean(false);
        final String[] qrText = {null};

        boolean useIpCamera = "ip".equals(settings.getWebcamSource());
        String ipUrl = useIpCamera ? settings.getIpCameraUrl() : null;

        if (useIpCamera && (ipUrl == null || ipUrl.trim().isEmpty())) {
            JOptionPane.showMessageDialog(null,
                i.tr("webcam.ip.notconfigured"),
                i.tr("qrcode.scan.title"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        int deviceIndex = -1;
        if (!useIpCamera) {
            deviceIndex = settings().getLocalDeviceIndex();
            if (deviceIndex < 0) {
                deviceIndex = selectDevice(i);
                if (deviceIndex < 0) {
                    return;
                }
            }
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

        if (useIpCamera) {
            runIpCameraScan(ipUrl, scanFrame, imageLabel, status, found, qrText, i);
        } else {
            runLocalWebcamScan(deviceIndex, scanFrame, imageLabel, status, found, qrText, i);
        }
    }

    private void runIpCameraScan(String url, JFrame scanFrame, JLabel imageLabel,
            JLabel status, AtomicBoolean found, String[] qrText, I18N i) {
        Thread scanThread = new Thread(() -> {
            Object grabber = null;
            try {
                Logger.info("webcam: IP camera - loading FFmpegFrameGrabber...");
                Class<?> grabberClass = forName("org.bytedeco.javacv.FFmpegFrameGrabber");
                Logger.info("webcam: FFmpegFrameGrabber class loaded");

                grabber = grabberClass.getConstructor(String.class).newInstance(url);
                Logger.info("webcam: FFmpegFrameGrabber created for: " + url);

                Logger.info("webcam: calling start()...");
                grabberClass.getMethod("start").invoke(grabber);
                Logger.info("webcam: FFmpegFrameGrabber started OK");

                Class<?> frameClass = forName("org.bytedeco.javacv.Frame");
                Class<?> converterClass = forName("org.bytedeco.javacv.Java2DFrameConverter");
                Object converter = converterClass.getConstructor().newInstance();
                Method convertMethod = converterClass.getMethod("convert", frameClass);

                Map<DecodeHintType, Object> hints = new HashMap<>();
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));

                int frameCount = 0;

                while (scanFrame.isDisplayable() && !found.get()) {
                    Object frame = grabberClass.getMethod("grab").invoke(grabber);
                    if (frame == null) {
                        try { Thread.sleep(100); } catch (InterruptedException ie) { break; }
                        continue;
                    }

                    BufferedImage image = null;
                    try {
                        Object img = convertMethod.invoke(converter, frame);
                        if (img instanceof BufferedImage) {
                            image = (BufferedImage) img;
                        }
                    } catch (Exception e) {
                        // Converter fehlgeschlagen
                    }

                    if (image != null) {
                        final BufferedImage displayImage = rotateImage(image, settings().getRotation());
                        SwingUtilities.invokeLater(() -> {
                            imageLabel.setIcon(new ImageIcon(
                                displayImage.getScaledInstance(320, 240, Image.SCALE_FAST)));
                        });

                        frameCount++;
                        if (frameCount % 3 == 0) {
                            try {
                                String decoded = decodeQrCode(displayImage);
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

                grabberClass.getMethod("stop").invoke(grabber);
                grabberClass.getMethod("release").invoke(grabber);
                Logger.info("webcam: FFmpegFrameGrabber stopped");

            } catch (Exception e) {
                Logger.error("webcam: IP camera scan failed: " + e.getMessage(), e);
                final String errMsg = e.getMessage();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(scanFrame,
                        i.tr("webcam.init.failed", errMsg != null ? errMsg : e.getClass().getName()),
                        i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
                    scanFrame.dispose();
                });
            }

            handleScanResult(found, qrText, scanFrame, i);
        }, "QR-Scanner-IP");
        scanThread.setDaemon(true);
        scanThread.start();
    }

    private void runLocalWebcamScan(int deviceIndex, JFrame scanFrame, JLabel imageLabel,
            JLabel status, AtomicBoolean found, String[] qrText, I18N i) {
        Object capture;
        try {
            Logger.info("webcam: loading VideoCapture class...");
            Class<?> captureClass = forName("org.bytedeco.opencv.opencv_videoio.VideoCapture");
            Logger.info("webcam: VideoCapture class loaded OK");

            capture = captureClass.getConstructor().newInstance();
            Logger.info("webcam: VideoCapture instance created OK");

            final Object cap = capture;
            final int devIdx = deviceIndex;
            final int timeout = settings().getIpTimeout();
            Logger.info("webcam: opening device " + devIdx);
            java.util.concurrent.FutureTask<Boolean> openTask = new java.util.concurrent.FutureTask<>(() -> {
                cap.getClass().getMethod("open", int.class).invoke(cap, devIdx);
                boolean opened = (Boolean) cap.getClass().getMethod("isOpened").invoke(cap);
                Logger.info("webcam: isOpened = " + opened);
                return opened;
            });
            Thread openThread = new Thread(openTask);
            openThread.setDaemon(true);
            openThread.start();

            Boolean isOpened = openTask.get(timeout, java.util.concurrent.TimeUnit.SECONDS);
            if (!isOpened) {
                Logger.error("webcam: isOpened() returned false for device " + deviceIndex);
                JOptionPane.showMessageDialog(null,
                    i.tr("webcam.cannot.start", "isOpened() returned false"),
                    i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
                scanFrame.dispose();
                return;
            }
        } catch (java.util.concurrent.TimeoutException te) {
            Logger.error("webcam: open() timed out", te);
            JOptionPane.showMessageDialog(null,
                i.tr("webcam.cannot.start", "open() timed out"),
                i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
            scanFrame.dispose();
            return;
        } catch (Throwable t) {
            Logger.error("webcam: " + t.getClass().getName() + ": " + t.getMessage(), t);
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName();
            JOptionPane.showMessageDialog(null,
                i.tr("webcam.cannot.start", msg),
                i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
            scanFrame.dispose();
            return;
        }

        final Object captureRef = capture;

        Thread scanThread = new Thread(() -> {
            try {
                Class<?> matClass = forName("org.bytedeco.opencv.opencv_core.Mat");

                java.lang.reflect.Method readMethod = captureRef.getClass().getMethod("read", matClass);
                java.lang.reflect.Method releaseCaptureMethod = captureRef.getClass().getMethod("release");

                Object mat = matClass.getConstructor().newInstance();
                java.lang.reflect.Method releaseMatMethod = matClass.getMethod("release");
                java.lang.reflect.Method emptyMethod = matClass.getMethod("empty");
                java.lang.reflect.Method rowsMethod = matClass.getMethod("rows");
                java.lang.reflect.Method colsMethod = matClass.getMethod("cols");

                Class<?> toMatConverterClass = forName("org.bytedeco.javacv.OpenCVFrameConverter$ToMat");
                Class<?> frameClass = forName("org.bytedeco.javacv.Frame");
                Class<?> java2dConverterClass = forName("org.bytedeco.javacv.Java2DFrameConverter");

                Object toMatConverter = toMatConverterClass.getConstructor().newInstance();
                Object java2dConverter = java2dConverterClass.getConstructor().newInstance();

                java.lang.reflect.Method convertToFrame = toMatConverterClass.getMethod("convert", matClass);
                java.lang.reflect.Method convertToImage = java2dConverterClass.getMethod("convert", frameClass);

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
                        final BufferedImage displayImage = rotateImage(image, settings().getRotation());
                        SwingUtilities.invokeLater(() -> {
                            imageLabel.setIcon(new ImageIcon(
                                displayImage.getScaledInstance(320, 240, Image.SCALE_FAST)));
                        });

                        frameCount++;
                        if (frameCount % 3 == 0) {
                            try {
                                String decoded = decodeQrCode(displayImage);
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
                Logger.error("webcam: scan thread failed: " + e.getMessage(), e);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(scanFrame,
                        i.tr("webcam.init.failed", e.getMessage()),
                        i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
                    scanFrame.dispose();
                });
            }

            handleScanResult(found, qrText, scanFrame, i);
        }, "QR-Scanner-Local");
        scanThread.setDaemon(true);
        scanThread.start();
    }

    private void handleScanResult(AtomicBoolean found, String[] qrText, JFrame scanFrame, I18N i) {
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
    }

    private BufferedImage rotateImage(BufferedImage image, int degrees) {
        if (degrees == 0) return image;
        int w = image.getWidth();
        int h = image.getHeight();
        double radians = Math.toRadians(degrees);
        boolean swap = (degrees == 90 || degrees == 270);
        int newW = swap ? h : w;
        int newH = swap ? w : h;
        BufferedImage rotated = new BufferedImage(newW, newH, image.getType());
        Graphics2D g = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate(newW / 2.0, newH / 2.0);
        at.rotate(radians);
        at.translate(-w / 2.0, -h / 2.0);
        g.drawRenderedImage(image, at);
        g.dispose();
        return rotated;
    }

    private OcrSettings settings() {
        return OcrSettings.getInstance();
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
