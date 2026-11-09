package me.theoria.barlinex.data;

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

    public boolean isFavorite = false;

    public BarcodeEntity(String barcodeContent, String barcodeFormat, String barcodeNote) {
        this.barcodeName = null;
        this.barcodeContent = barcodeContent;
        this.barcodeFormat = barcodeFormat;
        this.barcodeNote = barcodeNote;
        this.barcodeCreatedAt = System.currentTimeMillis();
    }
}
    

