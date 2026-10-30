package me.theoria.barly.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.theoria.barly.R;
import me.theoria.barly.ui.adapter.BarcodeHistoryAdapter;
import me.theoria.barly.data.SavedBarcode;
import me.theoria.barly.ui.viewmodel.HistoryViewModel;

public class HistoryActivity extends AppCompatActivity {

    private BarcodeHistoryAdapter adapter;
    private RecyclerView recyclerHistory;
    private TextView textHistoryEmpty;
    private HistoryViewModel historyViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        recyclerHistory = findViewById(R.id.recyclerHistory);
        textHistoryEmpty = findViewById(R.id.textHistoryEmpty);

        historyViewModel = new ViewModelProvider(this)
                .get(HistoryViewModel.class);

        adapter = new BarcodeHistoryAdapter(
                this::openBarcode,
                this::deleteBarcode);

        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistory.setAdapter(adapter);

        recyclerHistory.addItemDecoration(
                new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(
                            android.graphics.Rect outRect,
                            View view,
                            RecyclerView parent,
                            androidx.recyclerview.widget.RecyclerView.State state) {
                        outRect.bottom = 4;
                    }
                }
        );

        historyViewModel.getBarcodes().observe(this, barcodes -> {
            adapter.setBarcodes(barcodes);

            boolean isEmpty = barcodes == null || barcodes.isEmpty();

            textHistoryEmpty.setVisibility(
                    isEmpty ? View.VISIBLE : View.GONE
            );

            recyclerHistory.setVisibility(
                    isEmpty ? View.GONE : View.VISIBLE
            );
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
            com.google.zxing.BarcodeFormat format =
                    com.google.zxing.BarcodeFormat.valueOf(barcode.format);

            android.graphics.Bitmap bitmap =
                    me.theoria.barly.utils.CodeGenerator.generate(
                            barcode.content,
                            format,
                            800,
                            800
                    );

            android.widget.ImageView imageView =
                    new android.widget.ImageView(this);

            imageView.setImageBitmap(bitmap);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            imageView.setPadding(24, 24, 24, 24);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(barcode.content)
                    .setView(imageView)
                    .setPositiveButton("Close", null)
                    .show();

        } catch (com.google.zxing.WriterException |
                 IllegalArgumentException e) {
            android.widget.Toast.makeText(
                    this,
                    "Unable to display this barcode.",
                    android.widget.Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void deleteBarcode(SavedBarcode barcode) {
        historyViewModel.deleteBarcode(barcode);

        Toast.makeText(
                this,
                "Barcode deleted",
                Toast.LENGTH_SHORT
        ).show();
    }
}
