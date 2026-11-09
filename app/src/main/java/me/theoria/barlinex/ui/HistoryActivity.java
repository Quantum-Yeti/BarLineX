package me.theoria.barlinex.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import me.theoria.barlinex.R;
import me.theoria.barlinex.data.BarcodeEntity;
import me.theoria.barlinex.ui.adapter.BarcodeHistoryAdapter;
import me.theoria.barlinex.ui.helper.BottomNavigationHelper;
import me.theoria.barlinex.ui.helper.WindowsInsetHelper;
import me.theoria.barlinex.ui.viewmodel.HistoryViewModel;
import me.theoria.barlinex.utils.BarcodeGenerator;

public class HistoryActivity extends AppCompatActivity {

    // Views
    private BarcodeHistoryAdapter adapter;
    private RecyclerView recyclerHistory;
    private View historyEmptyContainer;

    private TextView textHistoryCount;
    private TextView textHistoryEmpty;

    private TextInputEditText editHistorySearch;
    private BottomNavigationView bottomNavigationView;

    // ViewModel
    private HistoryViewModel historyViewModel;

    // Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        WindowsInsetHelper.applySystemBarInsets(
                findViewById(R.id.historyRoot)
        );

        initializeViews();
        initializeViewModel();
        setupRecyclerView();
        setupSearch();
        observeBarcodes();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (historyViewModel != null) {
            historyViewModel.loadBarcodes();
        }
    }

    // Initialization
    private void initializeViews() {
        recyclerHistory = findViewById(R.id.recyclerHistory);
        historyEmptyContainer = findViewById(R.id.historyEmptyContainer);

        textHistoryCount = findViewById(R.id.textHistoryCount);
        textHistoryEmpty = findViewById(R.id.textHistoryEmpty);

        editHistorySearch = findViewById(R.id.editHistorySearch);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void initializeViewModel() {
        historyViewModel = new ViewModelProvider(this)
                .get(HistoryViewModel.class);

        adapter = new BarcodeHistoryAdapter(
                this::openBarcode,
                this::deleteBarcode,
                this::toggleFavorite,
                this::renameBarcode
        );
    }

    private void setupRecyclerView() {
        recyclerHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerHistory.setAdapter(adapter);
    }

    // Search
    private void setupSearch() {
        editHistorySearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {
                filterHistory(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterHistory(String query) {
        adapter.filter(query);
        updateHistoryDisplay();
    }

    // History
    private void observeBarcodes() {
        historyViewModel.getBarcodes().observe(this, barcodes -> {

            adapter.setBarcodes(barcodes);

            String query = getSearchQuery();
            adapter.filter(query);

            int count = barcodes == null ? 0 : barcodes.size();
            textHistoryCount.setText(count + " saved");

            updateHistoryDisplay();
        });
    }

    private String getSearchQuery() {
        return editHistorySearch.getText() == null
                ? ""
                : editHistorySearch.getText().toString().trim();
    }

    private void updateHistoryDisplay() {
        boolean hasResults = adapter.getItemCount() > 0;
        boolean hasSearchQuery = !getSearchQuery().isEmpty();

        historyEmptyContainer.setVisibility(
                hasResults ? View.GONE : View.VISIBLE
        );

        recyclerHistory.setVisibility(
                hasResults ? View.VISIBLE : View.GONE
        );

        if (!hasResults) {
            textHistoryEmpty.setText(
                    hasSearchQuery
                            ? "No barcodes match your search. Try a different barcodeName, value, or barcodeFormat."
                            : "Barcodes you save will appear here. Create your first barcode to get started."
            );
        }
    }

    // Preview
    private void openBarcode(BarcodeEntity barcode) {
        try {
            BarcodeFormat format = BarcodeFormat.valueOf(barcode.barcodeFormat);

            int width = format == BarcodeFormat.QR_CODE ? 800 : 1000;
            int height = format == BarcodeFormat.QR_CODE ? 800 : 400;

            Bitmap bitmap = BarcodeGenerator.generate(
                    barcode.barcodeContent,
                    format,
                    width,
                    height
            );

            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            int padding = (int) (
                    24 * getResources().getDisplayMetrics().density
            );

            imageView.setPadding(
                    padding,
                    padding,
                    padding,
                    padding
            );

            String title = barcode.barcodeName == null
                    || barcode.barcodeName.trim().isEmpty()
                    ? barcode.barcodeContent
                    : barcode.barcodeName;

            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setView(imageView)
                    .setPositiveButton("Close", null)
                    .show();

        } catch (WriterException | IllegalArgumentException e) {
            Toast.makeText(
                    this,
                    "Unable to display this barcode.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // Rename
    private void renameBarcode(BarcodeEntity barcode) {
        TextInputLayout inputLayout = new TextInputLayout(this);

        inputLayout.setHint("Barcode barcodeName");
        inputLayout.setBoxBackgroundMode(
                TextInputLayout.BOX_BACKGROUND_OUTLINE
        );

        TextInputEditText input = new TextInputEditText(
                inputLayout.getContext()
        );

        input.setSingleLine(true);
        input.setText(
                barcode.barcodeName == null ? "" : barcode.barcodeName
        );

        inputLayout.addView(input);

        int padding = (int) (
                24 * getResources().getDisplayMetrics().density
        );

        inputLayout.setPadding(padding, 8, padding, 0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rename Barcode")
                .setView(inputLayout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String name = input.getText() == null
                                ? ""
                                : input.getText().toString().trim();

                        historyViewModel.renameBarcode(
                                barcode,
                                name.isEmpty() ? null : name
                        );

                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    // Favorite
    private void toggleFavorite(BarcodeEntity barcode) {
        historyViewModel.toggleFavorite(barcode);
    }

    // Delete
    private void deleteBarcode(BarcodeEntity barcode) {
        historyViewModel.deleteBarcode(
                barcode,
                () -> Toast.makeText(
                        this,
                        "Barcode deleted",
                        Toast.LENGTH_SHORT
                ).show(),
                error -> Toast.makeText(
                        this,
                        "Unable to delete barcode",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    // Navigation
    private void setupBottomNavigation() {
        BottomNavigationHelper.setup(
                bottomNavigationView,
                R.id.nav_history,
                new BottomNavigationHelper.Listener() {

                    @Override
                    public void onCreateSelected() {
                        navigateToMain(R.id.nav_create);
                    }

                    @Override
                    public void onDecodeSelected() {
                        navigateToMain(R.id.nav_decode);
                    }

                    @Override
                    public void onHistorySelected() {
                        // Already on History.
                    }
                }
        );
    }

    private void navigateToMain(int selectedTab) {
        Intent result = new Intent();

        result.putExtra(
                MainActivity.EXTRA_SELECTED_TAB,
                selectedTab
        );

        setResult(RESULT_OK, result);
        finish();
    }
}
