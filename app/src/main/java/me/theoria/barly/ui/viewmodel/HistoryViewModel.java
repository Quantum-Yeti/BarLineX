package me.theoria.barly.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import me.theoria.barly.data.BarcodeRepository;
import me.theoria.barly.data.SavedBarcode;

public class HistoryViewModel extends AndroidViewModel {

    private final BarcodeRepository repository;

    private final MutableLiveData<List<SavedBarcode>> barcodes =
            new MutableLiveData<>();

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new BarcodeRepository(application);
        loadBarcodes();
    }

    public LiveData<List<SavedBarcode>> getBarcodes() {
        return barcodes;
    }

    public void loadBarcodes() {
        repository.getAll(barcodes::postValue);
    }

    public void deleteBarcode(SavedBarcode barcode) {
        repository.deleteById(barcode.id,
                this::loadBarcodes);
    }
}
