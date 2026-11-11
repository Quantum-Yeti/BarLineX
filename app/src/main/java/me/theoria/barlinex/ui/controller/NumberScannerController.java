package me.theoria.barlinex.ui.controller;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

import me.theoria.barlinex.ui.scanner.NumberScanner;

public class NumberScannerController {

    private static final String TAG = "NumberScannerController";

    public interface Listener {
        void onNumberDetected(String number);
        void onScanError(String message, Exception error);
    }

    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private final MaterialButton captureButton;
    private final NumberScanner numberScanner;
    private final Listener listener;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Uri pendingImageUri;

    public NumberScannerController(
            AppCompatActivity activity,
            PreviewView previewView,
            MaterialButton captureButton,
            Listener listener
    ) {
        this.activity = activity;
        this.previewView = previewView;
        this.captureButton = captureButton;
        this.listener = listener;
        this.numberScanner = new NumberScanner();
    }

    // Camera
    public void startCamera() {

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(activity);

        future.addListener(() -> {

            try {

                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }

                cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();

                preview.setSurfaceProvider(
                        previewView.getSurfaceProvider()
                );

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(
                                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                        )
                        .build();

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        activity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                );

            } catch (ExecutionException | InterruptedException e) {

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                Log.e(TAG, "Could not start camera", e);

                listener.onScanError(
                        "Could not open the camera.",
                        e
                );
            }

        }, ContextCompat.getMainExecutor(activity));
    }

    public void stopCamera() {

        imageCapture = null;

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    // Image capture
    public void capturePhoto() {

        if (imageCapture == null) {
            Toast.makeText(
                    activity,
                    "Camera is not ready yet.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        ContentValues values = new ContentValues();

        values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "BarLineX_scan_" + System.currentTimeMillis() + ".jpg"
        );

        values.put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/BarLineX/Scans"
            );
        }

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(
                        activity.getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                ).build();

        captureButton.setEnabled(false);

        imageCapture.takePicture(
                options,
                ContextCompat.getMainExecutor(activity),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults results
                    ) {

                        captureButton.setEnabled(true);

                        Uri savedUri = results.getSavedUri();

                        if (savedUri == null) {
                            listener.onScanError(
                                    "Could not save the photo. Please try again.",
                                    null
                            );
                            return;
                        }

                        pendingImageUri = savedUri;

                        processImage(savedUri);
                    }

                    @Override
                    public void onError(
                            @NonNull ImageCaptureException exception
                    ) {

                        captureButton.setEnabled(true);

                        Log.e(TAG, "Photo capture failed", exception);

                        listener.onScanError(
                                "Could not take the photo. Please try again.",
                                exception
                        );
                    }
                }
        );
    }

    // Number recognition
    private void processImage(Uri imageUri) {

        numberScanner.scan(
                activity,
                imageUri,
                new NumberScanner.Listener() {

                    @Override
                    public void onScanResult(String number) {

                        if (activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }

                        if (number == null || number.isEmpty()) {

                            listener.onScanError(
                                    "No numbers detected. Try again with better lighting and a clearer photo.",
                                    null
                            );

                        } else {

                            listener.onNumberDetected(number);
                        }
                    }

                    @Override
                    public void onScanError(Exception error) {

                        if (!activity.isFinishing() && !activity.isDestroyed()) {
                            listener.onScanError(
                                    "Could not read the number. Please try another photo.",
                                    error
                            );
                        }
                    }

                    @Override
                    public void onScanComplete() {
                        deleteImage(imageUri);
                    }
                }
        );
    }

    // Cleanup
    private void deleteImage(Uri imageUri) {

        try {
            activity.getContentResolver().delete(
                    imageUri,
                    null,
                    null
            );

        } catch (SecurityException e) {
            Log.w(TAG, "Could not delete temporary scan image", e);
        }

        if (imageUri.equals(pendingImageUri)) {
            pendingImageUri = null;
        }
    }

    public void close() {
        stopCamera();

        if (pendingImageUri != null) {
            deleteImage(pendingImageUri);
        }
    }
}

