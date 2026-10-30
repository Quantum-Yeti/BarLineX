package me.theoria.barly.ui;

import android.content.ContentValues;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;

import java.io.IOException;

import me.theoria.barly.R;
import me.theoria.barly.ui.helper.BarcodeImageHelper;
import me.theoria.barly.ui.helper.ClipboardHelper;
import me.theoria.barly.ui.scanner.NumberScanner;
import me.theoria.barly.ui.viewmodel.BarcodeViewModel;

public class MainActivity extends AppCompatActivity {

    // Input controls
    private EditText inputContent;
    private RadioGroup formatGroup;
    private Button buttonGenerate;

    // Preview views
    private ImageView imagePreview;
    private View previewPlaceholder;
    private TextView textStatus;
    private TextView textPreviewFormat;
    private TextView textPreviewContent;
    private TextView textPreviewInfo;
    private TextInputLayout contentInputLayout;

    // ViewModel
    private BarcodeViewModel barcodeViewModel;

    // Prevents listeners from reacting while loading saved content.
    private boolean isOpeningSavedBarcode = false;

    // Camera capture target used by the number scanner.
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri scanImageUri;

    // Number scanner field
    private final NumberScanner numberScanner = new NumberScanner();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && scanImageUri != null) {
                        processScannedImage(scanImageUri);
                    } else {
                        deleteScanImage();
                    }
                }
        );

        setContentView(R.layout.activity_main);

        applyWindowInsets();

        initializeViews();
        initializeViewModel();
        observeBarcode();
        setupInputListeners();
        setupButtons();

        updatePreviewLabels();
        openSavedBarcode();
    }

    // Initialization
    private void initializeViews() {
        inputContent = findViewById(R.id.inputContent);
        formatGroup = findViewById(R.id.formatGroup);
        buttonGenerate = findViewById(R.id.buttonGenerate);

        imagePreview = findViewById(R.id.imagePreview);
        previewPlaceholder = findViewById(R.id.previewPlaceholder);

        textPreviewFormat = findViewById(R.id.textPreviewFormat);
        textPreviewContent = findViewById(R.id.textPreviewContent);
        textPreviewInfo = findViewById(R.id.textPreviewInfo);
        contentInputLayout = findViewById(R.id.contentInputLayout);
    }

    private void initializeViewModel() {
        barcodeViewModel = new ViewModelProvider(this)
                .get(BarcodeViewModel.class);
    }

    private void observeBarcode() {

        // Show or hide the generated barcode image.
        barcodeViewModel.getBarcodeImage().observe(this, bitmap -> {
            if (bitmap == null) {
                imagePreview.setImageDrawable(null);
                imagePreview.setVisibility(View.GONE);
                previewPlaceholder.setVisibility(View.VISIBLE);
            } else {
                imagePreview.setImageBitmap(bitmap);
                imagePreview.setVisibility(View.VISIBLE);
                previewPlaceholder.setVisibility(View.GONE);
            }
        });

        // Only display validation errors here.
        barcodeViewModel.getStatus().observe(this, message -> {
            if ("Enter some text first".equals(message)) {
                inputContent.setError(message);
            } else {
                inputContent.setError(null);
            }
        });
    }

    // Input and live preview
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
                if (!isOpeningSavedBarcode) {
                    updateLivePreview();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No action needed.
            }
        });

        formatGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!isOpeningSavedBarcode) {
                updateLivePreview();
            }
        });
    }

    private void updateLivePreview() {
        updatePreviewLabels();

        String content = getCurrentContent();

        if (content.isEmpty()) {
            // Clear the previous barcode when the input is empty.
            imagePreview.setImageDrawable(null);
            imagePreview.setVisibility(View.GONE);
            previewPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        // Generate a preview without saving it to history.
        barcodeViewModel.generateBarcode(
                content,
                getSelectedFormat(),
                false
        );
    }

    private void updatePreviewLabels() {
        String content = getCurrentContent();
        BarcodeFormat format = getSelectedFormat();

        boolean isCode128 = format == BarcodeFormat.CODE_128;

        textPreviewFormat.setText(
                isCode128 ? "CODE 128" : "QR CODE"
        );

        textPreviewContent.setText(
                content.isEmpty() ? "Enter text below" : content
        );

        textPreviewInfo.setText(
                (isCode128 ? "Code 128" : "QR Code")
                        + "  •  "
                        + content.length()
                        + (content.length() == 1
                        ? " character"
                        : " characters")
        );
    }

    // Button setup
    private void setupButtons() {
        // Save the barcode to history.
        buttonGenerate.setText("Save Barcode");
        buttonGenerate.setOnClickListener(v -> saveBarcode());

        // Open barcode history.
        ImageButton buttonHistory = findViewById(R.id.buttonHistory);
        buttonHistory.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    HistoryActivity.class
            );
            startActivity(intent);
        });

        // Copy the barcode's underlying text.
        ImageButton buttonCopyText = findViewById(R.id.buttonCopyText);
        buttonCopyText.setOnClickListener(v -> copyBarcodeContent());

        // Copy icon beside the Barcode Content row.
        ImageButton buttonCopyContent =
                findViewById(R.id.buttonCopyContent);
        buttonCopyContent.setOnClickListener(
                v -> copyBarcodeContent()
        );

        // Save the generated barcode as a PNG image.
        View buttonSaveImage = findViewById(R.id.buttonSaveImage);
        buttonSaveImage.setOnClickListener(v -> saveBarcodeImage());

        // Open the enlarged barcode screen.
        View buttonEnlarge = findViewById(R.id.buttonEnlarge);
        buttonEnlarge.setOnClickListener(v -> openEnlargedPreview());

        // Scan a number
        contentInputLayout.setEndIconOnClickListener(v -> startNumberScan());

        // Share is the next action to implement.
    }

    // Save barcode to history
    private void saveBarcode() {
        String content = getCurrentContent();

        if (content.isEmpty()) {
            inputContent.setError("Enter some text first");
            inputContent.requestFocus();
            return;
        }

        barcodeViewModel.generateBarcode(
                content,
                getSelectedFormat(),
                true
        );

        Toast.makeText(
                this,
                "Barcode Saved Successfully",
                Toast.LENGTH_SHORT
        ).show();
    }

    // Copy barcode content
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

        boolean copied = ClipboardHelper.copyText(this, content);

        if (copied) {
            Toast.makeText(
                    this,
                    "Barcode content copied",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(
                    this,
                    "Clipboard is unavailable",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Save barcode image to gallery
    private void saveBarcodeImage() {

        Bitmap bitmap = barcodeViewModel.getBarcodeImage().getValue();

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
                    "BarLineX",
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


    // Enlarge barcode
    private void openEnlargedPreview() {
        String content = getCurrentContent();

        if (content.isEmpty()) {
            Toast.makeText(
                    this,
                    "Enter text to preview first",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Intent intent = new Intent(
                MainActivity.this,
                BarcodePreviewActivity.class
        );

        intent.putExtra("barcode_content", content);
        intent.putExtra(
                "barcode_format",
                getSelectedFormat().name()
        );

        startActivity(intent);
    }

    // Load a barcode selected from history
    private void openSavedBarcode() {
        String content = getIntent().getStringExtra("barcode_content");
        String savedFormat = getIntent().getStringExtra("barcode_format");

        if (content == null || savedFormat == null) {
            return;
        }

        BarcodeFormat format = "CODE_128".equals(savedFormat)
                ? BarcodeFormat.CODE_128
                : BarcodeFormat.QR_CODE;

        isOpeningSavedBarcode = true;

        try {
            inputContent.setText(content);

            formatGroup.check(
                    format == BarcodeFormat.CODE_128
                            ? R.id.radioCode128
                            : R.id.radioQr
            );
        } finally {
            isOpeningSavedBarcode = false;
        }

        updatePreviewLabels();

        // Display the saved barcode without adding a duplicate history entry.
        barcodeViewModel.generateBarcode(content, format, false);

        // Remove the extras so they are not reprocessed on recreation.
        getIntent().removeExtra("barcode_content");
        getIntent().removeExtra("barcode_format");
    }

    // Shared helpers
    private String getCurrentContent() {
        return inputContent.getText().toString().trim();
    }

    private BarcodeFormat getSelectedFormat() {
        int selectedId = formatGroup.getCheckedRadioButtonId();

        return selectedId == R.id.radioCode128
                ? BarcodeFormat.CODE_128
                : BarcodeFormat.QR_CODE;
    }

    private void startNumberScan() {
        ContentValues values = new ContentValues();
        values.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "Barly_scan_" + System.currentTimeMillis() + ".jpg"
        );
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/Barly/Scans"
            );
        }

        scanImageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (scanImageUri == null) {
            Toast.makeText(
                    this,
                    "Could not prepare the camera. Please try again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        try {
            cameraLauncher.launch(scanImageUri);
        } catch (Exception e) {
            deleteScanImage();
            Toast.makeText(
                    this,
                    "Could not open the camera app.",
                    Toast.LENGTH_LONG
            ).show();
            android.util.Log.e("Barly", "Could not launch number scanner", e);
        }
    }


    private void processScannedImage(Uri imageUri) {

        numberScanner.scan(this, imageUri, new NumberScanner.Listener() {

            @Override
            public void onScanResult(String number) {

                if (number.isEmpty()) {
                    Toast.makeText(
                            MainActivity.this,
                            "No numbers detected. Try again with better lighting and a clearer photo.",
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    showDetectedNumberDialog(number);
                }
            }

            @Override
            public void onScanError(Exception error) {

                android.util.Log.e(
                        "BarLineX",
                        "Number recognition failed",
                        error
                );

                Toast.makeText(
                        MainActivity.this,
                        "Could not read the number. Please try another photo.",
                        Toast.LENGTH_LONG
                ).show();
            }

            @Override
            public void onScanComplete() {

                getContentResolver().delete(imageUri, null, null);

                if (imageUri.equals(scanImageUri)) {
                    scanImageUri = null;
                }
            }
        });
    }

    private void showDetectedNumberDialog(String detectedNumber) {
        EditText numberInput = new EditText(this);
        numberInput.setSingleLine(true);
        numberInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        numberInput.setText(detectedNumber);
        numberInput.setSelection(numberInput.getText().length());
        numberInput.setHint("Review or correct the number");

        int horizontalPadding = (int) (24 * getResources().getDisplayMetrics().density);
        numberInput.setPadding(
                horizontalPadding,
                numberInput.getPaddingTop(),
                horizontalPadding,
                numberInput.getPaddingBottom()
        );

        new AlertDialog.Builder(this)
                .setTitle("Review detected number")
                .setMessage("Check the digits before using them in your barcode.")
                .setView(numberInput)
                .setNegativeButton("Retake", (dialog, which) -> startNumberScan())
                .setPositiveButton("Use Number", (dialog, which) -> {
                    String confirmedNumber = numberInput.getText().toString().trim();

                    if (confirmedNumber.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Enter a number first.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    inputContent.setText(confirmedNumber);
                    inputContent.setSelection(inputContent.getText().length());
                    inputContent.requestFocus();

                    Toast.makeText(
                            this,
                            "Number added to barcode",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .show();
    }

    private void deleteScanImage() {
        if (scanImageUri != null) {
            getContentResolver().delete(scanImageUri, null, null);
            scanImageUri = null;
        }
    }

    private void applyWindowInsets() {

        View root = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {

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
        });

        ViewCompat.requestApplyInsets(root);
    }
}
