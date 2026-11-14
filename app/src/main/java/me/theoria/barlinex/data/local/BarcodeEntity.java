package me.theoria.barlinex.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
    
@Entity(tableName = "saved_barcodes")
public class BarcodeEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String barcodeName;
    public String barcodeContent;
    public String barcodeFormat;
    public String barcodeNote;
    public long barcodeCreatedAt;
    public String barcodeCategory;

    public boolean isFavorite = false;

    public BarcodeEntity(String barcodeContent,
                         String barcodeFormat,
                         String barcodeNote,
                         String barcodeCategory) {
        this.barcodeName = null;
        this.barcodeContent = barcodeContent;
        this.barcodeFormat = barcodeFormat;
        this.barcodeNote = barcodeNote;
        this.barcodeCreatedAt = System.currentTimeMillis();
        this.barcodeCategory = barcodeCategory;
    }
}
    

