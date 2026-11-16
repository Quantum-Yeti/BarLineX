package me.theoria.barlinex.ui.decode;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.zxing.Result;

public class DecoderViewModel extends ViewModel {

    private final MutableLiveData<DecodeResult> decodedResult =
            new MutableLiveData<>();

    public LiveData<DecodeResult> getDecodedResult() {
        return decodedResult;
    }

    public void onBarcodeDetected(Result result) {
        if (result == null
                || result.getText() == null
                || result.getBarcodeFormat() == null) {
            return;
        }

        decodedResult.setValue(new DecodeResult(result));
    }

    public void clearResult() {
        decodedResult.setValue(null);
    }

    public boolean hasResult() {
        return decodedResult.getValue() != null;
    }
}
