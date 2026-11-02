package me.theoria.barlinex.data;

import android.content.Context;

import androidx.room.Room;

public final class DatabaseProvider {

    private static volatile AppDatabase INSTANCE;

    private DatabaseProvider() {}

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseProvider.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "barlinex_database"
                    )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
