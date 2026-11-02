package me.theoria.barlinex.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import me.theoria.barlinex.data.BarcodeRepository;
import me.theoria.barlinex.data.SavedBarcode;

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
        repository.getAll(loadedBarcodes -> {
            android.util.Log.d("FavoriteDebug", "----- HISTORY ORDER -----");

            for (SavedBarcode barcode : loadedBarcodes) {
                android.util.Log.d("FavoriteDebug",
                        "ID: " + barcode.id
                                + " | Favorite: " + barcode.isFavorite
                                + " | Content: " + barcode.content);
            }

            barcodes.setValue(loadedBarcodes);
        });
    }

    public void toggleFavorite(SavedBarcode barcode) {
        repository.updateFavorite(
                barcode.id,
                !barcode.isFavorite,
                this::loadBarcodes
        );
    }

    public void deleteBarcode(SavedBarcode barcode) {
        repository.deleteById(barcode.id,
                this::loadBarcodes);
    }
}
