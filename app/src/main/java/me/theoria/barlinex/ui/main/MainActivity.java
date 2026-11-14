package me.theoria.barlinex.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.camera.view.TransformExperimental;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.common.BottomNavigationHelper;
import me.theoria.barlinex.ui.common.SplashScreenHelper;
import me.theoria.barlinex.ui.common.WindowsInsetHelper;
import me.theoria.barlinex.ui.controller.BarcodeActionController;
import me.theoria.barlinex.ui.controller.BarcodeInputController;
import me.theoria.barlinex.ui.controller.BarcodePreviewController;
import me.theoria.barlinex.ui.controller.QrTemplateController;
import me.theoria.barlinex.ui.decode.DecodeActivity;
import me.theoria.barlinex.ui.history.HistoryActivity;
import me.theoria.barlinex.ui.scanner.DetectedNumberDialog;
import me.theoria.barlinex.ui.scanner.NumberScannerController;
import me.theoria.barlinex.ui.scanner.ScannerNavigationController;
import me.theoria.barlinex.ui.scanner.ScannerOverlay;

@TransformExperimental
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BarLineX";

    public static final String EXTRA_SELECTED_TAB = "selected_tab";

    // Views
    private EditText inputName;
    private EditText inputContent;

    private TextInputLayout contentInputLayout;

    private MaterialButton buttonSaveBarcode;
    private MaterialButton buttonExportBarcode;
    private MaterialButton buttonCapture;

    private ImageView imagePreview;

    private View previewPlaceholder;
    private TextView textPlaceholder;

    private TextView textPreviewFormat;
    private TextView textPreviewContent;
    private TextView textPreviewInfo;

    private BottomNavigationView bottomNavigation;

    private View qrContentTypeSection;
    private MaterialAutoCompleteTextView qrContentTypeDropdown;

    // Scanner views
    private View createContent;
    private View scanOverlay;
    private ScannerOverlay numberScannerOverlay;
    private PreviewView cameraPreview;

    // ViewModel
    private MainViewModel mainViewModel;

    // Controllers
    private BarcodeInputController inputController;
    private BarcodePreviewController previewController;
    private BarcodeActionController actionController;

    private NumberScannerController scannerController;
    private DetectedNumberDialog detectedNumberDialog;
    private ScannerNavigationController scannerNavigationController;

    private QrTemplateController qrTemplateController;

    // Activity result launchers
    private ActivityResultLauncher<String> csvExportLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> historyLauncher;

    // Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SplashScreenHelper.install(this);

        setContentView(R.layout.activity_main);

        // Initialize views and dependencies
        initializeViews();
        initializeWindowInsets();
        initializeViewModel();
        initializeLaunchers();
        initializeControllers();

        // Set up input and barcode functionality
        setupInputHandling();
        setupBarcodeActions();

        // Initialize scanner before navigation because
        // navigation uses ScannerNavigationController
        setupScanner();
        setupNavigation();

        // Initialize the barcode preview
        previewController.updatePreviewLabels();

        // Restore barcode data passed through the Intent
        loadSavedBarcode();
    }

    @Override
    protected void onDestroy() {
        if (inputController != null) {
            inputController.cleanup();
        }

        if (scannerController != null) {
            scannerController.close();
        }

        super.onDestroy();
    }

    // Initialization
    private void initializeViews() {
        inputName = findViewById(R.id.barcodeUserName);
        inputContent = findViewById(R.id.barcodeUserContent);
        contentInputLayout = findViewById(R.id.barcodeUserContentLayout);

        buttonSaveBarcode = findViewById(R.id.buttonSaveBarcodeToDB);
        buttonExportBarcode = findViewById(R.id.buttonExportBarcode);
        buttonCapture = findViewById(R.id.buttonCapture);

        imagePreview = findViewById(R.id.imagePreview);

        previewPlaceholder = findViewById(R.id.previewPlaceholder);
        textPlaceholder = findViewById(R.id.textPlaceholder);

        textPreviewFormat = findViewById(R.id.textPreviewFormat);
        textPreviewContent = findViewById(R.id.textPreviewContent);
        textPreviewInfo = findViewById(R.id.textPreviewInfo);

        createContent = findViewById(R.id.createContent);
        scanOverlay = findViewById(R.id.scanOverlay);
        numberScannerOverlay = findViewById(R.id.numberScannerOverlay);
        cameraPreview = findViewById(R.id.cameraPreview);

        qrContentTypeSection = findViewById(R.id.qrContentTypeSection);
        qrContentTypeDropdown = findViewById(R.id.qrContentTypeDropdown);

        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void initializeWindowInsets() {
        WindowsInsetHelper.applySystemBarInsets(
                findViewById(R.id.main)
        );
    }

    private void initializeViewModel() {
        mainViewModel = new ViewModelProvider(this)
                .get(MainViewModel.class);
    }

    private void initializeControllers() {
        initializeInputController();

        qrTemplateController = new QrTemplateController(
                qrContentTypeSection,
                qrContentTypeDropdown,
                findViewById(R.id.barcodeContentLabel),
                contentInputLayout,
                inputController
        );

        initializePreviewController();
        initializeActionController();

        detectedNumberDialog = new DetectedNumberDialog(this);
    }

    private void initializeInputController() {
        inputController = new BarcodeInputController(
                mainViewModel,
                inputContent,
                contentInputLayout,
                findViewById(R.id.barcodeFormatDropdown),
                findViewById(R.id.textFormatCount),
                qrContentTypeSection,
                qrContentTypeDropdown
        );

        inputController.setOnBarcodeFormatChangedListener(
                format -> inputController.refreshPreviewNow()
        );
    }

    private void initializePreviewController() {
        previewController = new BarcodePreviewController(
                mainViewModel,
                inputController,
                imagePreview,
                previewPlaceholder,
                textPlaceholder,
                textPreviewFormat,
                textPreviewContent,
                textPreviewInfo
        );

        previewController.observeBarcode(this);
    }

    private void initializeActionController() {
        actionController = new BarcodeActionController(
                this,
                mainViewModel,
                buttonSaveBarcode,
                this::openHistory,
                csvExportLauncher
        );
    }

    // Input handling
    private void setupInputHandling() {
        inputController.setupTextWatchers(
                inputName,
                previewController
        );
    }

    // Barcode actions
    private void setupBarcodeActions() {
        buttonSaveBarcode.setOnClickListener(
                view -> actionController.saveBarcode()
        );

        buttonExportBarcode.setOnClickListener(
                view -> actionController.showExportMenu()
        );

        findViewById(R.id.buttonCustomizeBarcode).setOnClickListener(
                view -> actionController.customizeBarcode()
        );

        findViewById(R.id.buttonShare).setOnClickListener(
                view -> actionController.shareBarcodeImage()
        );

        findViewById(R.id.buttonPrintText).setOnClickListener(
                view -> actionController.printBarcodeImage()
        );

        findViewById(R.id.buttonEnlarge).setOnClickListener(
                view -> actionController.openEnlargedPreview()
        );

        contentInputLayout.setEndIconOnClickListener(
                view -> scannerNavigationController.requestScan()
        );
    }

    // Activity result launchers
    private void initializeLaunchers() {
        registerCameraPermissionLauncher();
        registerHistoryLauncher();
        registerCsvExportLauncher();
    }

    private void registerCameraPermissionLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (scannerNavigationController != null) {
                        scannerNavigationController.onPermissionResult(granted);
                    }
                }
        );
    }

    private void registerHistoryLauncher() {
        historyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK
                            || result.getData() == null) {
                        return;
                    }

                    int selectedTab = result.getData().getIntExtra(
                            EXTRA_SELECTED_TAB,
                            R.id.nav_create
                    );

                    bottomNavigation.setSelectedItemId(selectedTab);
                }
        );
    }

    private void registerCsvExportLauncher() {
        csvExportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/csv"),
                uri -> {
                    if (uri != null && actionController != null) {
                        actionController.writeBarcodeCsv(uri);
                    }
                }
        );
    }

    // Saved barcode handling
    private void loadSavedBarcode() {
        Intent intent = getIntent();

        String content = intent.getStringExtra(
                BarcodeActionController.EXTRA_BARCODE_CONTENT
        );

        String savedFormat = intent.getStringExtra(
                BarcodeActionController.EXTRA_BARCODE_FORMAT
        );

        if (content == null || savedFormat == null) {
            return;
        }

        BarcodeFormat format = parseBarcodeFormat(savedFormat);

        inputController.setPreviewUpdatesSuppressed(true);

        try {
            inputController.selectBarcodeFormat(format);

            BarcodeFormat selectedFormat =
                    mainViewModel.getSelectedBarcodeFormat().getValue();

            if (selectedFormat == null) {
                inputController.selectBarcodeFormat(
                        BarcodeFormat.QR_CODE
                );
            }

            inputController.setBarcodeContent(content);

        } finally {
            inputController.setPreviewUpdatesSuppressed(false);
        }

        inputController.refreshPreviewNow();

        intent.removeExtra(
                BarcodeActionController.EXTRA_BARCODE_CONTENT
        );

        intent.removeExtra(
                BarcodeActionController.EXTRA_BARCODE_FORMAT
        );
    }

    private BarcodeFormat parseBarcodeFormat(String savedFormat) {
        try {

            return BarcodeFormat.valueOf(savedFormat);

        } catch (IllegalArgumentException exception) {
            return BarcodeFormat.QR_CODE;
        }
    }

    // Navigation
    private void setupNavigation() {
        BottomNavigationHelper.setup(
                bottomNavigation,
                R.id.nav_create,
                new BottomNavigationHelper.Listener() {

                    @Override
                    public void onCreateSelected() {
                        scannerNavigationController.exitScanMode();
                    }

                    @Override
                    public void onDecodeSelected() {
                        scannerNavigationController.exitScanMode();

                        startActivity(
                                new Intent(
                                        MainActivity.this,
                                        DecodeActivity.class
                                )
                        );
                    }

                    @Override
                    public void onHistorySelected() {
                        openHistory();
                    }
                }
        );
    }

    private void openHistory() {
        historyLauncher.launch(
                new Intent(this, HistoryActivity.class)
        );
    }

    // Scanner initialization
    @OptIn(markerClass = TransformExperimental.class)
    private void setupScanner() {

        scannerController = new NumberScannerController(
                this,
                cameraPreview,
                numberScannerOverlay,
                buttonCapture,
                new NumberScannerController.Listener() {

                    @Override
                    public void onTextDetected(String text) {
                        showDetectedNumber(text);
                    }

                    @Override
                    public void onScanError(
                            String message,
                            Exception error
                    ) {
                        handleScannerError(message, error);
                    }
                }
        );

        scannerNavigationController = new ScannerNavigationController(
                this,
                cameraPermissionLauncher,
                scannerController,
                createContent,
                scanOverlay,
                numberScannerOverlay,
                buttonSaveBarcode,
                buttonExportBarcode,
                bottomNavigation
        );

        buttonCapture.setOnClickListener(
                view -> scannerController.confirmCurrentText()
        );
    }

    // Detected text

    private void showDetectedNumber(String text) {
        detectedNumberDialog.show(
                text,
                new DetectedNumberDialog.Listener() {

                    @Override
                    public void onTextConfirmed(String confirmedText) {
                        handleConfirmedText(confirmedText);
                    }

                    @Override
                    public void onDialogDismissed() {
                        if (scannerController != null) {
                            scannerController.resumeScanning();
                        }
                    }
                }
        );
    }

    private void handleConfirmedText(String confirmedText) {
        inputController.setBarcodeContent(confirmedText);

        inputContent.setSelection(
                inputContent.getText().length()
        );

        inputContent.requestFocus();

        bottomNavigation.setSelectedItemId(R.id.nav_create);

        Toast.makeText(
                this,
                "Text added to barcode",
                Toast.LENGTH_SHORT
        ).show();
    }

    // Scanner errors

    private void handleScannerError(
            String message,
            Exception error
    ) {
        if (error != null) {
            Log.e(TAG, message, error);
        }

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
