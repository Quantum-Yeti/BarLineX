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
import me.theoria.barlinex.domain.barcode.BarcodeGenerationResult;
import me.theoria.barlinex.domain.barcode.BarcodeGenerator;
import me.theoria.barlinex.domain.barcode.BarcodeStatistics;
import me.theoria.barlinex.domain.barcode.BarcodeValidator;

public class MainViewModel extends AndroidViewModel {

    private final BarcodeRepository barcodeRepository;
    private Integer editingBarcodeId = null;

    private final MutableLiveData<Bitmap> barcodeImage =
            new MutableLiveData<>();

    private final MutableLiveData<BarcodeStatistics> barcodeStatistics =
            new MutableLiveData<>();

    private final MutableLiveData<String> barcodeStatus =
            new MutableLiveData<>("");

    private final MutableLiveData<BarcodeFormat> selectedBarcodeFormat =
            new MutableLiveData<>(BarcodeFormat.QR_CODE);

    private final MutableLiveData<String> barcodeName =
            new MutableLiveData<>("");

    private final MutableLiveData<String> barcodeContent =
            new MutableLiveData<>("");

    private final MutableLiveData<String> barcodeNote =
            new MutableLiveData<>("");

    private final MutableLiveData<String> barcodeCategory =
            new MutableLiveData<>("");

    public MainViewModel(@NonNull Application application) {
        super(application);
        barcodeRepository = new BarcodeRepository(application);
    }

    // LiveData
    public LiveData<Bitmap> getBarcodeImage() {
        return barcodeImage;
    }

    public LiveData<BarcodeStatistics> getBarcodeStatistics() {
        return barcodeStatistics;
    }

    public LiveData<String> getBarcodeStatus() {
        return barcodeStatus;
    }

    public LiveData<BarcodeFormat> getSelectedBarcodeFormat() {
        return selectedBarcodeFormat;
    }

    public LiveData<String> getBarcodeName() {
        return barcodeName;
    }

    public LiveData<String> getBarcodeContent() {
        return barcodeContent;
    }

    public LiveData<String> getBarcodeNote() {
        return barcodeNote;
    }

    public LiveData<String> getBarcodeCategory() {
        return barcodeCategory;
    }

    // Input state
    public void setSelectedBarcodeFormat(BarcodeFormat format) {
        if (format != null) {
            selectedBarcodeFormat.setValue(format);
        }
    }

    public void setBarcodeName(String name) {
        barcodeName.setValue(name != null ? name : "");
    }

    public void setBarcodeContent(String content) {
        barcodeContent.setValue(content != null ? content : "");
    }

    public void setBarcodeNote(String note) {
        barcodeNote.setValue(note != null ? note : "");
    }

    public void setBarcodeCategory(String category) {
        barcodeCategory.setValue(category != null ? category : "");
    }

    // Generation
    public void generateBarcode(boolean saveToHistory) {
        generateBarcode(
                barcodeContent.getValue(),
                selectedBarcodeFormat.getValue(),
                saveToHistory
        );
    }

    public void generateBarcode(
            String content,
            BarcodeFormat format,
            boolean saveToHistory
    ) {
        generateBarcode(
                content,
                format,
                saveToHistory,
                null
        );
    }

    public boolean generateBarcode(
            String content,
            BarcodeFormat format,
            boolean saveToHistory,
            BarcodeRepository.SaveCallback saveCallback
    ) {

        if (content == null || content.trim().isEmpty()) {
            clearGeneratedBarcode();
            return false;
        }

        if (format == null) {
            clearGeneratedBarcode();
            barcodeStatus.setValue("Select a barcode format");
            return false;
        }

        String validationError =
                BarcodeValidator.validate(format, content);

        if (validationError != null) {
            clearGeneratedBarcode();
            barcodeStatus.setValue(validationError);
            return false;
        }

        int width = format == BarcodeFormat.QR_CODE
                ? 500
                : 800;

        int height = format == BarcodeFormat.QR_CODE
                ? 500
                : 300;

        try {

            BarcodeGenerationResult result =
                    BarcodeGenerator.generateDetailed(
                            content,
                            format,
                            width,
                            height
                    );

            barcodeImage.setValue(result.getBitmap());

            barcodeStatistics.setValue(
                    result.getStatistics()
            );

            barcodeStatus.setValue("");

            if (saveToHistory) {
                saveBarcodeToHistory(
                        content,
                        format,
                        barcodeNote.getValue(),
                        saveCallback
                );
            }

            return true;

        } catch (WriterException | IllegalArgumentException e) {

            clearGeneratedBarcode();

            barcodeStatus.setValue(
                    "Could not generate code: " + e.getMessage()
            );

            return false;
        }
    }

    private void clearGeneratedBarcode() {
        barcodeImage.setValue(null);
        barcodeStatistics.setValue(null);
    }

    // History
    private void saveBarcodeToHistory(
            String content,
            BarcodeFormat format,
            String note,
            BarcodeRepository.SaveCallback callback
    ) {
        String name = barcodeName.getValue();

        String savedName = name != null && !name.trim().isEmpty()
                ? name.trim()
                : null;

        Integer currentEditingId = editingBarcodeId;

        // Update an existing barcode.
        if (currentEditingId != null) {

            barcodeRepository.updateBarcodeDetails(
                    currentEditingId,
                    savedName,
                    content,
                    format.name(),
                    note,
                    barcodeCategory.getValue(),
                    new BarcodeRepository.SaveCallback() {

                        @Override
                        public void onSaved(long id) {
                            // Exit edit mode after a successful update.
                            editingBarcodeId = null;

                            if (callback != null) {
                                callback.onSaved(id);
                            }
                        }

                        @Override
                        public void onError(Exception exception) {
                            // Keep edit mode active if the update fails.
                            if (callback != null) {
                                callback.onError(exception);
                            }
                        }
                    }
            );

            return;
        }

        // Otherwise, create a new barcode record.
        BarcodeEntity entity = new BarcodeEntity(
                content,
                format.name(),
                note,
                barcodeCategory.getValue()
        );

        entity.barcodeName = savedName;

        barcodeRepository.save(entity, callback);
    }

    public void setEditingBarcodeId(Integer id) {
        editingBarcodeId = id;
    }

    public Integer getEditingBarcodeId() {
        return editingBarcodeId;
    }

    public boolean isEditingBarcode() {
        return editingBarcodeId != null;
    }

    // Status
    public void clearBarcodeStatus() {
        barcodeStatus.setValue("");
    }

    public void clearBarcodeImage() {
        clearGeneratedBarcode();
        clearBarcodeStatus();
    }

    @Override
    protected void onCleared() {
        barcodeRepository.shutdown();
    }
}