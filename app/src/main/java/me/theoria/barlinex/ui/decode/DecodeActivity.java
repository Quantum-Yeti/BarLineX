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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import java.text.DateFormat;
import java.util.Date;

import me.theoria.barlinex.R;
import me.theoria.barlinex.domain.barcode.BarcodeValidator;
import me.theoria.barlinex.ui.common.WindowsInsetHelper;
import me.theoria.barlinex.ui.common.ZoomControlsController;
import me.theoria.barlinex.ui.controller.BarcodeDecoderController;
import me.theoria.barlinex.ui.scanner.ScannerOverlay;

public class DecodeActivity extends AppCompatActivity {

    // Views
    private PreviewView cameraPreview;
    private View scannerPlaceholder;
    private View decodeResultPanel;
    private TextView textDecodeStatus;
    private ScannerOverlay scannerOverlay;

    // Barcode information
    private TextView textDecodedFormat;
    private TextView textDecodedContent;
    private TextView textDecodedType;
    private TextView textDecodedLength;
    private TextView textDecodedCheckDigit;
    private TextView textDecodedTime;

    // Content analysis
    private TextView textAnalysisCharacters;
    private TextView textAnalysisBytes;
    private TextView textAnalysisLetters;
    private TextView textAnalysisNumbers;
    private TextView textAnalysisWhitespace;
    private TextView textAnalysisSymbols;

    // Technical properties
    private TextView textTechnicalFormat;
    private TextView textTechnicalErrorCorrection;
    private TextView textTechnicalSymbology;
    private TextView textTechnicalByteSegments;
    private TextView textTechnicalStructuredAppend;

    // Interpretation and validation
    private TextView textDataInterpretation;
    private TextView textValidationStatus;
    private TextView textRawData;

    // Components
    private DecoderViewModel decodeViewModel;
    private BarcodeDecoderController decodeController;
    private ZoomControlsController zoomControlsController;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private boolean isScanning = false;

    // Expandable sections: header, content, arrow, initially expanded
    private final int[][] expandableSections = {
            {R.id.headerBarcodeInformation,
                    R.id.contentBarcodeInformation,
                    R.id.arrowBarcodeInformation, 1},

            {R.id.headerContentAnalysis,
                    R.id.contentContentAnalysis,
                    R.id.arrowContentAnalysis, 0},

            {R.id.headerTechnicalProperties,
                    R.id.contentTechnicalProperties,
                    R.id.arrowTechnicalProperties, 0},

            {R.id.headerDataInterpretation,
                    R.id.contentDataInterpretation,
                    R.id.arrowDataInterpretation, 0},

            {R.id.headerValidation,
                    R.id.contentValidation,
                    R.id.arrowValidation, 0},

            {R.id.headerRawData,
                    R.id.contentRawData,
                    R.id.arrowRawData, 0}
    };

    // Activity setup
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

        setupGalleryButton();
        setupResultActions();
        checkCameraPermission();
    }

    // View initialization
    private void initializeViews() {
        cameraPreview = findViewById(R.id.decodeCameraPreview);
        scannerPlaceholder = findViewById(R.id.scannerPlaceholder);
        textDecodeStatus = findViewById(R.id.textDecodeStatus);
        decodeResultPanel = findViewById(R.id.decodeResultPanel);
        scannerOverlay = findViewById(R.id.scannerOverlay);

        initializeBarcodeInformationViews();
        initializeAnalysisViews();
        initializeTechnicalViews();
        initializeInterpretationViews();
    }

    private void initializeBarcodeInformationViews() {
        textDecodedFormat = findViewById(R.id.textDecodedFormat);
        textDecodedContent = findViewById(R.id.textDecodedContent);
        textDecodedType = findViewById(R.id.textDecodedType);
        textDecodedLength = findViewById(R.id.textDecodedLength);
        textDecodedCheckDigit = findViewById(R.id.textDecodedCheckDigit);
        textDecodedTime = findViewById(R.id.textDecodedTime);
    }

    private void initializeAnalysisViews() {
        textAnalysisCharacters = findViewById(R.id.textAnalysisCharacters);
        textAnalysisBytes = findViewById(R.id.textAnalysisBytes);
        textAnalysisLetters = findViewById(R.id.textAnalysisLetters);
        textAnalysisNumbers = findViewById(R.id.textAnalysisNumbers);
        textAnalysisWhitespace = findViewById(R.id.textAnalysisWhitespace);
        textAnalysisSymbols = findViewById(R.id.textAnalysisSymbols);
    }

    private void initializeTechnicalViews() {
        textTechnicalFormat = findViewById(R.id.textTechnicalFormat);
        textTechnicalErrorCorrection = findViewById(R.id.textTechnicalErrorCorrection);
        textTechnicalSymbology = findViewById(R.id.textTechnicalSymbology);
        textTechnicalByteSegments = findViewById(R.id.textTechnicalByteSegments);
        textTechnicalStructuredAppend = findViewById(R.id.textTechnicalStructuredAppend);
    }

    private void initializeInterpretationViews() {
       textDataInterpretation = findViewById(R.id.textDataInterpretation);
        textValidationStatus = findViewById(R.id.textValidationStatus);
        textRawData = findViewById(R.id.textRawData);
    }

    // Expandable sections
    private void setupExpandableSections() {
        for (int[] section : expandableSections) {
            setupExpandableSection(
                    section[0],
                    section[1],
                    section[2],
                    section[3] == 1
            );
        }
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

        setSectionState(content, arrow, initiallyExpanded);

        header.setOnClickListener(view -> {
            boolean expanded = content.getVisibility() != View.VISIBLE;
            setSectionState(content, arrow, expanded);

            header.setContentDescription(
                    expanded ? "Collapse section" : "Expand section"
            );
        });
    }

    private void setSectionState(
            View content,
            View arrow,
            boolean expanded
    ) {
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        arrow.setRotation(expanded ? 180f : 0f);
    }

    private void resetExpandableSections() {
        for (int[] section : expandableSections) {
            collapseSection(
                    section[1],
                    section[2],
                    section[3] == 1
            );
        }
    }

    private void collapseSection(
            int contentId,
            int arrowId,
            boolean expanded
    ) {
        setSectionState(
                findViewById(contentId),
                findViewById(arrowId),
                expanded
        );
    }

    // ViewModel
    private void initializeViewModel() {
        decodeViewModel = new ViewModelProvider(this)
                .get(DecoderViewModel.class);

        decodeViewModel.getDecodedResult().observe(this, result -> {
            if (result == null) return;

            isScanning = false;
            cameraPreview.setVisibility(View.GONE);
            scannerPlaceholder.setVisibility(View.GONE);
            decodeResultPanel.setVisibility(View.VISIBLE);

            if (zoomControlsController != null) {
                zoomControlsController.hide();
            }

            textDecodeStatus.setText("DETECTED");
            displayBarcodeDetails(result);
        });
    }

    // Camera permission
    private void registerCameraPermissionLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startScanning();
                    } else {
                        textDecodeStatus.setText("PERMISSION REQUIRED");
                        showToast(
                                "Camera permission is needed to scan barcodes.",
                                Toast.LENGTH_LONG
                        );
                    }
                }
        );
    }

    private void checkCameraPermission() {
        if (hasCameraPermission()) {
            startScanning();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    // Decoder controller
    private void initializeDecoder() {
        decodeController = new BarcodeDecoderController(
                this,
                cameraPreview,
                scannerOverlay,
                new BarcodeDecoderController.Listener() {

                    @Override
                    public void onBarcodeDetected(Result result) {
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;

                            isScanning = false;
                            decodeController.stopCamera();
                            decodeViewModel.onBarcodeDetected(result);
                        });
                    }

                    @Override
                    public void onZoomCapabilitiesChanged(float maxZoomRatio) {
                        runOnUiThread(() -> {
                            if (zoomControlsController != null) {
                                zoomControlsController.onZoomCapabilitiesChanged(
                                        maxZoomRatio
                                );
                            }
                        });
                    }

                    @Override
                    public void onScanError(String message, Exception error) {
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;

                            isScanning = false;
                            textDecodeStatus.setText("CAMERA ERROR");
                            showToast(message, Toast.LENGTH_LONG);
                        });
                    }
                }
        );
    }

    // Barcode result display
    private void displayBarcodeDetails(DecodeResult result) {
        String content = result.getContent();
        BarcodeFormat format = result.getFormat();
        String formattedType = format.toString().replace("_", " ");

        resetExpandableSections();

        displayBasicInformation(result, content, format);
        displayContentAnalysis(result);
        displayTechnicalProperties(result, formattedType);
        displayInterpretation(result, content);
    }

    private void displayBasicInformation(
            DecodeResult result,
            String content,
            BarcodeFormat format
    ) {
        textDecodedFormat.setText(format.toString().replace("_", " "));
        textDecodedContent.setText(content);

        textDecodedType.setText(
                "Barcode type: " +
                        (result.isTwoDimensional() ? "2D" : "Linear")
        );

        int characterCount = result.getCharacterCount();

        textDecodedLength.setText(
                "Content length: " + characterCount +
                        (characterCount == 1 ? " character" : " characters")
        );

        String checkDigitStatus = result.isProductBarcode()
                ? BarcodeValidator.getCheckDigitStatus(content, format)
                : "Not applicable";

        textDecodedCheckDigit.setText("Check digit: " + checkDigitStatus);

        String scanTime = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
        ).format(new Date(result.getScannedAt()));

        textDecodedTime.setText("Scanned: " + scanTime);
    }

    private void displayContentAnalysis(DecodeResult result) {
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
    }

    private void displayTechnicalProperties(
            DecodeResult result,
            String formattedType
    ) {
        textTechnicalFormat.setText("Format: " + formattedType);

        String errorCorrection = result.getErrorCorrectionLevel();

        textTechnicalErrorCorrection.setText(
                "Error correction: " +
                        (errorCorrection == null
                                ? "Not available"
                                : errorCorrection)
        );

        String symbologyIdentifier = result.getSymbologyIdentifier();

        textTechnicalSymbology.setText(
                "Symbology identifier: " +
                        (symbologyIdentifier == null
                                ? "Not available"
                                : symbologyIdentifier)
        );

        textTechnicalByteSegments.setText(
                "Byte segments: " +
                        result.getByteSegmentCount() +
                        " (" +
                        result.getByteSegmentSize() +
                        " bytes)"
        );

        displayStructuredAppend(result);
    }

    private void displayStructuredAppend(DecodeResult result) {
        if (!result.hasStructuredAppend()) {
            textTechnicalStructuredAppend.setText(
                    "Structured append: Not detected"
            );
            return;
        }

        String sequence = result.getStructuredAppendSequence();
        String parity = result.getStructuredAppendParity();

        textTechnicalStructuredAppend.setText(
                "Structured append: Detected" +
                        "\nSequence: " +
                        (sequence == null ? "Unknown" : sequence) +
                        "\nParity: " +
                        (parity == null ? "Unknown" : parity)
        );
    }

    private void displayInterpretation(
            DecodeResult result,
            String content
    ) {
        textDataInterpretation.setText(
                "Interpretation: " + result.getInterpretation()
        );

        textValidationStatus.setText(
                "Validation: " + result.getValidationStatus()
        );

        textRawData.setText(content);
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
        scannerOverlay.setVisibility(View.VISIBLE);
        textDecodeStatus.setText("SCANNING");

        decodeController.startCamera();
    }

    // Gallery
    private void setupGalleryButton() {
        findViewById(R.id.buttonGallery).setOnClickListener(view ->
                showToast(
                        "Gallery decoding is coming next.",
                        Toast.LENGTH_SHORT
                )
        );
    }

    // Result actions
    private void setupResultActions() {
        findViewById(R.id.buttonCopyResult).setOnClickListener(view ->
                copyDecodedContent()
        );

        findViewById(R.id.buttonShareResult).setOnClickListener(view ->
                shareDecodedContent()
        );

        findViewById(R.id.buttonScanAgain).setOnClickListener(view ->
                scanAgain()
        );
    }

    private DecodeResult getCurrentResult() {
        return decodeViewModel.getDecodedResult().getValue();
    }

    private void copyDecodedContent() {
        DecodeResult result = getCurrentResult();
        if (result == null) return;

        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        clipboard.setPrimaryClip(
                ClipData.newPlainText(
                        "Decoded barcode",
                        result.getContent()
                )
        );

        showToast("Copied to clipboard", Toast.LENGTH_SHORT);
    }

    private void shareDecodedContent() {
        DecodeResult result = getCurrentResult();
        if (result == null) return;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, result.getContent());

        startActivity(
                Intent.createChooser(
                        shareIntent,
                        "Share decoded barcode content"
                )
        );
    }

    private void scanAgain() {
        decodeViewModel.clearResult();

        decodeResultPanel.setVisibility(View.GONE);
        cameraPreview.setVisibility(View.VISIBLE);
        scannerOverlay.setVisibility(View.VISIBLE);

        decodeController.resetScanning();
        startScanning();
    }

    // Zoom touch events
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (zoomControlsController != null) {
            zoomControlsController.onTouchEvent(event);
        }

        return super.dispatchTouchEvent(event);
    }

    // Lifecycle
    @Override
    protected void onResume() {
        super.onResume();

        if (decodeController != null && hasCameraPermission()) {
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

    // Utilities
    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }
}
