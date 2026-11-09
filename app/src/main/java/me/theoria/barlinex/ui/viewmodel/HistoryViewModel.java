package me.theoria.barlinex.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import me.theoria.barlinex.data.BarcodeEntity;
import me.theoria.barlinex.data.BarcodeRepository;

public class HistoryViewModel extends AndroidViewModel {

    // Repository
    private final BarcodeRepository repository;

    // History
    private final MutableLiveData<List<BarcodeEntity>> barcodes =
            new MutableLiveData<>();

    // Initialization
    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new BarcodeRepository(application);
        loadBarcodes();
    }

    // LiveData
    public LiveData<List<BarcodeEntity>> getBarcodes() {
        return barcodes;
    }

    // Load
    public void loadBarcodes() {
        repository.getAll(new BarcodeRepository.LoadCallback() {
            @Override
            public void onLoaded(List<BarcodeEntity> loadedBarcodes) {
                barcodes.setValue(loadedBarcodes);
            }

            @Override
            public void onError(Exception error) {
                Log.e("HistoryViewModel", "Failed to load history", error);
            }
        });
    }

    // Rename
    public void renameBarcode(BarcodeEntity barcode, String name) {
        repository.updateName(
                barcode.id,
                name,
                new BarcodeRepository.OperationCallback() {
                    @Override
                    public void onComplete() {
                        loadBarcodes();
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e("HistoryViewModel", "Failed to rename barcode", error);
                    }
                }
        );
    }

    // Favorite
    public void toggleFavorite(BarcodeEntity barcode) {
        repository.updateFavorite(
                barcode.id,
                !barcode.isFavorite,
                new BarcodeRepository.OperationCallback() {
                    @Override
                    public void onComplete() {
                        loadBarcodes();
                    }

                    @Override
                    public void onError(Exception error) {
                        Log.e("HistoryViewModel", "Failed to update favorite", error);
                    }
                }
        );
    }

    // Delete

    public void deleteBarcode(
            BarcodeEntity barcode,
            Runnable onDeleted,
            java.util.function.Consumer<Exception> onError
    ) {
        repository.deleteById(
                barcode.id,
                new BarcodeRepository.DeleteCallback() {
                    @Override
                    public void onDeleted() {
                        loadBarcodes();

                        if (onDeleted != null) {
                            onDeleted.run();
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        if (onError != null) {
                            onError.accept(error);
                        }
                    }
                }
        );
    }


    // Cleanup
    @Override
    protected void onCleared() {
        repository.shutdown();
        super.onCleared();
    }
}
