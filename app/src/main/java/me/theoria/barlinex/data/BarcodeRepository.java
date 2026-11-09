
package me.theoria.barlinex.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeRepository {

    // Database
    private final BarcodeDao barcodeDao;

    // Executor
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private static final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    // Callbacks
    public interface SaveCallback {
        void onSaved(long id);
        void onError(Exception error);
    }

    public interface LoadCallback {
        void onLoaded(List<BarcodeEntity> barcodes);
        void onError(Exception error);
    }

    public interface DeleteCallback {
        void onDeleted();
        void onError(Exception error);
    }

    public interface OperationCallback {
        void onComplete();
        void onError(Exception error);
    }

    // Initialization
    public BarcodeRepository(Context context) {
        AppDatabase database =
                DatabaseProvider.getDatabase(context);

        barcodeDao = database.savedBarcodeDao();
    }

    // Save
    public void save(BarcodeEntity barcode, SaveCallback callback) {
        executor.execute(() -> {
            try {
                long id = barcodeDao.insert(barcode);

                if (callback != null) {
                    mainHandler.post(() -> callback.onSaved(id));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    // Load
    public void getAll(LoadCallback callback) {
        executor.execute(() -> {
            try {
                List<BarcodeEntity> barcodes = barcodeDao.getAll();

                if (callback != null) {
                    mainHandler.post(() -> callback.onLoaded(barcodes));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    // Update barcodeName
    public void updateName(int id, String name, OperationCallback callback) {
        executor.execute(() -> {
            try {
                barcodeDao.updateName(id, name);

                if (callback != null) {
                    mainHandler.post(callback::onComplete);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    // Update favorite
    public void updateFavorite(
            int id,
            boolean isFavorite,
            OperationCallback callback
    ) {
        executor.execute(() -> {
            try {
                barcodeDao.updateFavorite(id, isFavorite);

                if (callback != null) {
                    mainHandler.post(callback::onComplete);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    // Delete
    public void deleteById(int id, DeleteCallback callback) {
        executor.execute(() -> {
            try {
                barcodeDao.deleteById(id);

                if (callback != null) {
                    mainHandler.post(callback::onDeleted);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    // Cleanup
    public void shutdown() {
        executor.shutdown();
    }
}
