package me.theoria.barlinex.ui.preview;

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
import me.theoria.barlinex.ui.controller.BarcodeActionController;
import me.theoria.barlinex.ui.main.MainViewModel;

public class BarcodePreviewActivity extends AppCompatActivity {

    private ImageView imageBarcode;
    private TextView textBarcodeName;
    private TextView textBarcodeContent;
    private TextView textBarcodeFormat;

    private MainViewModel mainViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_preview);

        applyWindowInsets();

        imageBarcode = findViewById(R.id.imageBarcode);
        textBarcodeName = findViewById(R.id.textBarcodeName);
        textBarcodeContent = findViewById(R.id.textBarcodeContent);
        textBarcodeFormat = findViewById(R.id.textBarcodeFormat);

        Button buttonBackBottom = findViewById(R.id.buttonBackBottom);
        buttonBackBottom.setOnClickListener(v -> finish());

        String name = getIntent().getStringExtra(
                BarcodeActionController.EXTRA_BARCODE_NAME
        );

        textBarcodeName.setText(
                name == null || name.trim().isEmpty()
                        ? "Untitled Barcode"
                        : name.trim()
        );
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

        mainViewModel = new ViewModelProvider(this)
                .get(MainViewModel.class);

        mainViewModel.getBarcodeImage().observe(this, bitmap -> {
            if (bitmap != null) {
                imageBarcode.setImageBitmap(bitmap);
            }
        });

        if (content != null && !content.trim().isEmpty()) {
            com.google.zxing.BarcodeFormat barcodeFormat =
                    "CODE_128".equals(format)
                            ? com.google.zxing.BarcodeFormat.CODE_128
                            : com.google.zxing.BarcodeFormat.QR_CODE;

            mainViewModel.generateBarcode(content, barcodeFormat, false);
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
