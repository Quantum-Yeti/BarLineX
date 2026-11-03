package me.theoria.barlinex.ui;

import android.content.Intent;
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
import me.theoria.barlinex.data.SavedBarcode;
import me.theoria.barlinex.ui.adapter.BarcodeHistoryAdapter;
import me.theoria.barlinex.ui.helper.BottomNavigationHelper;
import me.theoria.barlinex.ui.helper.WindowsInsetHelper;
import me.theoria.barlinex.ui.viewmodel.HistoryViewModel;
import me.theoria.barlinex.utils.CodeGenerator;

public class HistoryActivity extends AppCompatActivity {

    private BarcodeHistoryAdapter adapter;
    private HistoryViewModel historyViewModel;

    private RecyclerView recyclerHistory;
    private View historyEmptyContainer;

    private TextView textHistoryCount;
    private TextView textHistoryEmpty;

    private TextInputEditText editHistorySearch;

    private BottomNavigationView bottomNavigationView;

    // Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        WindowsInsetHelper.applySystemBarInsets(
                findViewById(R.id.historyRoot)
        );

        initializeHistory();
        initializeViews();
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
    private void initializeHistory() {
        historyViewModel = new ViewModelProvider(this)
                .get(HistoryViewModel.class);

        adapter = new BarcodeHistoryAdapter(
                this::openBarcode,
                this::deleteBarcode,
                this::toggleFavorite,
                this::renameBarcode
        );
    }

    private void initializeViews() {
        recyclerHistory = findViewById(R.id.recyclerHistory);
        historyEmptyContainer = findViewById(R.id.historyEmptyContainer);

        textHistoryCount = findViewById(R.id.textHistoryCount);
        textHistoryEmpty = findViewById(R.id.textHistoryEmpty);

        editHistorySearch = findViewById(R.id.editHistorySearch);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
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
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {

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

    // History Data
    private void observeBarcodes() {
        historyViewModel.getBarcodes().observe(this, barcodes -> {

            adapter.setBarcodes(barcodes);

            // Preserve the current search after database updates.
            String query = editHistorySearch.getText() == null
                    ? ""
                    : editHistorySearch.getText().toString();

            adapter.filter(query);

            int count = barcodes == null ? 0 : barcodes.size();
            textHistoryCount.setText(String.valueOf(count));

            updateHistoryDisplay();
        });
    }

    private void updateHistoryDisplay() {
        boolean hasResults = adapter.getItemCount() > 0;

        String query = editHistorySearch.getText() == null
                ? ""
                : editHistorySearch.getText().toString().trim();

        boolean hasSearchQuery = !query.isEmpty();

        historyEmptyContainer.setVisibility(
                hasResults ? View.GONE : View.VISIBLE
        );

        recyclerHistory.setVisibility(
                hasResults ? View.VISIBLE : View.GONE
        );

        if (!hasResults) {
            if (hasSearchQuery) {
                textHistoryEmpty.setText(
                        "No barcodes match your search. Try a different name, value, or format."
                );
            } else {
                textHistoryEmpty.setText(
                        "Barcodes you save will appear here. Create your first barcode to get started."
                );
            }
        }
    }

    // Barcode Actions
    private void openBarcode(SavedBarcode barcode) {
        try {
            BarcodeFormat format = BarcodeFormat.valueOf(barcode.format);

            android.graphics.Bitmap bitmap = CodeGenerator.generate(
                    barcode.content,
                    format,
                    800,
                    800
            );

            ImageView imageView = new ImageView(this);

            imageView.setImageBitmap(bitmap);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(24, 24, 24, 24);

            String title = barcode.name == null || barcode.name.trim().isEmpty()
                    ? barcode.content
                    : barcode.name;

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

    private void renameBarcode(SavedBarcode barcode) {
        TextInputLayout inputLayout = new TextInputLayout(this);

        inputLayout.setHint("Barcode name");
        inputLayout.setBoxBackgroundMode(
                TextInputLayout.BOX_BACKGROUND_OUTLINE
        );

        TextInputEditText input = new TextInputEditText(inputLayout.getContext());
        input.setSingleLine(true);

        input.setText(
                barcode.name == null ? "" : barcode.name
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

                        // An empty name restores the barcode's original value.
                        historyViewModel.renameBarcode(
                                barcode,
                                name.isEmpty() ? null : name
                        );

                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    private void toggleFavorite(SavedBarcode barcode) {
        historyViewModel.toggleFavorite(barcode);
    }

    private void deleteBarcode(SavedBarcode barcode) {
        historyViewModel.deleteBarcode(barcode);

        Toast.makeText(
                this,
                "Barcode deleted",
                Toast.LENGTH_SHORT
        ).show();
    }

    // Bottom Navigation
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
