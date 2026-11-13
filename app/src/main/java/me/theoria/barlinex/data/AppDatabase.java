package me.theoria.barlinex.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {BarcodeEntity.class},
        version = 5,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract BarcodeDao savedBarcodeDao();

}