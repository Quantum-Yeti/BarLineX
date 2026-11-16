package me.theoria.barlinex.ui.decode;

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
import com.google.zxing.Result;

import java.text.DateFormat;
import java.util.Date;

import me.theoria.barlinex.R;
import me.theoria.barlinex.domain.barcode.BarcodeValidator;
import me.theoria.barlinex.ui.advanced.AdvancedActivity;
import me.theoria.barlinex.ui.common.BottomNavigationHelper;
import me.theoria.barlinex.ui.common.WindowsInsetHelper;
import me.theoria.barlinex.ui.common.ZoomControlsController;
import me.theoria.barlinex.ui.controller.BarcodeDecoderController;
import me.theoria.barlinex.ui.history.HistoryActivity;
import me.theoria.barlinex.ui.scanner.ScannerOverlay;

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

    // Content Analysis

    private TextView textAnalysisCharacters;
    private TextView textAnalysisBytes;
    private TextView textAnalysisLetters;
    private TextView textAnalysisNumbers;
    private TextView textAnalysisWhitespace;
    private TextView textAnalysisSymbols;

    // Technical Properties

    private TextView textTechnicalFormat;
    private TextView textTechnicalErrorCorrection;
    private TextView textTechnicalSymbology;
    private TextView textTechnicalByteSegments;
    private TextView textTechnicalStructuredAppend;

    // Interpretation and Validation

    private TextView textDataInterpretation;
    private TextView textValidationStatus;
    private TextView textRawData;

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
        setupExpandableSections();

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

        // Content Analysis

        textAnalysisCharacters = findViewById(
                R.id.textAnalysisCharacters
        );

        textAnalysisBytes = findViewById(
                R.id.textAnalysisBytes
        );

        textAnalysisLetters = findViewById(
                R.id.textAnalysisLetters
        );

        textAnalysisNumbers = findViewById(
                R.id.textAnalysisNumbers
        );

        textAnalysisWhitespace = findViewById(
                R.id.textAnalysisWhitespace
        );

        textAnalysisSymbols = findViewById(
                R.id.textAnalysisSymbols
        );

        // Technical Properties

        textTechnicalFormat = findViewById(
                R.id.textTechnicalFormat
        );

        textTechnicalErrorCorrection = findViewById(
                R.id.textTechnicalErrorCorrection
        );

        textTechnicalSymbology = findViewById(
                R.id.textTechnicalSymbology
        );

        textTechnicalByteSegments = findViewById(
                R.id.textTechnicalByteSegments
        );

        textTechnicalStructuredAppend = findViewById(
                R.id.textTechnicalStructuredAppend
        );

        // Interpretation and Validation

        textDataInterpretation = findViewById(
                R.id.textDataInterpretation
        );

        textValidationStatus = findViewById(
                R.id.textValidationStatus
        );

        textRawData = findViewById(
                R.id.textRawData
        );
    }

    // Expandable sections

    private void setupExpandableSections() {

        setupExpandableSection(
                R.id.headerBarcodeInformation,
                R.id.contentBarcodeInformation,
                R.id.arrowBarcodeInformation,
                true
        );

        setupExpandableSection(
                R.id.headerContentAnalysis,
                R.id.contentContentAnalysis,
                R.id.arrowContentAnalysis,
                false
        );

        setupExpandableSection(
                R.id.headerTechnicalProperties,
                R.id.contentTechnicalProperties,
                R.id.arrowTechnicalProperties,
                false
        );

        setupExpandableSection(
                R.id.headerDataInterpretation,
                R.id.contentDataInterpretation,
                R.id.arrowDataInterpretation,
                false
        );

        setupExpandableSection(
                R.id.headerValidation,
                R.id.contentValidation,
                R.id.arrowValidation,
                false
        );

        setupExpandableSection(
                R.id.headerRawData,
                R.id.contentRawData,
                R.id.arrowRawData,
                false
        );
    }

    private void setupExpandableSection(
            int headerId,
            int contentId,
            int arrowId,
            boolean initiallyExpanded
    ) {

        View header = findViewById(headerId);
        View content = findViewById(contentId);
        TextView arrow = findViewById(arrowId);

        content.setVisibility(
                initiallyExpanded
                        ? View.VISIBLE
                        : View.GONE
        );

        arrow.setRotation(
                initiallyExpanded ? 180f : 0f
        );

        header.setOnClickListener(view -> {

            boolean isExpanded =
                    content.getVisibility() == View.VISIBLE;

            content.setVisibility(
                    isExpanded
                            ? View.GONE
                            : View.VISIBLE
            );

            arrow.animate()
                    .rotation(isExpanded ? 0f : 180f)
                    .setDuration(180)
                    .start();

            header.setContentDescription(
                    isExpanded
                            ? "Expand section"
                            : "Collapse section"
            );
        });
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
                    public void onBarcodeDetected(Result result) {

                        runOnUiThread(() -> {

                            if (isFinishing() || isDestroyed()) {
                                return;
                            }

                            isScanning = false;

                            decodeController.stopCamera();

                            decodeViewModel.onBarcodeDetected(result);
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

        // Reset section states for each new scan.

        resetExpandableSections();

        // Main result

        textDecodedFormat.setText(
                format.toString().replace("_", " ")
        );

        textDecodedContent.setText(content);

        // Barcode Information

        textDecodedType.setText(
                "Barcode type: "
                        + (result.isTwoDimensional() ? "2D" : "Linear")
        );

        int characterCount = result.getCharacterCount();

        textDecodedLength.setText(
                "Content length: "
                        + characterCount
                        + (characterCount == 1
                        ? " character"
                        : " characters")
        );

        String checkDigitStatus = result.isProductBarcode()
                ? BarcodeValidator.getCheckDigitStatus(
                content,
                format
        )
                : "Not applicable";

        textDecodedCheckDigit.setText(
                "Check digit: " + checkDigitStatus
        );

        String scanTime = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
        ).format(new Date(result.getScannedAt()));

        textDecodedTime.setText(
                "Scanned: " + scanTime
        );

        // Content Analysis

        textAnalysisCharacters.setText(
                "Characters: " + result.getCharacterCount()
        );

        textAnalysisBytes.setText(
                "UTF-8 bytes: " + result.getByteCount()
        );

        textAnalysisLetters.setText(
                "Letters: " + result.getLetterCount()
        );

        textAnalysisNumbers.setText(
                "Numbers: " + result.getNumberCount()
        );

        textAnalysisWhitespace.setText(
                "Whitespace: " + result.getWhitespaceCount()
        );

        textAnalysisSymbols.setText(
                "Symbols: " + result.getSymbolCount()
        );

        // Technical Properties

        textTechnicalFormat.setText(
                "Format: " + format.toString().replace("_", " ")
        );

        String errorCorrection = result.getErrorCorrectionLevel();

        textTechnicalErrorCorrection.setText(
                "Error correction: "
                        + (errorCorrection == null
                        ? "Not available"
                        : errorCorrection)
        );

        String symbologyIdentifier = result.getSymbologyIdentifier();

        textTechnicalSymbology.setText(
                "Symbology identifier: "
                        + (symbologyIdentifier == null
                        ? "Not available"
                        : symbologyIdentifier)
        );

        int byteSegmentCount = result.getByteSegmentCount();
        int byteSegmentSize = result.getByteSegmentSize();

        textTechnicalByteSegments.setText(
                "Byte segments: "
                        + byteSegmentCount
                        + " ("
                        + byteSegmentSize
                        + " bytes)"
        );

        if (result.hasStructuredAppend()) {

            String sequence = result.getStructuredAppendSequence();
            String parity = result.getStructuredAppendParity();

            textTechnicalStructuredAppend.setText(
                    "Structured append: Detected"
                            + "\nSequence: "
                            + (sequence == null ? "Unknown" : sequence)
                            + "\nParity: "
                            + (parity == null ? "Unknown" : parity)
            );

        } else {

            textTechnicalStructuredAppend.setText(
                    "Structured append: Not detected"
            );
        }

        // Data Interpretation

        textDataInterpretation.setText(
                "Interpretation: " + result.getInterpretation()
        );

        // Validation

        textValidationStatus.setText(
                "Validation: " + result.getValidationStatus()
        );

        // Raw Data

        textRawData.setText(content);
    }

    private void resetExpandableSections() {

        collapseSection(
                R.id.contentBarcodeInformation,
                R.id.arrowBarcodeInformation,
                true
        );

        collapseSection(
                R.id.contentContentAnalysis,
                R.id.arrowContentAnalysis,
                false
        );

        collapseSection(
                R.id.contentTechnicalProperties,
                R.id.arrowTechnicalProperties,
                false
        );

        collapseSection(
                R.id.contentDataInterpretation,
                R.id.arrowDataInterpretation,
                false
        );

        collapseSection(
                R.id.contentValidation,
                R.id.arrowValidation,
                false
        );

        collapseSection(
                R.id.contentRawData,
                R.id.arrowRawData,
                false
        );
    }

    private void collapseSection(
            int contentId,
            int arrowId,
            boolean expanded
    ) {

        View content = findViewById(contentId);
        View arrow = findViewById(arrowId);

        content.setVisibility(
                expanded ? View.VISIBLE : View.GONE
        );

        arrow.setRotation(
                expanded ? 180f : 0f
        );
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
                    public void onAdvancedSelected() {

                        startActivity(
                                new Intent(
                                        DecodeActivity.this,
                                        AdvancedActivity.class
                                )
                        );
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