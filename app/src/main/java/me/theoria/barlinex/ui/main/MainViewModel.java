
package me.theoria.barlinex.ui.main;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.zxing.BarcodeFormat;

import me.theoria.barlinex.data.local.BarcodeEntity;
import me.theoria.barlinex.data.repository.BarcodeRepository;
import me.theoria.barlinex.domain.barcode.BarcodeGenerationResult;
import me.theoria.barlinex.domain.barcode.BarcodeGenerator;
import me.theoria.barlinex.domain.barcode.BarcodeStatistics;
import me.theoria.barlinex.domain.barcode.BarcodeValidator;

public class MainViewModel extends AndroidViewModel {

    private final BarcodeRepository barcodeRepository;

    private final ExecutorService generationExecutor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private final AtomicInteger generationRequestId =
            new AtomicInteger(0);

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

    public interface GenerationCallback {
        void onComplete(boolean success);
    }

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
        generateBarcodeAsync(
                barcodeContent.getValue(),
                selectedBarcodeFormat.getValue(),
                saveToHistory,
                null,
                null
        );
    }

    public void generateBarcode(
            String content,
            BarcodeFormat format,
            boolean saveToHistory
    ) {
        generateBarcodeAsync(
                content,
                format,
                saveToHistory,
                null,
                null
        );
    }

    public void generateBarcodeAsync(
            String content,
            BarcodeFormat format,
            boolean saveToHistory,
            BarcodeRepository.SaveCallback saveCallback,
            GenerationCallback generationCallback
    ) {

        if (content == null || content.trim().isEmpty()) {
            clearGeneratedBarcode();
            return;
        }

        if (format == null) {
            clearGeneratedBarcode();
            barcodeStatus.setValue("Select a barcode format");

            if (generationCallback != null) {
                generationCallback.onComplete(false);
            }

            return;
        }

        // Capture the current state before entering the background thread.

        String savedName = barcodeName.getValue();
        String savedNote = barcodeNote.getValue();
        String savedCategory = barcodeCategory.getValue();

        Integer currentEditingId = editingBarcodeId;

        int requestId = generationRequestId.incrementAndGet();

        generationExecutor.execute(() -> {

            try {

                String validationError =
                        BarcodeValidator.validate(format, content);

                if (validationError != null) {

                    publishGenerationError(
                            requestId,
                            validationError,
                            generationCallback
                    );

                    return;
                }

                int width = format == BarcodeFormat.QR_CODE
                        ? 500
                        : 800;

                int height = format == BarcodeFormat.QR_CODE
                        ? 500
                        : 300;

                BarcodeGenerationResult result =
                        BarcodeGenerator.generateDetailed(
                                content,
                                format,
                                width,
                                height
                        );

                Bitmap bitmap = result.getBitmap();
                BarcodeStatistics statistics = result.getStatistics();

                // Only publish the latest requested preview.

                mainHandler.post(() -> {

                    if (requestId != generationRequestId.get()) {
                        return;
                    }

                    barcodeImage.setValue(bitmap);
                    barcodeStatistics.setValue(statistics);
                    barcodeStatus.setValue("");

                    if (generationCallback != null) {
                        generationCallback.onComplete(true);
                    }
                });

                // Saving is independent of whether this is still
                // the latest preview request.

                if (saveToHistory) {

                    saveBarcodeToHistory(
                            content,
                            format,
                            savedName,
                            savedNote,
                            savedCategory,
                            currentEditingId,
                            saveCallback
                    );
                }

            } catch (WriterException | IllegalArgumentException e) {

                publishGenerationError(
                        requestId,
                        "Could not generate code: " + e.getMessage(),
                        generationCallback
                );
            }
        });
    }

    private void publishGenerationError(
            int requestId,
            String message,
            GenerationCallback callback
    ) {

        mainHandler.post(() -> {

            if (requestId != generationRequestId.get()) {
                return;
            }

            clearGeneratedBarcode();

            barcodeStatus.setValue(message);

            if (callback != null) {
                callback.onComplete(false);
            }
        });
    }

    private void clearGeneratedBarcode() {

        generationRequestId.incrementAndGet();

        barcodeImage.setValue(null);
        barcodeStatistics.setValue(null);
    }

    // History

    private void saveBarcodeToHistory(
            String content,
            BarcodeFormat format,
            String name,
            String note,
            String category,
            Integer currentEditingId,
            BarcodeRepository.SaveCallback callback
    ) {

        String savedName = name != null && !name.trim().isEmpty()
                ? name.trim()
                : null;

        // Update an existing barcode.

        if (currentEditingId != null) {

            barcodeRepository.updateBarcodeDetails(
                    currentEditingId,
                    savedName,
                    content,
                    format.name(),
                    note,
                    category,
                    new BarcodeRepository.SaveCallback() {

                        @Override
                        public void onSaved(long id) {

                            mainHandler.post(() -> {
                                editingBarcodeId = null;

                                if (callback != null) {
                                    callback.onSaved(id);
                                }
                            });
                        }

                        @Override
                        public void onError(Exception exception) {

                            mainHandler.post(() -> {
                                if (callback != null) {
                                    callback.onError(exception);
                                }
                            });
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
                category
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

        generationRequestId.incrementAndGet();

        generationExecutor.shutdownNow();

        barcodeRepository.shutdown();

        super.onCleared();
    }
}
