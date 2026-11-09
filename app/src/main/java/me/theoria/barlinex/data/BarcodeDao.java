package me.theoria.barlinex.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BarcodeDao {

    @Insert
    long insert(BarcodeEntity barcodeEntity);

    @Query("SELECT * FROM saved_barcodes ORDER BY isFavorite DESC, barcodeCreatedAt DESC")
    List<BarcodeEntity> getAll();

    @Query("UPDATE saved_barcodes SET barcodeName = :name WHERE id = :id")
    void updateName(int id, String name);

    @Query("UPDATE saved_barcodes SET isFavorite = :isFavorite WHERE id = :id")
    void updateFavorite(int id, boolean isFavorite);

    @Query("DELETE FROM saved_barcodes WHERE id = :id")
    void deleteById(int id);

}
