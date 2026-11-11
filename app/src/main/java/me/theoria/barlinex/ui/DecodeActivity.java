package me.theoria.barlinex.ui;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.zxing.BarcodeFormat;

import java.text.DateFormat;
import java.util.Date;

import me.theoria.barlinex.R;
import me.theoria.barlinex.domain.validator.BarcodeValidator;
import me.theoria.barlinex.ui.controller.BarcodeDecoderController;
import me.theoria.barlinex.ui.helper.BottomNavigationHelper;
import me.theoria.barlinex.ui.helper.DecodeResult;
import me.theoria.barlinex.ui.helper.WindowsInsetHelper;
import me.theoria.barlinex.ui.helper.ZoomControlsController;
import me.theoria.barlinex.ui.viewmodel.DecoderViewModel;
import me.theoria.barlinex.ui.widget.ScannerOverlay;

public class DecodeActivity extends AppCompatActivity {

    // Views

    private BottomNavigationView bottomNavigation;
    private PreviewView cameraPreview;

    private View scannerPlaceholder;
    private TextView textDecodeStatus;
    private View decodeResultPanel;
    private ScannerOverlay scannerOverlay;

    private TextView textDecodedFormat;
    private TextView textDecodedContent;
    private TextView textDecodedType;
    private TextView textDecodedLength;
    private TextView textDecodedCheckDigit;
    private TextView textDecodedTime;

    // ViewModel

    private DecoderViewModel decodeViewModel;

    // Controllers

    private BarcodeDecoderController decodeController;
    private ZoomControlsController zoomControlsController;

    // Camera state

    private boolean isScanning = false;

    // Permissions

    private ActivityResultLauncher<String> cameraPermissionLauncher;


    // Activity initialization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);

        WindowsInsetHelper.applySystemBarInsets(
                findViewById(R.id.decodeRoot)
        );

        initializeViews();
        initializeViewModel();
        registerCameraPermissionLauncher();
        initializeDecoder();

        zoomControlsController = new ZoomControlsController(
                this,
                decodeController
        );

        setupBottomNavigation();
        setupGalleryButton();
        setupResultActions();

        checkCameraPermission();
    }


    // View initialization

    private void initializeViews() {

        bottomNavigation = findViewById(
                R.id.decodeBottomNavigation
        );

        cameraPreview = findViewById(
                R.id.decodeCameraPreview
        );

        scannerPlaceholder = findViewById(
                R.id.scannerPlaceholder
        );

        textDecodeStatus = findViewById(
                R.id.textDecodeStatus
        );

        decodeResultPanel = findViewById(
                R.id.decodeResultPanel
        );

        scannerOverlay = findViewById(
                R.id.scannerOverlay
        );

        textDecodedFormat = findViewById(
                R.id.textDecodedFormat
        );

        textDecodedContent = findViewById(
                R.id.textDecodedContent
        );

        textDecodedType = findViewById(
                R.id.textDecodedType
        );

        textDecodedLength = findViewById(
                R.id.textDecodedLength
        );

        textDecodedCheckDigit = findViewById(
                R.id.textDecodedCheckDigit
        );

        textDecodedTime = findViewById(
                R.id.textDecodedTime
        );
    }


    // ViewModel

    private void initializeViewModel() {

        decodeViewModel = new ViewModelProvider(this)
                .get(DecoderViewModel.class);

        observeDecodedResult();
    }

    private void observeDecodedResult() {

        decodeViewModel.getDecodedResult().observe(
                this,
                result -> {

                    if (result == null) {
                        return;
                    }

                    isScanning = false;

                    cameraPreview.setVisibility(View.GONE);
                    scannerPlaceholder.setVisibility(View.GONE);
                    decodeResultPanel.setVisibility(View.VISIBLE);

                    if (zoomControlsController != null) {
                        zoomControlsController.hide();
                    }

                    textDecodeStatus.setText("DETECTED");

                    displayBarcodeDetails(result);
                }
        );
    }


    // Camera permissions

    private void registerCameraPermissionLauncher() {

        cameraPermissionLauncher = registerForActivityResult(
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

    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startScanning();

        } else {

            cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }


    // Barcode decoder

    private void initializeDecoder() {

        decodeController = new BarcodeDecoderController(
                this,
                cameraPreview,
                scannerOverlay,
                new BarcodeDecoderController.Listener() {

                    @Override
                    public void onBarcodeDetected(
                            String content,
                            BarcodeFormat format
                    ) {

                        runOnUiThread(() -> {

                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            isScanning = false;

                            decodeController.stopCamera();

                            decodeViewModel.onBarcodeDetected(
                                    content,
                                    format
                            );
                        });
                    }

                    @Override
                    public void onZoomCapabilitiesChanged(
                            float maxZoomRatio
                    ) {

                        runOnUiThread(() -> {

                            if (zoomControlsController != null) {

                                zoomControlsController
                                        .onZoomCapabilitiesChanged(
                                                maxZoomRatio
                                        );
                            }
                        });
                    }

                    @Override
                    public void onScanError(
                            String message,
                            Exception error
                    ) {

                        runOnUiThread(() -> {

                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            isScanning = false;

                            textDecodeStatus.setText("CAMERA ERROR");

                            Toast.makeText(
                                    DecodeActivity.this,
                                    message,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }


    // Barcode result

    private void displayBarcodeDetails(DecodeResult result) {

        String content = result.getContent();
        BarcodeFormat format = result.getFormat();

        textDecodedFormat.setText(
                format.toString().replace("_", " ")
        );

        textDecodedContent.setText(content);

        textDecodedType.setText(
                result.isTwoDimensional()
                        ? "2D"
                        : "Linear"
        );

        int characterCount = result.getCharacterCount();

        textDecodedLength.setText(
                characterCount + (
                        characterCount == 1
                                ? " character"
                                : " characters"
                )
        );

        textDecodedCheckDigit.setText(
                BarcodeValidator.getCheckDigitStatus(
                        content,
                        format
                )
        );

        String scanTime = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
        ).format(new Date(result.getScannedAt()));

        textDecodedTime.setText(scanTime);
    }


    // Scanning

    private void startScanning() {

        if (decodeController == null
                || isScanning
                || decodeViewModel.hasResult()) {

            return;
        }

        isScanning = true;

        scannerPlaceholder.setVisibility(View.GONE);
        decodeResultPanel.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.VISIBLE);

        textDecodeStatus.setText("SCANNING");

        scannerOverlay.setVisibility(View.VISIBLE);

        decodeController.startCamera();
    }


    // Gallery

    private void setupGalleryButton() {

        findViewById(R.id.buttonGallery).setOnClickListener(view -> {

            Toast.makeText(
                    this,
                    "Gallery decoding is coming next.",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }


    // Result actions

    private void setupResultActions() {

        findViewById(R.id.buttonCopyResult).setOnClickListener(view -> {

            DecodeResult result =
                    decodeViewModel.getDecodedResult().getValue();

            if (result == null) {
                return;
            }

            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(
                            Context.CLIPBOARD_SERVICE
                    );

            ClipData clip = ClipData.newPlainText(
                    "Decoded barcode",
                    result.getContent()
            );

            clipboard.setPrimaryClip(clip);

            Toast.makeText(
                    this,
                    "Copied to clipboard",
                    Toast.LENGTH_SHORT
            ).show();
        });


        findViewById(R.id.buttonShareResult).setOnClickListener(view -> {

            DecodeResult result =
                    decodeViewModel.getDecodedResult().getValue();

            if (result == null) {
                return;
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            shareIntent.setType("text/plain");

            shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    result.getContent()
            );

            startActivity(
                    Intent.createChooser(
                            shareIntent,
                            "Share decoded barcode content"
                    )
            );
        });


        findViewById(R.id.buttonScanAgain).setOnClickListener(view -> {

            decodeViewModel.clearResult();

            decodeResultPanel.setVisibility(View.GONE);
            cameraPreview.setVisibility(View.VISIBLE);
            scannerOverlay.setVisibility(View.VISIBLE);

            decodeController.resetScanning();

            startScanning();
        });
    }


    // Bottom navigation

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


    // Zoom touch events

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (zoomControlsController != null) {
            zoomControlsController.onTouchEvent(event);
        }

        return super.dispatchTouchEvent(event);
    }


    // Activity lifecycle

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

        isScanning = false;

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
