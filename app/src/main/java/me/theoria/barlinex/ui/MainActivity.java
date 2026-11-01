package me.theoria.barlinex.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;

import java.io.IOException;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.controller.BarcodeInputController;
import me.theoria.barlinex.ui.controller.BarcodePreviewController;
import me.theoria.barlinex.ui.controller.NumberScannerController;
import me.theoria.barlinex.ui.helper.BarcodeImageHelper;
import me.theoria.barlinex.ui.helper.ClipboardHelper;
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class MainActivity extends AppCompatActivity {

    // Constants
    private static final String TAG = "BarLineX";

    private static final String EXTRA_BARCODE_CONTENT = "barcode_content";
    private static final String EXTRA_BARCODE_FORMAT = "barcode_format";

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

    private BarcodeViewModel barcodeViewModel;

    // Camera
    private ActivityResultLauncher<String> cameraPermissionLauncher;
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
        super.onCreate(savedInstanceState);

        registerPermissionLauncher();

        setContentView(R.layout.activity_main);

        applyWindowInsets();

        initializeViews();
        initializeViewModel();

        initializeInputController();
        initializePreviewController();

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
                findViewById(R.id.formatChips),
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

    private String getCurrentContent() {
        return inputController.getCurrentContent();
    }

    // Barcode actions
    private void setupButtons() {

        buttonGenerate.setOnClickListener(
                v -> saveBarcode()
        );

        findViewById(R.id.buttonShare).setOnClickListener(
                v -> shareBarcode()
        );

        findViewById(R.id.buttonSaveImage).setOnClickListener(
                v -> saveBarcodeImage()
        );

        findViewById(R.id.buttonCopyText).setOnClickListener(
                v -> copyBarcodeContent()
        );

        findViewById(R.id.buttonEnlarge).setOnClickListener(
                v -> openEnlargedPreview()
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

    private void saveBarcode() {

        String content = getCurrentContent();
        BarcodeFormat format = inputController.getSelectedFormat();

        String error = BarcodeInputController.validate(
                format,
                content
        );

        if (error != null) {

            inputController.setError(error);
            inputContent.requestFocus();

            return;
        }

        inputController.clearError();

        barcodeViewModel.generateBarcode(
                content,
                format,
                true
        );

        buttonGenerate.performHapticFeedback(
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? HapticFeedbackConstants.CONFIRM
                        : HapticFeedbackConstants.VIRTUAL_KEY
        );

        Snackbar.make(
                        findViewById(R.id.main),
                        "Barcode saved",
                        Snackbar.LENGTH_LONG
                )
                .setAnchorView(buttonGenerate)
                .setAction(
                        "View",
                        v -> openHistory()
                )
                .show();
    }

    private void shareBarcode() {

        String content = getCurrentContent();

        if (content.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter text to share first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent send = new Intent(Intent.ACTION_SEND);

        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, content);

        startActivity(
                Intent.createChooser(send, "Share barcode")
        );
    }

    private void copyBarcodeContent() {

        String content = getCurrentContent();

        if (content.isEmpty()) {

            Toast.makeText(
                    this,
                    "Enter text to copy first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        boolean copied = ClipboardHelper.copyText(
                this,
                content
        );

        Toast.makeText(
                this,
                copied
                        ? "Barcode content copied"
                        : "Clipboard is unavailable",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void saveBarcodeImage() {

        Bitmap bitmap = barcodeViewModel
                .getBarcodeImage()
                .getValue();

        if (bitmap == null) {

            Toast.makeText(
                    this,
                    "No barcode image available. Generate one first.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            BarcodeImageHelper.saveImage(this, bitmap);

            Toast.makeText(
                    this,
                    "PNG saved to Pictures/BarLineX",
                    Toast.LENGTH_LONG
            ).show();

        } catch (IOException | SecurityException e) {

            android.util.Log.e(
                    TAG,
                    "Failed to save barcode PNG",
                    e
            );

            Toast.makeText(
                    this,
                    "Image save failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void openEnlargedPreview() {

        String content = getCurrentContent();

        BarcodeFormat format = inputController.getSelectedFormat();

        String error = BarcodeInputController.validate(
                format,
                content
        );

        if (error != null) {

            Toast.makeText(
                    this,
                    "Enter a valid value to preview first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent = new Intent(
                this,
                BarcodePreviewActivity.class
        );

        intent.putExtra(EXTRA_BARCODE_CONTENT, content);
        intent.putExtra(EXTRA_BARCODE_FORMAT, format.name());

        startActivity(intent);
    }

    // History
    private void openHistory() {

        startActivity(
                new Intent(
                        this,
                        HistoryActivity.class
                )
        );
    }

    private void openSavedBarcode() {

        String content = getIntent().getStringExtra(
                EXTRA_BARCODE_CONTENT
        );

        String savedFormat = getIntent().getStringExtra(
                EXTRA_BARCODE_FORMAT
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

        getIntent().removeExtra(EXTRA_BARCODE_CONTENT);
        getIntent().removeExtra(EXTRA_BARCODE_FORMAT);
    }

    // Bottom navigation
    private void setupBottomNavigation() {

        bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setItemActiveIndicatorColor(
                ColorStateList.valueOf(COLOR_PRIMARY_SOFT)
        );

        bottomNavigation.setSelectedItemId(R.id.nav_create);

        bottomNavigation.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            if (itemId == R.id.nav_create) {

                exitScanMode();

                return true;
            }

            if (itemId == R.id.nav_scan) {

                enterScanMode();

                return true;
            }

            if (itemId == R.id.nav_history) {

                openHistory();

                return false;
            }

            return false;
        });
    }

    // Scanner initialization and permissions
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
                        showDetectedNumberDialog(number);
                    }

                    @Override
                    public void onScanError(String message, Exception error) {

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

    private void startNumberScan() {
        bottomNavigation.setSelectedItemId(R.id.nav_scan);
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

    // Detected number confirmation
    private void showDetectedNumberDialog(String detectedNumber) {

        EditText numberInput = new EditText(this);

        numberInput.setSingleLine(true);

        numberInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );

        numberInput.setText(detectedNumber);

        numberInput.setSelection(
                numberInput.getText().length()
        );

        numberInput.setHint("Review or correct the number");

        int horizontalPadding = (int) (
                24 * getResources().getDisplayMetrics().density
        );

        numberInput.setPadding(
                horizontalPadding,
                numberInput.getPaddingTop(),
                horizontalPadding,
                numberInput.getPaddingBottom()
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Review detected number")
                .setMessage(
                        "Check the digits before using them in your barcode."
                )
                .setView(numberInput)
                .setNegativeButton("Retake", null)
                .setPositiveButton("Use Number", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String confirmedNumber = numberInput
                                .getText()
                                .toString()
                                .trim();

                        if (confirmedNumber.isEmpty()) {

                            numberInput.setError("Enter a number first.");

                            return;
                        }

                        inputController.setContent(confirmedNumber);

                        inputContent.setSelection(
                                inputContent.getText().length()
                        );

                        inputContent.requestFocus();

                        bottomNavigation.setSelectedItemId(
                                R.id.nav_create
                        );

                        Toast.makeText(
                                MainActivity.this,
                                "Number added to barcode",
                                Toast.LENGTH_SHORT
                        ).show();

                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    // Window insets
    private void applyWindowInsets() {

        View root = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {

                    int bottomInset = windowInsets.getInsets(
                            WindowInsetsCompat.Type.navigationBars()
                    ).bottom;

                    view.setPadding(
                            view.getPaddingLeft(),
                            view.getPaddingTop(),
                            view.getPaddingRight(),
                            bottomInset
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(root);
    }
}