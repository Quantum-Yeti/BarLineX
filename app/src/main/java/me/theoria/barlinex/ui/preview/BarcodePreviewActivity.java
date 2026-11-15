package me.theoria.barlinex.ui.preview;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;

import java.util.Locale;

import me.theoria.barlinex.R;
import me.theoria.barlinex.domain.barcode.BarcodeStatistics;
import me.theoria.barlinex.ui.controller.BarcodeActionController;
import me.theoria.barlinex.ui.main.MainViewModel;

public class BarcodePreviewActivity extends AppCompatActivity {

    // Preview views
    private ImageView imageBarcode;
    private TextView textBarcodeName;
    private TextView textBarcodeContent;
    private TextView textBarcodeFormat;

    // Toolbar buttons
    private MaterialButton buttonBackBottom;
    private MaterialButton buttonSaveImage;
    private MaterialButton buttonShareBarcode;

    // Expandable section buttons
    private MaterialButton buttonContentDropDown;
    private MaterialButton buttonDropDown;

    // Expandable section containers
    private LinearLayout contentBarcodeContent;
    private LinearLayout contentBarcodeInformation;

    // Statistics summary
    private TextView textBarcodeLength;

    // Individual statistics containers
    private LinearLayout sectionGeneralStatistics;
    private LinearLayout sectionCharacterAnalysis;
    private LinearLayout sectionContentAnalysis;
    private LinearLayout sectionEncodingDetails;
    private LinearLayout sectionGenerationDetails;

    // ViewModel
    private MainViewModel mainViewModel;

    private boolean isContentExpanded = false;
    private boolean isInformationExpanded = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_preview);

        initializeViews();

        mainViewModel = new ViewModelProvider(this)
                .get(MainViewModel.class);

        setupListeners();
        observeBarcodeImage();
        observeBarcodeStatistics();
        loadBarcodeDetails();
    }

    private void initializeViews() {

        // Preview
        imageBarcode = findViewById(R.id.imageBarcode);
        textBarcodeName = findViewById(R.id.textBarcodeName);
        textBarcodeContent = findViewById(R.id.textBarcodeContent);
        textBarcodeFormat = findViewById(R.id.textBarcodeFormat);

        // Toolbar
        buttonBackBottom = findViewById(R.id.buttonBackBottom);
        buttonSaveImage = findViewById(R.id.buttonSaveImage);
        buttonShareBarcode = findViewById(R.id.buttonShareBarcode);

        // Dropdown buttons
        buttonContentDropDown =
                findViewById(R.id.buttonContentDropDown);

        buttonDropDown =
                findViewById(R.id.buttonDropDown);

        // Expandable content
        contentBarcodeContent =
                findViewById(R.id.contentBarcodeContent);

        contentBarcodeInformation =
                findViewById(R.id.contentBarcodeInformation);

        // Statistics summary
        textBarcodeLength =
                findViewById(R.id.textBarcodeLength);

        // Statistics containers
        sectionGeneralStatistics =
                findViewById(R.id.sectionGeneralStatistics);

        sectionCharacterAnalysis =
                findViewById(R.id.sectionCharacterAnalysis);

        sectionContentAnalysis =
                findViewById(R.id.sectionContentAnalysis);

        sectionEncodingDetails =
                findViewById(R.id.sectionEncodingDetails);

        sectionGenerationDetails =
                findViewById(R.id.sectionGenerationDetails);
    }

    private void setupListeners() {

        buttonBackBottom.setOnClickListener(v -> finish());

        buttonContentDropDown.setOnClickListener(v ->
                toggleSection(
                        contentBarcodeContent,
                        buttonContentDropDown,
                        !isContentExpanded,
                        true
                )
        );

        buttonDropDown.setOnClickListener(v ->
                toggleSection(
                        contentBarcodeInformation,
                        buttonDropDown,
                        !isInformationExpanded,
                        false
                )
        );
    }

    private void toggleSection(
            LinearLayout section,
            MaterialButton button,
            boolean expand,
            boolean isContentSection
    ) {

        section.setVisibility(expand ? View.VISIBLE : View.GONE);

        button.animate()
                .rotation(expand ? 180f : 0f)
                .setDuration(180)
                .start();

        button.setContentDescription(
                expand ? "Collapse section" : "Expand section"
        );

        if (isContentSection) {
            isContentExpanded = expand;
        } else {
            isInformationExpanded = expand;
        }
    }

    private void loadBarcodeDetails() {

        String barcodeName = getIntent().getStringExtra(
                BarcodeActionController.EXTRA_BARCODE_NAME
        );

        String barcodeContent = getIntent().getStringExtra(
                "barcode_content"
        );

        String barcodeFormat = getIntent().getStringExtra(
                "barcode_format"
        );

        if (barcodeName == null || barcodeName.trim().isEmpty()) {
            barcodeName = "Untitled Barcode";
        }

        if (barcodeContent == null) {
            barcodeContent = "";
        }

        if (barcodeFormat == null || barcodeFormat.trim().isEmpty()) {
            barcodeFormat = "QR_CODE";
        }

        textBarcodeName.setText(barcodeName);
        textBarcodeContent.setText(barcodeContent);
        textBarcodeFormat.setText(formatDisplayName(barcodeFormat));

        if (barcodeContent.isEmpty()) {
            textBarcodeLength.setText("No barcode content available");
            clearStatistics();
            return;
        }

        try {
            BarcodeFormat format = BarcodeFormat.valueOf(
                    barcodeFormat.toUpperCase(Locale.ROOT)
            );

            mainViewModel.generateBarcode(
                    barcodeContent,
                    format,
                    false
            );

        } catch (IllegalArgumentException exception) {
            textBarcodeFormat.setText(barcodeFormat);
            textBarcodeLength.setText("Unsupported barcode format");
            clearStatistics();
        }
    }

    private String formatDisplayName(String format) {

        if (format == null) {
            return "Barcode";
        }

        switch (format.toUpperCase(Locale.ROOT)) {
            case "QR_CODE":
                return "QR Code";

            case "CODE_128":
                return "Code 128";

            default:
                return format.replace("_", " ");
        }
    }

    private void observeBarcodeImage() {

        mainViewModel.getBarcodeImage().observe(this, bitmap -> {

            if (bitmap != null) {
                imageBarcode.setImageBitmap(bitmap);
            } else {
                imageBarcode.setImageDrawable(null);
            }
        });
    }

    private void observeBarcodeStatistics() {

        mainViewModel.getBarcodeStatistics().observe(this, statistics -> {

            if (statistics == null) {
                textBarcodeLength.setText("No statistics available");
                clearStatistics();
                return;
            }

            textBarcodeLength.setText(
                    statistics.getCharacterCount()
                            + " characters  \u2022  "
                            + statistics.getByteCount()
                            + " bytes"
            );

            renderStatistics(statistics);
        });
    }

    private void renderStatistics(BarcodeStatistics statistics) {

        clearStatistics();

        if (statistics == null || statistics.getSections() == null) {
            return;
        }

        for (BarcodeStatistics.Section section : statistics.getSections()) {

            if (section == null || section.getMetrics() == null) {
                continue;
            }

            LinearLayout container = getStatisticsContainer(
                    section.getTitle()
            );

            if (container == null) {
                continue;
            }

            for (BarcodeStatistics.Metric metric : section.getMetrics()) {

                if (metric == null) {
                    continue;
                }

                container.addView(
                        createStatisticRow(
                                metric.getLabel(),
                                metric.getValue()
                        )
                );
            }

            container.setVisibility(
                    container.getChildCount() > 0
                            ? View.VISIBLE
                            : View.GONE
            );
        }
    }

    private LinearLayout getStatisticsContainer(String sectionTitle) {

        if (sectionTitle == null) {
            return null;
        }

        String normalizedTitle = sectionTitle
                .trim()
                .toLowerCase(Locale.ROOT);

        switch (normalizedTitle) {

            case "general statistics":
                return sectionGeneralStatistics;

            case "character analysis":
                return sectionCharacterAnalysis;

            case "content analysis":
                return sectionContentAnalysis;

            case "encoding details":
                return sectionEncodingDetails;

            case "generation details":
                return sectionGenerationDetails;

            default:
                return null;
        }
    }

    private TextView createSectionHeading(String title) {

        TextView heading = new TextView(this);

        heading.setText(title);
        heading.setTextColor(0xFF92929B);
        heading.setTextSize(12);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setIncludeFontPadding(false);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = dp(10);

        heading.setLayoutParams(params);

        return heading;
    }

    private View createStatisticRow(String label, String value) {

        LinearLayout row = new LinearLayout(this);

        row.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView labelView = new TextView(this);

        labelView.setText(label);
        labelView.setTextColor(0xFFC4C4CE);
        labelView.setTextSize(13);
        labelView.setIncludeFontPadding(false);

        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        labelView.setLayoutParams(labelParams);

        TextView valueView = new TextView(this);

        valueView.setText(value);
        valueView.setTextColor(0xFFF5F5F5);
        valueView.setTextSize(13);
        valueView.setGravity(Gravity.END);
        valueView.setIncludeFontPadding(false);
        valueView.setTextIsSelectable(true);

        LinearLayout.LayoutParams valueParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        valueView.setLayoutParams(valueParams);

        row.addView(labelView);
        row.addView(valueView);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rowParams.bottomMargin = dp(11);

        row.setLayoutParams(rowParams);

        return row;
    }

    private void clearStatistics() {

        if (sectionGeneralStatistics != null) {
            sectionGeneralStatistics.removeAllViews();
        }

        if (sectionCharacterAnalysis != null) {
            sectionCharacterAnalysis.removeAllViews();
        }

        if (sectionContentAnalysis != null) {
            sectionContentAnalysis.removeAllViews();
        }

        if (sectionEncodingDetails != null) {
            sectionEncodingDetails.removeAllViews();
        }

        if (sectionGenerationDetails != null) {
            sectionGenerationDetails.removeAllViews();
        }
    }

    private int dp(int value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
