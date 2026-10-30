package me.theoria.barly.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BarcodeDao {

    @Insert
    long insert(SavedBarcode savedBarcode);

    @Query("SELECT * FROM saved_barcodes ORDER BY createdAt DESC")
    List<SavedBarcode> getAll();

    @Query("DELETE FROM saved_barcodes WHERE id = :id")
    void deleteById(int id);

}
