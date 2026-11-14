package me.theoria.barlinex.ui.main;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import me.theoria.barlinex.data.local.BarcodeEntity;
import me.theoria.barlinex.data.repository.BarcodeRepository;
import me.theoria.barlinex.domain.barcode.BarcodeValidator;
import me.theoria.barlinex.domain.barcode.BarcodeGenerator;

public class MainViewModel extends AndroidViewModel {

    // Repository
    private final BarcodeRepository barcodeRepository;

    // Barcode preview
    private final MutableLiveData<Bitmap> barcodeImage =
            new MutableLiveData<>();

    // Status
    private final MutableLiveData<String> barcodeStatus =
            new MutableLiveData<>("");

    // Barcode input state
    private final MutableLiveData<BarcodeFormat> selectedBarcodeFormat =
            new MutableLiveData<>(BarcodeFormat.QR_CODE);

    private final MutableLiveData<String> barcodeName =
            new MutableLiveData<>("");

    private final MutableLiveData<String> barcodeContent =
            new MutableLiveData<>("");

    // Barcode note
    private final MutableLiveData<String> barcodeNote =
            new MutableLiveData<>("");

    // Barcode category
    private final MutableLiveData<String> barcodeCategory =
            new MutableLiveData<>("");

    public MainViewModel(@NonNull Application application) {
        super(application);
        barcodeRepository = new BarcodeRepository(application);
    }

    // LiveData Getters
    public LiveData<Bitmap> getBarcodeImage() {
        return barcodeImage;
    }

    public LiveData<String> getBarcodeStatus() {
        return barcodeStatus;
    }

    public LiveData<BarcodeFormat> getSelectedBarcodeFormat() {
        return selectedBarcodeFormat;
    }

    public LiveData<String> getBarcodeName() { return barcodeName; }

    public LiveData<String> getBarcodeContent() {
        return barcodeContent;
    }

    public LiveData<String> getBarcodeNote() {
        return barcodeNote;
    }

    public LiveData<String> getBarcodeCategory() { return barcodeCategory; }

    // Barcode input state
    public void setSelectedBarcodeFormat(BarcodeFormat barcodeFormat) {
        if (barcodeFormat != null) {
            selectedBarcodeFormat.setValue(barcodeFormat);
        }
    }

    public void setBarcodeName(String barcodeName) {
        this.barcodeName.setValue(
                barcodeName != null ? barcodeName : ""
        );
    }

    public void setBarcodeContent(String barcodeContent) {
        this.barcodeContent.setValue(
                barcodeContent != null ? barcodeContent : ""
        );
    }

    public void setBarcodeNote(String barcodeNote) {
        this.barcodeNote.setValue(
                barcodeNote != null ? barcodeNote : ""
        );
    }

    public void setBarcodeCategory(String barcodeCategory) {
        this.barcodeCategory.setValue(
                barcodeCategory != null ? barcodeCategory : ""
        );
    }

    // Barcode generation
    public void generateBarcode(boolean saveToHistory) {
        generateBarcode(
                barcodeContent.getValue(),
                selectedBarcodeFormat.getValue(),
                saveToHistory
        );
    }

    public void generateBarcode(
            String barcodeContent,
            BarcodeFormat barcodeFormat,
            boolean saveToHistory
    ) {
        generateBarcode(
                barcodeContent,
                barcodeFormat,
                saveToHistory,
                null
        );
    }

    public boolean generateBarcode(
            String barcodeContent,
            BarcodeFormat barcodeFormat,
            boolean saveToHistory,
            BarcodeRepository.SaveCallback saveCallback
    ) {

        if (barcodeContent == null || barcodeContent.trim().isEmpty()) {
            barcodeImage.setValue(null);
            barcodeStatus.setValue("");
            return false;
        }

        if (barcodeFormat == null) {
            barcodeImage.setValue(null);
            barcodeStatus.setValue("Select a barcode format");
            return false;
        }

        String validationError =
                BarcodeValidator.validate(barcodeFormat, barcodeContent);

        if (validationError != null) {
            barcodeImage.setValue(null);
            barcodeStatus.setValue(validationError);
            return false;
        }

        int barcodeWidth =
                barcodeFormat == BarcodeFormat.QR_CODE ? 500 : 800;

        int barcodeHeight =
                barcodeFormat == BarcodeFormat.QR_CODE ? 500 : 300;

        try {

            Bitmap generatedBarcode = BarcodeGenerator.generate(
                    barcodeContent,
                    barcodeFormat,
                    barcodeWidth,
                    barcodeHeight
            );

            barcodeImage.setValue(generatedBarcode);
            barcodeStatus.setValue("");

            if (saveToHistory) {
                saveBarcodeToHistory(
                        barcodeContent,
                        barcodeFormat,
                        barcodeNote.getValue(),
                        saveCallback
                );
            }

            return true;

        } catch (WriterException | IllegalArgumentException e) {

            barcodeImage.setValue(null);

            barcodeStatus.setValue(
                    "Could not generate code: " + e.getMessage()
            );

            return false;
        }
    }

    // History

    private void saveBarcodeToHistory(
            String barcodeContent,
            BarcodeFormat barcodeFormat,
            String barcodeNote,
            BarcodeRepository.SaveCallback saveCallback
    ) {

        BarcodeEntity barcodeEntity = new BarcodeEntity(
                barcodeContent,
                barcodeFormat.name(),
                barcodeNote,
                barcodeCategory.getValue()
        );

        String name = barcodeName.getValue();
        barcodeEntity.barcodeName = name != null && !name.trim().isEmpty()
                ? name.trim()
                : null;

        barcodeRepository.save(barcodeEntity, saveCallback);
    }

    // Status

    public void clearBarcodeStatus() {
        barcodeStatus.setValue("");
    }

    public void clearBarcodeImage() {
        barcodeImage.setValue(null);
        clearBarcodeStatus();
    }

    // Cleanup
    @Override
    protected void onCleared() {
        barcodeRepository.shutdown();
    }
}