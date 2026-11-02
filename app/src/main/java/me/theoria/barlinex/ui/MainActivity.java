package me.theoria.barlinex.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.splashscreen.SplashScreen;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.controller.BarcodeActionController;
import me.theoria.barlinex.ui.controller.BarcodeInputController;
import me.theoria.barlinex.ui.controller.BarcodePreviewController;
import me.theoria.barlinex.ui.controller.DetectedNumberDialogController;
import me.theoria.barlinex.ui.controller.NumberScannerController;
import me.theoria.barlinex.ui.helper.BottomNavigationHelper;
import me.theoria.barlinex.ui.helper.WindowsInsetHelper;
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class MainActivity extends AppCompatActivity {

    // Constants
    private static final String TAG = "BarLineX";

    public static final String EXTRA_SELECTED_TAB = "selected_tab";

    private static final long PREVIEW_DEBOUNCE_MS = 150L;

    private static final int COLOR_PRIMARY_SOFT = 0x268B9EFF;

    // Views
    private EditText inputContent;
    private TextInputLayout contentInputLayout;

    private MaterialButton buttonGenerate;

    private ImageView imagePreview;
    private View previewPlaceholder;
    private TextView textPlaceholder;

    private TextView textPreviewFormat;
    private TextView textPreviewContent;
    private TextView textPreviewInfo;

    private BottomNavigationView bottomNavigation;

    private View createContent;
    private View scanOverlay;

    private PreviewView cameraPreview;
    private MaterialButton buttonCapture;

    // Controllers and ViewModel
    private BarcodeInputController inputController;
    private BarcodePreviewController previewController;
    private BarcodeActionController actionController;
    private DetectedNumberDialogController detectedNumberDialogController;

    private BarcodeViewModel barcodeViewModel;

    // Activity Result launchers
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> historyLauncher;

    // Scanner
    private NumberScannerController scannerController;

    // State
    private boolean isOpeningSavedBarcode = false;

    private final Handler uiHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable previewRunnable = () -> {
        if (previewController != null) {
            previewController.updateLivePreview();
        }
    };

    private final OnBackPressedCallback scanBackCallback =
            new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    bottomNavigation.setSelectedItemId(R.id.nav_create);
                }
            };

    // Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashscreen = SplashScreen.installSplashScreen(this);

        long splashScreenStartTime = SystemClock.elapsedRealtime();

        splashscreen.setKeepOnScreenCondition(() ->
            SystemClock.elapsedRealtime() - splashScreenStartTime < 2000
        );

        super.onCreate(savedInstanceState);

        registerPermissionLauncher();
        registerHistoryLauncher();

        setContentView(R.layout.activity_main);

        WindowsInsetHelper.applySystemBarInsets(
                findViewById(R.id.main)
        );

        initializeViews();
        initializeViewModel();

        initializeInputController();
        initializePreviewController();
        initializeActionController();
        initializeDetectedNumberDialogController();

        setupInputListeners();
        setupButtons();
        setupBottomNavigation();
        setupScanner();

        previewController.updatePreviewLabels();

        openSavedBarcode();
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(previewRunnable);

        if (scannerController != null) {
            scannerController.close();
        }

        super.onDestroy();
    }

    // Initialization
    private void initializeViews() {
        inputContent = findViewById(R.id.inputContent);
        contentInputLayout = findViewById(R.id.contentInputLayout);

        buttonGenerate = findViewById(R.id.buttonGenerate);

        imagePreview = findViewById(R.id.imagePreview);
        previewPlaceholder = findViewById(R.id.previewPlaceholder);
        textPlaceholder = findViewById(R.id.textPlaceholder);

        textPreviewFormat = findViewById(R.id.textPreviewFormat);
        textPreviewContent = findViewById(R.id.textPreviewContent);
        textPreviewInfo = findViewById(R.id.textPreviewInfo);

        createContent = findViewById(R.id.createContent);
        scanOverlay = findViewById(R.id.scanOverlay);

        cameraPreview = findViewById(R.id.cameraPreview);
        buttonCapture = findViewById(R.id.buttonCapture);
    }

    private void initializeViewModel() {
        barcodeViewModel = new ViewModelProvider(this)
                .get(BarcodeViewModel.class);
    }

    private void initializeInputController() {
        inputController = new BarcodeInputController(
                inputContent,
                contentInputLayout,
                findViewById(R.id.formatDropdown),
                findViewById(R.id.textFormatCount)
        );

        inputController.setOnFormatChangedListener(format -> {
            if (previewController == null) {
                return;
            }

            uiHandler.removeCallbacks(previewRunnable);
            previewController.updateLivePreview();
        });
    }

    private void initializePreviewController() {
        previewController = new BarcodePreviewController(
                barcodeViewModel,
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
                inputController,
                barcodeViewModel,
                buttonGenerate
        );
    }

    private void initializeDetectedNumberDialogController() {
        detectedNumberDialogController =
                new DetectedNumberDialogController(this);
    }

    // Input
    private void setupInputListeners() {
        inputContent.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
                // No action needed.
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                if (isOpeningSavedBarcode) {
                    return;
                }

                previewController.updatePreviewLabels();

                uiHandler.removeCallbacks(previewRunnable);

                uiHandler.postDelayed(
                        previewRunnable,
                        PREVIEW_DEBOUNCE_MS
                );
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed.
            }
        });
    }

    // Barcode actions
    private void setupButtons() {
        buttonGenerate.setOnClickListener(
                v -> actionController.saveBarcode()
        );

        findViewById(R.id.buttonShare).setOnClickListener(
                v -> actionController.shareBarcode()
        );

        findViewById(R.id.buttonSaveImage).setOnClickListener(
                v -> actionController.saveBarcodeImage()
        );

        findViewById(R.id.buttonCopyText).setOnClickListener(
                v -> actionController.copyBarcodeContent()
        );

        findViewById(R.id.buttonEnlarge).setOnClickListener(
                v -> actionController.openEnlargedPreview()
        );

        findViewById(R.id.buttonAppearance).setOnClickListener(
                v -> Snackbar.make(
                        findViewById(R.id.main),
                        "Appearance options are coming soon",
                        Snackbar.LENGTH_SHORT
                ).setAnchorView(buttonGenerate).show()
        );

        contentInputLayout.setEndIconOnClickListener(
                v -> startNumberScan()
        );
    }

    // History navigation
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

    private void openHistory() {
        historyLauncher.launch(
                new Intent(this, HistoryActivity.class)
        );
    }

    // Saved barcode
    private void openSavedBarcode() {
        String content = getIntent().getStringExtra(
                BarcodeActionController.EXTRA_BARCODE_CONTENT
        );

        String savedFormat = getIntent().getStringExtra(
                BarcodeActionController.EXTRA_BARCODE_FORMAT
        );

        if (content == null || savedFormat == null) {
            return;
        }

        BarcodeFormat format;

        try {
            format = BarcodeFormat.valueOf(savedFormat);
        } catch (IllegalArgumentException e) {
            format = BarcodeFormat.QR_CODE;
        }

        isOpeningSavedBarcode = true;

        try {
            inputController.selectFormat(format);
            inputController.setContent(content);
        } finally {
            isOpeningSavedBarcode = false;
        }

        previewController.updatePreviewLabels();

        barcodeViewModel.generateBarcode(
                content,
                format,
                false
        );

        getIntent().removeExtra(
                BarcodeActionController.EXTRA_BARCODE_CONTENT
        );

        getIntent().removeExtra(
                BarcodeActionController.EXTRA_BARCODE_FORMAT
        );
    }

    // Bottom navigation
    private void setupBottomNavigation() {

        bottomNavigation = findViewById(R.id.bottomNavigation);

        BottomNavigationHelper.setup(
                bottomNavigation,
                R.id.nav_create,
                new BottomNavigationHelper.Listener() {

                    @Override
                    public void onCreateSelected() {
                        exitScanMode();
                    }

                    @Override
                    public void onDecodeSelected() {
                        exitScanMode();

                        startActivity(
                                new Intent(MainActivity.this, DecodeActivity.class)
                        );
                    }

                    @Override
                    public void onHistorySelected() {
                        openHistory();
                    }
                }
        );
    }

    // Detected number
    private void handleConfirmedNumber(String confirmedNumber) {
        inputController.setContent(confirmedNumber);

        inputContent.setSelection(
                inputContent.getText().length()
        );

        inputContent.requestFocus();

        bottomNavigation.setSelectedItemId(R.id.nav_create);

        Toast.makeText(
                this,
                "Number added to barcode",
                Toast.LENGTH_SHORT
        ).show();
    }

    // Camera permissions
    private void registerPermissionLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        showScanner();
                    } else {
                        Toast.makeText(
                                this,
                                "Camera permission is needed to scan numbers.",
                                Toast.LENGTH_LONG
                        ).show();

                        bottomNavigation.setSelectedItemId(
                                R.id.nav_create
                        );
                    }
                }
        );
    }

    // Scanner initialization
    private void setupScanner() {
        getOnBackPressedDispatcher().addCallback(
                this,
                scanBackCallback
        );

        scannerController = new NumberScannerController(
                this,
                cameraPreview,
                buttonCapture,
                new NumberScannerController.Listener() {

                    @Override
                    public void onNumberDetected(String number) {
                        detectedNumberDialogController.show(
                                number,
                                MainActivity.this::handleConfirmedNumber
                        );
                    }

                    @Override
                    public void onScanError(
                            String message,
                            Exception error
                    ) {
                        if (error != null) {
                            android.util.Log.e(TAG, message, error);
                        }

                        Toast.makeText(
                                MainActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );

        buttonCapture.setOnClickListener(
                v -> scannerController.capturePhoto()
        );
    }

    // Scanner navigation
    private void startNumberScan() {
        enterScanMode();
    }

    private void enterScanMode() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            showScanner();

        } else {
            cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }

    // Scanner visibility and camera lifecycle
    private void showScanner() {
        View focused = getCurrentFocus();

        if (focused != null) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(
                        focused.getWindowToken(),
                        0
                );
            }
        }

        createContent.setVisibility(View.GONE);
        buttonGenerate.setVisibility(View.GONE);
        scanOverlay.setVisibility(View.VISIBLE);

        scanBackCallback.setEnabled(true);

        scannerController.startCamera();
    }

    private void exitScanMode() {
        scanBackCallback.setEnabled(false);

        if (scannerController != null) {
            scannerController.stopCamera();
        }

        scanOverlay.setVisibility(View.GONE);
        createContent.setVisibility(View.VISIBLE);
        buttonGenerate.setVisibility(View.VISIBLE);
    }
}