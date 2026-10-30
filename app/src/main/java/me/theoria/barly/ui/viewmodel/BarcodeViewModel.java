package me.theoria.barly.ui.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import me.theoria.barly.data.BarcodeRepository;
import me.theoria.barly.data.SavedBarcode;
import me.theoria.barly.utils.CodeGenerator;

public class BarcodeViewModel extends AndroidViewModel {

    private final BarcodeRepository repository;

    private final MutableLiveData<Bitmap> barcodeImage =
            new MutableLiveData<>();

    private final MutableLiveData<String> status =
            new MutableLiveData<>();

    public BarcodeViewModel(@NonNull Application application) {
        super(application);
        repository = new BarcodeRepository(application);
    }

    public LiveData<Bitmap> getBarcodeImage() {
        return barcodeImage;
    }

    public LiveData<String> getStatus() {
        return status;
    }

    public void generateBarcode(
            String content,
            BarcodeFormat format,
            boolean saveToHistory) {

        if (content == null || content.trim().isEmpty()) {
            barcodeImage.setValue(null);
            status.setValue("");
            return;
        }

        content = content.trim();

        int width = format == BarcodeFormat.QR_CODE ? 500 : 800;
        int height = format == BarcodeFormat.QR_CODE ? 500 : 300;

        try {
            Bitmap bitmap = CodeGenerator.generate(
                    content, format, width, height
            );

            barcodeImage.setValue(bitmap);

            if (!saveToHistory) {
                status.setValue("");
                return;
            }

            saveBarcode(content, format);

        } catch (WriterException | IllegalArgumentException e) {
            barcodeImage.setValue(null);
            status.setValue(
                    "Could not generate code: " + e.getMessage()
            );
        }
    }

    private void saveBarcode(String content, BarcodeFormat format) {
        SavedBarcode barcode =
                new SavedBarcode(content, format.name());

        repository.save(barcode, new BarcodeRepository.SaveCallback() {
            @Override
            public void onSaved(long id) {
                status.setValue("Barcode saved to history!");
            }

            @Override
            public void onError(Exception error) {
                status.setValue("Generated, but failed to save.");
                error.printStackTrace();
            }
        });
    }
}