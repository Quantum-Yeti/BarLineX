package me.theoria.barlinex.ui.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import me.theoria.barlinex.data.BarcodeRepository;
import me.theoria.barlinex.data.SavedBarcode;
import me.theoria.barlinex.domain.validator.BarcodeValidator;
import me.theoria.barlinex.utils.CodeGenerator;

public class BarcodeViewModel extends AndroidViewModel {

    // Repository
    private final BarcodeRepository repository;

    // Barcode preview
    private final MutableLiveData<Bitmap> barcodeImage =
            new MutableLiveData<>();

    // Status
    private final MutableLiveData<String> status =
            new MutableLiveData<>("");

    // Input state
    private final MutableLiveData<BarcodeFormat> selectedFormat =
            new MutableLiveData<>(BarcodeFormat.QR_CODE);

    private final MutableLiveData<String> inputContent =
            new MutableLiveData<>("");

    public BarcodeViewModel(@NonNull Application application) {
        super(application);
        repository = new BarcodeRepository(application);
    }

    // LiveData
    public LiveData<Bitmap> getBarcodeImage() {
        return barcodeImage;
    }

    public LiveData<String> getStatus() {
        return status;
    }

    public LiveData<BarcodeFormat> getSelectedFormat() {
        return selectedFormat;
    }

    public LiveData<String> getInputContent() {
        return inputContent;
    }

    // Input state
    public void setSelectedFormat(BarcodeFormat format) {
        if (format != null) {
            selectedFormat.setValue(format);
        }
    }

    public void setInputContent(String content) {
        inputContent.setValue(content != null ? content : "");
    }

    // Barcode generation
    public void generateBarcode(boolean saveToHistory) {
        generateBarcode(
                inputContent.getValue(),
                selectedFormat.getValue(),
                saveToHistory
        );
    }

    public void generateBarcode(
            String content,
            BarcodeFormat format,
            boolean saveToHistory
    ) {
        generateBarcode(content, format, saveToHistory, null);
    }

    public boolean generateBarcode(
            String content,
            BarcodeFormat format,
            boolean saveToHistory,
            BarcodeRepository.SaveCallback callback
    ) {

        if (content == null || content.trim().isEmpty()) {
            barcodeImage.setValue(null);
            status.setValue("");
            return false;
        }

        if (format == null) {
            barcodeImage.setValue(null);
            status.setValue("Select a barcode format");
            return false;
        }

        content = content.trim();

        String error = BarcodeValidator.validate(format, content);

        if (error != null) {
            barcodeImage.setValue(null);
            status.setValue(error);
            return false;
        }

        int width = format == BarcodeFormat.QR_CODE ? 500 : 800;
        int height = format == BarcodeFormat.QR_CODE ? 500 : 300;

        try {
            Bitmap bitmap = CodeGenerator.generate(
                    content,
                    format,
                    width,
                    height
            );

            barcodeImage.setValue(bitmap);
            status.setValue("");

            if (saveToHistory) {
                saveBarcode(content, format, callback);
            }

            return true;

        } catch (WriterException | IllegalArgumentException e) {
            barcodeImage.setValue(null);
            status.setValue(
                    "Could not generate code: " + e.getMessage()
            );
            return false;
        }
    }

    // History
    private void saveBarcode(
            String content,
            BarcodeFormat format,
            BarcodeRepository.SaveCallback callback
    ) {

        SavedBarcode barcode = new SavedBarcode(
                content,
                format.name()
        );

        repository.save(barcode, callback);
    }

    // Status
    public void clearStatus() {
        status.setValue("");
    }

    public void clearBarcode() {
        barcodeImage.setValue(null);
        clearStatus();
    }

    // Cleanup
    @Override
    protected void onCleared() {
        repository.shutdown();
        super.onCleared();
    }
}