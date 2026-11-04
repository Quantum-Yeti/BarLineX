package me.theoria.barlinex.ui.controller;

import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BarcodeDecoderController {

    private static final String TAG = "BarcodeDecodeController";

    public interface Listener {
        void onBarcodeDetected(String content, BarcodeFormat format);
        void onScanError(String message, Exception error);
    }

    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private final Listener listener;

    private final ExecutorService analysisExecutor =
            Executors.newSingleThreadExecutor();

    private final AtomicBoolean resultDelivered = new AtomicBoolean(false);

    private final MultiFormatReader reader = new MultiFormatReader();

    private ProcessCameraProvider cameraProvider;
    private Preview preview;
    private ImageAnalysis imageAnalysis;

    private boolean closed;

    public BarcodeDecoderController(
            AppCompatActivity activity,
            PreviewView previewView,
            Listener listener
    ) {
        this.activity = activity;
        this.previewView = previewView;
        this.listener = listener;

        configureReader();
    }

    private void configureReader() {

        List<BarcodeFormat> formats = new ArrayList<>();

        formats.add(BarcodeFormat.QR_CODE);
        formats.add(BarcodeFormat.CODE_128);
        formats.add(BarcodeFormat.CODE_39);
        formats.add(BarcodeFormat.EAN_13);
        formats.add(BarcodeFormat.EAN_8);
        formats.add(BarcodeFormat.UPC_A);
        formats.add(BarcodeFormat.UPC_E);
        formats.add(BarcodeFormat.ITF);

        Map<DecodeHintType, Object> hints =
                new EnumMap<>(DecodeHintType.class);

        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        reader.setHints(hints);
    }

    // Camera
    public void startCamera() {

        if (closed) {
            return;
        }

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(activity);

        future.addListener(() -> {

            try {

                if (closed || activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                cameraProvider = future.get();

                preview = new Preview.Builder().build();

                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider()
                );

                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setResolutionStrategy(
                                new ResolutionStrategy(
                                        new Size(1920, 1080),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                        )
                        .build();

                imageAnalysis = new ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(
                        analysisExecutor,
                        this::analyzeImage
                );

                cameraProvider.unbindAll();

                Camera camera = cameraProvider.bindToLifecycle(
                        activity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );

                focusCamera(camera);

            } catch (Exception e) {

                Log.e(TAG, "Could not start barcode camera", e);

                notifyError(
                        "Could not open the camera.",
                        e
                );
            }

        }, ContextCompat.getMainExecutor(activity));
    }

    private void focusCamera(Camera camera) {

        previewView.post(() -> {

            if (closed || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            float centerX = previewView.getWidth() / 2f;
            float centerY = previewView.getHeight() / 2f;

            MeteringPoint point = previewView
                    .getMeteringPointFactory()
                    .createPoint(centerX, centerY);

            FocusMeteringAction action = new FocusMeteringAction.Builder(
                    point,
                    FocusMeteringAction.FLAG_AF
                            | FocusMeteringAction.FLAG_AE
                            | FocusMeteringAction.FLAG_AWB
            )
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build();

            CameraControl cameraControl = camera.getCameraControl();

            cameraControl.startFocusAndMetering(action)
                    .addListener(() -> {
                        Log.d(TAG, "Camera focus operation completed.");
                    }, ContextCompat.getMainExecutor(activity));
        });
    }

    public void stopCamera() {

        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
            imageAnalysis = null;
        }

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        preview = null;
    }

    // Barcode analysis
    private void analyzeImage(@NonNull ImageProxy image) {

        try {

            if (resultDelivered.get()) {
                return;
            }

            if (image.getFormat() != android.graphics.ImageFormat.YUV_420_888) {
                return;
            }

            ByteBuffer buffer = image.getPlanes()[0].getBuffer();

            int width = image.getWidth();
            int height = image.getHeight();

            byte[] luminance = copyLuminance(
                    buffer,
                    width,
                    height,
                    image.getPlanes()[0].getRowStride(),
                    image.getPlanes()[0].getPixelStride()
            );

            int rotation = image.getImageInfo().getRotationDegrees();

            LuminanceFrame frame = rotateLuminance(
                    luminance,
                    width,
                    height,
                    rotation
            );

            Result result = decodeFrame(frame);

            if (result != null
                    && result.getText() != null
                    && resultDelivered.compareAndSet(false, true)) {

                String content = result.getText();
                BarcodeFormat format = result.getBarcodeFormat();

                activity.runOnUiThread(() -> {

                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        listener.onBarcodeDetected(content, format);
                    }
                });
            }

        } catch (Exception e) {

            Log.e(TAG, "Barcode analysis failed", e);

        } finally {

            reader.reset();
            image.close();
        }
    }

    private Result decodeFrame(LuminanceFrame frame) {

        PlanarYUVLuminanceSource fullSource =
                new PlanarYUVLuminanceSource(
                        frame.data,
                        frame.width,
                        frame.height,
                        0,
                        0,
                        frame.width,
                        frame.height,
                        false
                );

        // Attempt 1: Full image using HybridBinarizer and fallbacks.
        Result result = tryDecode(fullSource);

        if (result != null) {
            return result;
        }

        // Attempt 2: Focus on the central horizontal band.
        int bandHeight = Math.max(1, frame.height * 2 / 3);
        int top = (frame.height - bandHeight) / 2;

        LuminanceSource centerBand = fullSource.crop(
                0,
                top,
                frame.width,
                bandHeight
        );

        return tryDecode(centerBand);
    }


    private Result tryDecode(LuminanceSource source) {

        // Attempt 1: Hybrid binarization.
        Result result = decodeWithHybrid(source);

        if (result != null) {
            return result;
        }

        // Attempt 2: Global histogram binarization.
        result = decodeWithGlobal(source);

        if (result != null) {
            return result;
        }

        // Attempt 3: Try an inverted image.
        return decodeWithHybrid(source.invert());
    }


    private Result decodeWithHybrid(LuminanceSource source) {

        try {

            BinaryBitmap bitmap = new BinaryBitmap(
                    new HybridBinarizer(source)
            );

            return reader.decodeWithState(bitmap);

        } catch (NotFoundException e) {

            reader.reset();
            return null;
        }
    }


    private Result decodeWithGlobal(LuminanceSource source) {

        try {

            BinaryBitmap bitmap = new BinaryBitmap(
                    new GlobalHistogramBinarizer(source)
            );

            return reader.decodeWithState(bitmap);

        } catch (NotFoundException e) {

            reader.reset();
            return null;
        }
    }

    private byte[] copyLuminance(
            ByteBuffer buffer,
            int width,
            int height,
            int rowStride,
            int pixelStride
    ) {

        byte[] data = new byte[width * height];
        ByteBuffer source = buffer.duplicate();

        for (int y = 0; y < height; y++) {

            int rowStart = y * rowStride;

            for (int x = 0; x < width; x++) {

                data[y * width + x] =
                        source.get(rowStart + x * pixelStride);
            }
        }

        return data;
    }

    // --------------------------------------------------
    // Image rotation
    // --------------------------------------------------

    private LuminanceFrame rotateLuminance(
            byte[] source,
            int width,
            int height,
            int rotation
    ) {

        if (rotation == 0) {
            return new LuminanceFrame(source, width, height);
        }

        if (rotation != 90 && rotation != 180 && rotation != 270) {
            return new LuminanceFrame(source, width, height);
        }

        int rotatedWidth = (rotation == 180) ? width : height;
        int rotatedHeight = (rotation == 180) ? height : width;

        byte[] rotated = new byte[rotatedWidth * rotatedHeight];

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                int destinationX;
                int destinationY;

                if (rotation == 90) {

                    destinationX = height - 1 - y;
                    destinationY = x;

                } else if (rotation == 180) {

                    destinationX = width - 1 - x;
                    destinationY = height - 1 - y;

                } else {

                    destinationX = y;
                    destinationY = width - 1 - x;
                }

                rotated[destinationY * rotatedWidth + destinationX] =
                        source[y * width + x];
            }
        }

        return new LuminanceFrame(
                rotated,
                rotatedWidth,
                rotatedHeight
        );
    }

    private static class LuminanceFrame {

        final byte[] data;
        final int width;
        final int height;

        LuminanceFrame(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }
    }

    // --------------------------------------------------
    // Scan controls
    // --------------------------------------------------

    public void resetScanning() {
        resultDelivered.set(false);
    }

    private void notifyError(String message, Exception error) {

        activity.runOnUiThread(() -> {

            if (!activity.isFinishing() && !activity.isDestroyed()) {
                listener.onScanError(message, error);
            }
        });
    }

    public void close() {

        closed = true;

        stopCamera();
        analysisExecutor.shutdown();
    }
}

