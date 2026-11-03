package me.theoria.barlinex.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
    
@Entity(tableName = "saved_barcodes")
public class SavedBarcode {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String content;
    public String format;
    public long createdAt;

    public boolean isFavorite = false;

    public SavedBarcode(String content, String format) {
        this.name = null;
        this.content = content;
        this.format = format;
        this.createdAt = System.currentTimeMillis();
    }
}
    

