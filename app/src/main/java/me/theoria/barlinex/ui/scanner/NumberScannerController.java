
package me.theoria.barlinex.ui.scanner;

import android.graphics.RectF;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.view.TransformExperimental;
import androidx.camera.view.transform.CoordinateTransform;
import androidx.camera.view.transform.ImageProxyTransformFactory;
import androidx.camera.view.transform.OutputTransform;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import me.theoria.barlinex.domain.scanner.NumberScanner;

@TransformExperimental
public class NumberScannerController {

    private static final String TAG = "NumberScannerController";
    private static final long ANALYSIS_INTERVAL_MS = 250L;

    public interface Listener {
        void onTextDetected(String text);
        void onScanError(String message, Exception error);
    }

    // UI components
    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private final ScannerOverlay scannerOverlay;
    private final MaterialButton useTextButton;

    // Scanner
    private NumberScanner numberScanner;
    private final Listener listener;

    // Camera image transformation
    private final ImageProxyTransformFactory imageTransformFactory =
            new ImageProxyTransformFactory();

    // Background processing
    private final ExecutorService analysisExecutor =
            Executors.newSingleThreadExecutor();

    private final AtomicBoolean processingFrame =
            new AtomicBoolean(false);

    // Camera components
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private Camera camera;

    // Scanner state
    private volatile boolean cameraActive = false;
    private volatile boolean paused = false;

    private long lastAnalysisTime = 0L;
    private String stableText = "";

    public NumberScannerController(
            AppCompatActivity activity,
            PreviewView previewView,
            ScannerOverlay scannerOverlay,
            MaterialButton useTextButton,
            Listener listener
    ) {

        this.activity = activity;
        this.previewView = previewView;
        this.scannerOverlay = scannerOverlay;
        this.useTextButton = useTextButton;
        this.listener = listener;

        imageTransformFactory.setUsingCropRect(true);
        imageTransformFactory.setUsingRotationDegrees(true);

        useTextButton.setEnabled(false);
    }

    // Camera initialization
    public void startCamera() {

        if (numberScanner == null) {
            numberScanner = new NumberScanner();
        }

        if (cameraActive) {
            return;
        }

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(activity);

        future.addListener(() -> {

            try {

                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                cameraProvider = future.get();

                Preview preview = new Preview.Builder()
                        .build();

                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider()
                );

                int rotation = previewView.getDisplay() != null
                        ? previewView.getDisplay().getRotation()
                        : Surface.ROTATION_0;

                imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetRotation(rotation)
                        .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build();

                imageAnalysis.setAnalyzer(
                        analysisExecutor,
                        this::analyzeFrame
                );

                cameraProvider.unbindAll();

                camera = cameraProvider.bindToLifecycle(
                        activity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );

                cameraActive = true;
                paused = false;

                focusOnScanningArea();

            } catch (Exception e) {

                Log.e(TAG, "Could not start camera", e);

                listener.onScanError(
                        "Could not open the camera.",
                        e
                );
            }

        }, ContextCompat.getMainExecutor(activity));
    }

    // Camera focus
    private void focusOnScanningArea() {

        previewView.post(() -> {

            if (camera == null || !cameraActive) {
                return;
            }

            RectF guide = scannerOverlay.getGuideRect();

            MeteringPoint point =
                    previewView.getMeteringPointFactory().createPoint(
                            guide.centerX(),
                            guide.centerY()
                    );

            FocusMeteringAction action =
                    new FocusMeteringAction.Builder(point)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build();

            camera.getCameraControl().startFocusAndMetering(action);
        });
    }

    // Scanning region transformation
    private RectF getImageScanRegion(ImageProxy imageProxy) {

        OutputTransform previewTransform =
                previewView.getOutputTransform();

        if (previewTransform == null) {
            return null;
        }

        OutputTransform imageTransform =
                imageTransformFactory.getOutputTransform(imageProxy);

        RectF scanRegion = scannerOverlay.getGuideRect();

        CoordinateTransform coordinateTransform =
                new CoordinateTransform(
                        previewTransform,
                        imageTransform
                );

        coordinateTransform.mapRect(scanRegion);

        int rotation =
                imageProxy.getImageInfo().getRotationDegrees();

        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();

        if (rotation == 90 || rotation == 270) {
            imageWidth = imageProxy.getHeight();
            imageHeight = imageProxy.getWidth();
        }

        if (!scanRegion.intersect(
                0,
                0,
                imageWidth,
                imageHeight
        )) {
            return null;
        }

        return scanRegion;
    }

    // Live image analysis
    private void analyzeFrame(@NonNull ImageProxy imageProxy) {

        if (!cameraActive || paused) {
            imageProxy.close();
            return;
        }

        long now = android.os.SystemClock.elapsedRealtime();

        if (now - lastAnalysisTime < ANALYSIS_INTERVAL_MS
                || !processingFrame.compareAndSet(false, true)) {

            imageProxy.close();
            return;
        }

        lastAnalysisTime = now;

        try {

            RectF scanRegion = getImageScanRegion(imageProxy);

            if (scanRegion == null) {
                imageProxy.close();
                processingFrame.set(false);
                return;
            }

            numberScanner.process(imageProxy)

                    .addOnSuccessListener(
                            ContextCompat.getMainExecutor(activity),
                            result -> {

                                if (!cameraActive || paused
                                        || activity.isFinishing()
                                        || activity.isDestroyed()) {
                                    return;
                                }

                                String candidate =
                                        numberScanner.extractText(
                                                result,
                                                scanRegion
                                        );

                                Log.d(
                                        TAG,
                                        "Recognized text: " + candidate
                                );

                                updateStableText(candidate);
                            }
                    )

                    .addOnFailureListener(
                            ContextCompat.getMainExecutor(activity),
                            error -> Log.w(
                                    TAG,
                                    "Text recognition failed",
                                    error
                            )
                    )

                    .addOnCompleteListener(
                            ContextCompat.getMainExecutor(activity),
                            task -> {

                                imageProxy.close();
                                processingFrame.set(false);
                            }
                    );

        } catch (Exception e) {

            Log.e(TAG, "Could not process camera frame", e);

            imageProxy.close();
            processingFrame.set(false);
        }
    }

    // Recognized text handling
    private void updateStableText(String candidate) {

        if (candidate == null || candidate.trim().isEmpty()) {
            return;
        }

        stableText = candidate.trim();

        useTextButton.setEnabled(true);

        Log.d(TAG, "Text ready: " + stableText);
    }

    // User confirmation
    public void confirmCurrentText() {

        if (stableText == null || stableText.trim().isEmpty() || paused) {

            Log.d(TAG, "No recognized text available.");
            return;
        }

        paused = true;
        useTextButton.setEnabled(false);

        Log.d(TAG, "Confirming text: " + stableText);

        listener.onTextDetected(stableText);
    }

    public void resumeScanning() {

        if (!cameraActive) {
            return;
        }

        paused = false;
        stableText = "";

        useTextButton.setEnabled(false);
    }

    // Camera cleanup
    public void stopCamera() {

        cameraActive = false;
        paused = true;

        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
            imageAnalysis = null;
        }

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        camera = null;
        stableText = "";

        useTextButton.setEnabled(false);
    }

    public void close() {

        stopCamera();

        if (numberScanner != null) {
            numberScanner.close();
            numberScanner = null;
        }

        analysisExecutor.shutdown();
    }
}
