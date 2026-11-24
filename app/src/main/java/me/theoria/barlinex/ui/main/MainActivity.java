package me.theoria.barlinex.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.advanced.AdvancedActivity;
import me.theoria.barlinex.ui.common.BottomNavigationHelper;
import me.theoria.barlinex.ui.common.SplashScreenHelper;
import me.theoria.barlinex.ui.common.WindowsInsetHelper;
import me.theoria.barlinex.ui.controller.BarcodeActionController;
import me.theoria.barlinex.ui.controller.BarcodeInputController;
import me.theoria.barlinex.ui.controller.BarcodePreviewController;
import me.theoria.barlinex.ui.controller.BarcodeRestoreController;
import me.theoria.barlinex.ui.controller.QrTemplateController;
import me.theoria.barlinex.ui.history.HistoryActivity;
import me.theoria.barlinex.ui.options.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_TAB = "selected_tab";

    // Views
    private EditText inputName;
    private EditText inputContent;

    private TextInputLayout contentInputLayout;

    private MaterialButton buttonSaveBarcode;
    private MaterialButton buttonExportBarcode;

    private ImageView imagePreview;

    private View previewPlaceholder;
    private TextView textPlaceholder;

    private TextView textPreviewFormat;
    private TextView textPreviewContent;
    private TextView textPreviewInfo;

    private BottomNavigationView bottomNavigation;

    private View qrContentTypeSection;
    private MaterialAutoCompleteTextView qrContentTypeDropdown;

    // ViewModel
    private MainViewModel mainViewModel;

    // Controllers
    private BarcodeInputController inputController;
    private BarcodePreviewController previewController;
    private BarcodeActionController actionController;
    private QrTemplateController qrTemplateController;
    private BarcodeRestoreController restoreController;

    // Activity result launchers
    private ActivityResultLauncher<String> csvExportLauncher;
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
        setupSettingsNavigation();
        setupBarcodeActions();
        setupBottomNavigation();

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

        super.onDestroy();
    }

    // Initialization
    private void initializeViews() {

        inputName = findViewById(R.id.barcodeUserName);
        inputContent = findViewById(R.id.barcodeUserContent);
        contentInputLayout = findViewById(R.id.barcodeUserContentLayout);

        buttonSaveBarcode = findViewById(R.id.buttonSaveBarcodeToDB);
        buttonExportBarcode = findViewById(R.id.buttonExportBarcode);

        imagePreview = findViewById(R.id.imagePreview);

        previewPlaceholder = findViewById(R.id.previewPlaceholder);
        textPlaceholder = findViewById(R.id.textPlaceholder);

        textPreviewFormat = findViewById(R.id.textPreviewFormat);
        textPreviewContent = findViewById(R.id.textPreviewContent);
        textPreviewInfo = findViewById(R.id.textPreviewInfo);

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

        restoreController = new BarcodeRestoreController(
                mainViewModel,
                inputController,
                inputName
        );

        initializePreviewController();
        initializeActionController();
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

        /*buttonExportBarcode.setOnClickListener(
                view -> actionController.showExportMenu()
        );*/

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
    }

    // Activity result launchers
    private void initializeLaunchers() {

        registerHistoryLauncher();
        registerCsvExportLauncher();
    }

    private void registerHistoryLauncher() {

        historyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    if (result.getResultCode() != RESULT_OK
                            || result.getData() == null) {
                        return;
                    }

                    Intent data = result.getData();

                    int selectedTab = data.getIntExtra(
                            EXTRA_SELECTED_TAB,
                            R.id.nav_create
                    );

                    // Restore barcode data when returning from Edit.
                    if (data.hasExtra(
                            BarcodeActionController.EXTRA_BARCODE_CONTENT
                    )) {
                        loadSavedBarcode(data);
                    }

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

        loadSavedBarcode(getIntent());
    }

    private void loadSavedBarcode(Intent intent) {

        if (restoreController != null) {
            restoreController.restoreBarcode(intent);
        }
    }

    // Navigation
    private void setupBottomNavigation() {

        BottomNavigationHelper.setup(
                bottomNavigation,
                R.id.nav_create,
                new BottomNavigationHelper.Listener() {

                    @Override
                    public void onCreateSelected() {
                        // No additional action required.
                    }

                    @Override
                    public void onAdvancedSelected() {

                        startActivity(
                                new Intent(
                                        MainActivity.this,
                                        AdvancedActivity.class
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

    private void setupSettingsNavigation() {

        findViewById(R.id.buttonSettings).setOnClickListener(view -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    SettingsActivity.class
            );

            startActivity(intent);
        });
    }

    private void openHistory() {

        historyLauncher.launch(
                new Intent(this, HistoryActivity.class)
        );
    }
}