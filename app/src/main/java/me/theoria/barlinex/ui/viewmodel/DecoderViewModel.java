package me.theoria.barlinex.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.zxing.BarcodeFormat;

import me.theoria.barlinex.ui.helper.DecodeResult;

public class DecoderViewModel extends ViewModel {

    private final MutableLiveData<DecodeResult> decodedResult =
            new MutableLiveData<>();

    public LiveData<DecodeResult> getDecodedResult() {
        return decodedResult;
    }

    public void onBarcodeDetected(
            String content,
            BarcodeFormat format
    ) {
        if (content == null || format == null) {
            return;
        }

        decodedResult.setValue(
                new DecodeResult(content, format)
        );
    }

    public void clearResult() {
        decodedResult.setValue(null);
    }

    public boolean hasResult() {
        return decodedResult.getValue() != null;
    }
}