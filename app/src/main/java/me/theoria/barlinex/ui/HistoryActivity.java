package me.theoria.barlinex.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import me.theoria.barlinex.R;
import me.theoria.barlinex.data.BarcodeEntity;
import me.theoria.barlinex.ui.adapter.BarcodeHistoryAdapter;
import me.theoria.barlinex.ui.controller.BarcodeActionController;
import me.theoria.barlinex.ui.helper.BottomNavigationHelper;
import me.theoria.barlinex.ui.helper.WindowsInsetHelper;
import me.theoria.barlinex.ui.viewmodel.HistoryViewModel;

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
                this::editBarcode
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
                            ? "No barcodes match your search. \nTry a different name, value, or format."
                            : "Barcodes you save will appear here. \nCreate your first barcode to get started."
            );
        }
    }

    // Preview
    private void openBarcode(BarcodeEntity barcode) {
        Intent intent = new Intent(this, BarcodePreviewActivity.class);

        intent.putExtra(
                BarcodeActionController.EXTRA_BARCODE_NAME,
                barcode.barcodeName
        );

        intent.putExtra(
                BarcodeActionController.EXTRA_BARCODE_CONTENT,
                barcode.barcodeContent
        );

        intent.putExtra(
                BarcodeActionController.EXTRA_BARCODE_FORMAT,
                barcode.barcodeFormat
        );

        startActivity(intent);
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

    // Edit Barcode
    private void editBarcode(BarcodeEntity barcode) {
        Intent result = new Intent();

        result.putExtra(
                MainActivity.EXTRA_SELECTED_TAB,
                R.id.nav_create
        );

        result.putExtra(
                BarcodeActionController.EXTRA_BARCODE_NAME,
                barcode.barcodeName
        );

        result.putExtra(
                BarcodeActionController.EXTRA_BARCODE_CONTENT,
                barcode.barcodeContent
        );

        result.putExtra(
                BarcodeActionController.EXTRA_BARCODE_FORMAT,
                barcode.barcodeFormat
        );

        setResult(RESULT_OK, result);
        finish();
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
