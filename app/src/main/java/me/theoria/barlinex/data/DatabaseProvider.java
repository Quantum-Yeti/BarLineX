package me.theoria.barlinex.data;

import android.content.Context;

import androidx.room.Room;

import me.theoria.barlinex.data.local.BarcodeDatabase;

public final class DatabaseProvider {

    private static volatile BarcodeDatabase INSTANCE;

    private DatabaseProvider() {}

    public static BarcodeDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseProvider.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            BarcodeDatabase.class,
                            "barlinex_database"
                    )
                            .fallbackToDestructiveMigration(true)
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
