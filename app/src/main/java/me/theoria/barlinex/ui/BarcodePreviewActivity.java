package me.theoria.barlinex.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class BarcodePreviewActivity extends AppCompatActivity {

    private ImageView imageBarcode;
    private TextView textBarcodeContent;
    private TextView textBarcodeFormat;

    private BarcodeViewModel barcodeViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_preview);

        applyWindowInsets();

        imageBarcode = findViewById(R.id.imageBarcode);
        textBarcodeContent = findViewById(R.id.textBarcodeContent);
        textBarcodeFormat = findViewById(R.id.textBarcodeFormat);

        Button buttonBackBottom = findViewById(R.id.buttonBackBottom);
        buttonBackBottom.setOnClickListener(v -> finish());

        String content = getIntent().getStringExtra("barcode_content");
        String format = getIntent().getStringExtra("barcode_format");

        textBarcodeContent.setText(
                content == null || content.isEmpty()
                        ? "No barcodeContent"
                        : content
        );

        textBarcodeFormat.setText(
                "CODE_128".equals(format) ? "CODE 128" : "QR CODE"
        );

        barcodeViewModel = new ViewModelProvider(this)
                .get(BarcodeViewModel.class);

        barcodeViewModel.getBarcodeImage().observe(this, bitmap -> {
            if (bitmap != null) {
                imageBarcode.setImageBitmap(bitmap);
            }
        });

        if (content != null && !content.trim().isEmpty()) {
            com.google.zxing.BarcodeFormat barcodeFormat =
                    "CODE_128".equals(format)
                            ? com.google.zxing.BarcodeFormat.CODE_128
                            : com.google.zxing.BarcodeFormat.QR_CODE;

            barcodeViewModel.generateBarcode(content, barcodeFormat, false);
        }
    }

    private void applyWindowInsets() {

        View root = findViewById(R.id.barcodePreviewRoot);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {

            androidx.core.graphics.Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            view.setPadding(
                    22,
                    22 + insets.top,
                    22,
                    22 + insets.bottom
            );

            return windowInsets;
        });

        ViewCompat.requestApplyInsets(root);
    }
}
