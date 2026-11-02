package me.theoria.barlinex.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BarcodeDao {

    @Insert
    long insert(SavedBarcode savedBarcode);

    @Query("SELECT * FROM saved_barcodes ORDER BY isFavorite DESC, createdAt DESC")
    List<SavedBarcode> getAll();

    @Query("UPDATE saved_barcodes SET isFavorite = :isFavorite WHERE id = :id")
    void updateFavorite(int id, boolean isFavorite);

    @Query("DELETE FROM saved_barcodes WHERE id = :id")
    void deleteById(int id);

}
