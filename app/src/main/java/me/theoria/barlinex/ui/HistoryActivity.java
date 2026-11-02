package me.theoria.barlinex.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import me.theoria.barlinex.R;
import me.theoria.barlinex.data.SavedBarcode;
import me.theoria.barlinex.ui.adapter.BarcodeHistoryAdapter;
import me.theoria.barlinex.ui.helper.BottomNavigationHelper;
import me.theoria.barlinex.ui.viewmodel.HistoryViewModel;
import me.theoria.barlinex.utils.CodeGenerator;
import me.theoria.barlinex.ui.helper.WindowsInsetHelper;

public class HistoryActivity extends AppCompatActivity {

    private BarcodeHistoryAdapter adapter;
    private RecyclerView recyclerHistory;
    private View historyEmptyContainer;
    private TextView textHistoryCount;
    private HistoryViewModel historyViewModel;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        WindowsInsetHelper.applySystemBarInsets(
                findViewById(R.id.historyRoot)
        );

        initializeViews();
        initializeHistory();
        setupRecyclerView();
        observeBarcodes();
        setupBottomNavigation();
    }

    private void initializeViews() {

        recyclerHistory = findViewById(R.id.recyclerHistory);
        historyEmptyContainer = findViewById(R.id.historyEmptyContainer);
        textHistoryCount = findViewById(R.id.textHistoryCount);
    }

    private void initializeHistory() {
        historyViewModel = new ViewModelProvider(this)
                .get(HistoryViewModel.class);

        adapter = new BarcodeHistoryAdapter(
                this::openBarcode,
                this::deleteBarcode,
                this::toggleFavorite
        );
    }

    private void setupRecyclerView() {
        recyclerHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerHistory.setAdapter(adapter);
    }

    private void observeBarcodes() {
        historyViewModel.getBarcodes().observe(this, barcodes -> {

            adapter.setBarcodes(barcodes);

            boolean isEmpty = barcodes == null || barcodes.isEmpty();

            historyEmptyContainer.setVisibility(
                    isEmpty ? View.VISIBLE : View.GONE
            );

            recyclerHistory.setVisibility(
                    isEmpty ? View.GONE : View.VISIBLE
            );

            int count = barcodes == null ? 0 : barcodes.size();
            textHistoryCount.setText(String.valueOf(count));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (historyViewModel != null) {
            historyViewModel.loadBarcodes();
        }
    }

    private void openBarcode(SavedBarcode barcode) {
        try {
            BarcodeFormat format = BarcodeFormat.valueOf(barcode.format);

            android.graphics.Bitmap bitmap = CodeGenerator.generate(
                    barcode.content,
                    format,
                    800,
                    800
            );

            android.widget.ImageView imageView =
                    new android.widget.ImageView(this);

            imageView.setImageBitmap(bitmap);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(
                    android.widget.ImageView.ScaleType.FIT_CENTER
            );
            imageView.setPadding(24, 24, 24, 24);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(barcode.content)
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

    private void setupBottomNavigation() {

        bottomNavigationView = findViewById(R.id.bottomNavigation);

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
        result.putExtra(MainActivity.EXTRA_SELECTED_TAB, selectedTab);

        setResult(RESULT_OK, result);
        finish();
    }
}