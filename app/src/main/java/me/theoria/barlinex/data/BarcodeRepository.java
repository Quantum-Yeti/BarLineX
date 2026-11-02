
package me.theoria.barlinex.data;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeRepository {

    private final BarcodeDao barcodeDao;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public interface SaveCallback {
        void onSaved(long id);
        void onError(Exception error);
    }

    public interface LoadCallback {
        void onLoaded(List<SavedBarcode> barcodes);
    }

    public interface DeleteCallback {
        void onDeleted();
    }

    public BarcodeRepository(Context context) {
        AppDatabase database =
                DatabaseProvider.getDatabase(context);
        barcodeDao = database.savedBarcodeDao();
    }

    public void save(SavedBarcode barcode, SaveCallback callback) {
        executor.execute(() -> {
            try {
                long id = barcodeDao.insert(barcode);

                if (callback != null) {
                    android.os.Handler mainHandler =
                            new android.os.Handler(
                                    android.os.Looper.getMainLooper());

                    mainHandler.post(() -> callback.onSaved(id));
                }
            } catch (Exception e) {
                if (callback != null) {
                    android.os.Handler mainHandler =
                            new android.os.Handler(
                                    android.os.Looper.getMainLooper());

                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    public void getAll(LoadCallback callback) {
        executor.execute(() -> {
            List<SavedBarcode> barcodes = barcodeDao.getAll();

            new android.os.Handler(
                    android.os.Looper.getMainLooper()
            ).post(() -> callback.onLoaded(barcodes));
        });
    }

    public void updateFavorite(int id, boolean isFavorite, Runnable callback) {
        executor.execute(() -> {
            barcodeDao.updateFavorite(id, isFavorite);

            new android.os.Handler(
                    android.os.Looper.getMainLooper()
            ).post(() -> {
                if (callback != null) {
                    callback.run();
                }
            });
        });
    }

    public void deleteById(int id, DeleteCallback callback) {
        executor.execute(() -> {
            barcodeDao.deleteById(id);

            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper())
                        .post(callback::onDeleted);
            }
        });
    }
}

