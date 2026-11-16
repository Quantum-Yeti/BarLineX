
package me.theoria.barlinex.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.theoria.barlinex.data.local.BarcodeEntity;
import me.theoria.barlinex.data.DatabaseProvider;
import me.theoria.barlinex.data.local.BarcodeDatabase;
import me.theoria.barlinex.data.local.BarcodeDao;

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
        BarcodeDatabase database =
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

    public void updateBarcodeDetails(
            int id,
            String name,
            String content,
            String format,
            String note,
            String category,
            SaveCallback callback
    ) {
        executor.execute(() -> {
            try {
                int rowsUpdated = barcodeDao.updateBarcodeDetails(
                        id,
                        name,
                        content,
                        format,
                        note,
                        category
                );

                mainHandler.post(() -> {
                    if (rowsUpdated > 0) {
                        if (callback != null) {
                            callback.onSaved(id);
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(
                                    new Exception("Barcode record could not be updated.")
                            );
                        }
                    }
                });

            } catch (Exception exception) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(exception);
                    }
                });
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
