package me.theoria.barlinex.ui;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.controller.BarcodeDecoderController;
import me.theoria.barlinex.ui.helper.BottomNavigationHelper;
import me.theoria.barlinex.ui.helper.WindowsInsetHelper;

public class DecodeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private PreviewView cameraPreview;
    private View scannerPlaceholder;
    private TextView textDecodeStatus;
    private View decodeResultPanel;
    private TextView textDecodedFormat;
    private TextView textDecodedContent;

    private String decodedContent;

    private BarcodeDecoderController decodeController;

    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);

        WindowsInsetHelper.applySystemBarInsets(
                findViewById(R.id.decodeRoot)
        );

        initializeViews();
        registerCameraPermissionLauncher();
        initializeDecoder();
        setupBottomNavigation();
        setupGalleryButton();

        checkCameraPermission();
        setupResultActions();
    }

    private void initializeViews() {

        bottomNavigation = findViewById(R.id.decodeBottomNavigation);
        cameraPreview = findViewById(R.id.decodeCameraPreview);
        scannerPlaceholder = findViewById(R.id.scannerPlaceholder);
        textDecodeStatus = findViewById(R.id.textDecodeStatus);
        decodeResultPanel = findViewById(R.id.decodeResultPanel);
        textDecodedFormat = findViewById(R.id.textDecodedFormat);
        textDecodedContent = findViewById(R.id.textDecodedContent);
    }

    private void registerCameraPermissionLauncher() {

        cameraPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        granted -> {

                            if (granted) {
                                startScanning();
                            } else {
                                textDecodeStatus.setText("PERMISSION REQUIRED");
                                Toast.makeText(
                                        this,
                                        "Camera permission is needed to scan barcodes.",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }

    private void initializeDecoder() {

        decodeController = new BarcodeDecoderController(
                this,
                cameraPreview,
                new BarcodeDecoderController.Listener() {

                    @Override
                    public void onBarcodeDetected(
                            String content,
                            com.google.zxing.BarcodeFormat format
                    ) {

                        decodedContent = content;

                        decodeController.stopCamera();

                        cameraPreview.setVisibility(View.GONE);
                        scannerPlaceholder.setVisibility(View.GONE);
                        decodeResultPanel.setVisibility(View.VISIBLE);

                        textDecodeStatus.setText("DETECTED");

                        textDecodedFormat.setText(format.toString().replace("_", " "));
                        textDecodedContent.setText(content);
                    }

                    @Override
                    public void onScanError(String message, Exception error) {

                        textDecodeStatus.setText("CAMERA ERROR");

                        Toast.makeText(
                                DecodeActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startScanning();

        } else {

            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScanning() {

        if (decodeController == null) {
            return;
        }

        scannerPlaceholder.setVisibility(View.GONE);
        textDecodeStatus.setText("SCANNING");

        decodeController.startCamera();
    }

    private void setupGalleryButton() {

        findViewById(R.id.buttonGallery).setOnClickListener(view -> {
            Toast.makeText(
                    this,
                    "Gallery decoding is coming next.",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void setupResultActions() {

        findViewById(R.id.buttonCopyResult).setOnClickListener(view -> {

            if (decodedContent == null) {
                return;
            }

            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            ClipData clip = ClipData.newPlainText(
                    "Decoded barcode",
                    decodedContent
            );

            clipboard.setPrimaryClip(clip);

            Toast.makeText(
                    this,
                    "Copied to clipboard",
                    Toast.LENGTH_SHORT
            ).show();
        });

        findViewById(R.id.buttonShareResult).setOnClickListener(view -> {

            if (decodedContent == null) {
                return;
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, decodedContent);

            startActivity(
                    Intent.createChooser(shareIntent, "Share decoded content")
            );
        });

        findViewById(R.id.buttonScanAgain).setOnClickListener(view -> {

            decodedContent = null;

            decodeResultPanel.setVisibility(View.GONE);
            cameraPreview.setVisibility(View.VISIBLE);

            decodeController.resetScanning();

            startScanning();
        });
    }

    private void setupBottomNavigation() {

        BottomNavigationHelper.setup(
                bottomNavigation,
                R.id.nav_decode,
                new BottomNavigationHelper.Listener() {

                    @Override
                    public void onCreateSelected() {
                        finish();
                    }

                    @Override
                    public void onDecodeSelected() {
                        // Already on Decode.
                    }

                    @Override
                    public void onHistorySelected() {
                        startActivity(
                                new Intent(
                                        DecodeActivity.this,
                                        HistoryActivity.class
                                )
                        );
                        finish();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (decodeController != null
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (decodeController != null) {
            decodeController.stopCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (decodeController != null) {
            decodeController.close();
            decodeController = null;
        }
    }
}
