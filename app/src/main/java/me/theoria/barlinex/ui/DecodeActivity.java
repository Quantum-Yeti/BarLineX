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
import com.google.zxing.BarcodeFormat;

import java.text.DateFormat;
import java.util.Date;

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
    private View scannerOverlay;

    private TextView textDecodedFormat;
    private TextView textDecodedContent;
    private TextView textDecodedType;
    private TextView textDecodedLength;
    private TextView textDecodedCheckDigit;
    private TextView textDecodedTime;

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
        scannerOverlay = findViewById(R.id.scannerOverlay);

        textDecodedFormat = findViewById(R.id.textDecodedFormat);
        textDecodedContent = findViewById(R.id.textDecodedContent);
        textDecodedType = findViewById(R.id.textDecodedType);
        textDecodedLength = findViewById(R.id.textDecodedLength);
        textDecodedCheckDigit = findViewById(R.id.textDecodedCheckDigit);
        textDecodedTime = findViewById(R.id.textDecodedTime);
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
                            BarcodeFormat format
                    ) {

                        decodedContent = content;

                        decodeController.stopCamera();

                        cameraPreview.setVisibility(View.GONE);
                        scannerPlaceholder.setVisibility(View.GONE);
                        decodeResultPanel.setVisibility(View.VISIBLE);

                        textDecodeStatus.setText("DETECTED");

                        displayBarcodeDetails(content, format);
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

    private void displayBarcodeDetails(
            String content,
            BarcodeFormat format
    ) {

        textDecodedFormat.setText(
                format.toString().replace("_", " ")
        );

        textDecodedContent.setText(content);

        // Barcode category
        textDecodedType.setText(
                format == BarcodeFormat.QR_CODE ? "2D" : "Linear"
        );

        // Character count
        int characterCount = content.codePointCount(
                0,
                content.length()
        );

        textDecodedLength.setText(
                characterCount + (
                        characterCount == 1
                                ? " character"
                                : " characters"
                )
        );

        // Check digit validation
        textDecodedCheckDigit.setText(
                getCheckDigitStatus(content, format)
        );

        // Scan date and time
        String scanTime = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
        ).format(new Date());

        textDecodedTime.setText(scanTime);
    }

    private String getCheckDigitStatus(
            String content,
            BarcodeFormat format
    ) {

        switch (format) {

            case EAN_13:
                return validateMod10(content, 13)
                        ? "Valid"
                        : "Invalid";

            case EAN_8:
                return validateMod10(content, 8)
                        ? "Valid"
                        : "Invalid";

            case UPC_A:
                return validateMod10(content, 12)
                        ? "Valid"
                        : "Invalid";

            case UPC_E:
                return validateUpcE(content)
                        ? "Valid"
                        : "Invalid";

            default:
                return "Not applicable";
        }
    }

    private boolean validateMod10(String content, int expectedLength) {

        if (content == null
                || content.length() != expectedLength
                || !content.matches("[0-9]+")) {
            return false;
        }

        int sum = 0;
        int weight = 3;

        // Process all digits except the check digit.
        for (int i = content.length() - 2; i >= 0; i--) {

            int digit = content.charAt(i) - '0';

            sum += digit * weight;

            weight = (weight == 3) ? 1 : 3;
        }

        int expectedCheckDigit = (10 - (sum % 10)) % 10;
        int actualCheckDigit =
                content.charAt(content.length() - 1) - '0';

        return expectedCheckDigit == actualCheckDigit;
    }

    private boolean validateUpcE(String content) {

        if (content == null
                || content.length() != 8
                || !content.matches("[0-9]+")) {
            return false;
        }

        String numberSystem = content.substring(0, 1);

        if (!numberSystem.equals("0")
                && !numberSystem.equals("1")) {
            return false;
        }

        String compressed = content.substring(1, 7);

        char lastDigit = compressed.charAt(5);

        String manufacturer;
        String product;

        switch (lastDigit) {

            case '0':
            case '1':
            case '2':
                manufacturer =
                        compressed.substring(0, 2) + lastDigit + "00";
                product =
                        "00" + compressed.substring(2, 5);
                break;

            case '3':
                manufacturer =
                        compressed.substring(0, 3) + "00";
                product =
                        "000" + compressed.substring(3, 5);
                break;

            case '4':
                manufacturer =
                        compressed.substring(0, 4) + "0";
                product =
                        "0000" + compressed.substring(4, 5);
                break;

            default:
                manufacturer =
                        compressed.substring(0, 5);
                product =
                        "0000" + lastDigit;
                break;
        }

        String expandedUpcA =
                numberSystem + manufacturer + product;

        return validateMod10(expandedUpcA + content.charAt(7), 12);
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

    private void startScanning() {

        if (decodeController == null) {
            return;
        }

        scannerPlaceholder.setVisibility(View.GONE);
        textDecodeStatus.setText("SCANNING");

        decodeController.startCamera();
        scannerOverlay.setVisibility(View.VISIBLE);
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
                    (ClipboardManager) getSystemService(
                            Context.CLIPBOARD_SERVICE
                    );

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
                    Intent.createChooser(
                            shareIntent,
                            "Share decoded barcodeContent"
                    )
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