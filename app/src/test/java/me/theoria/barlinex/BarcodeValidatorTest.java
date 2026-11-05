package me.theoria.barlinex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.zxing.BarcodeFormat;

import org.junit.Test;

import me.theoria.barlinex.domain.validator.BarcodeValidator;
import me.theoria.barlinex.ui.controller.BarcodeInputController;
import me.theoria.barlinex.ui.viewmodel.BarcodeViewModel;

public class BarcodeValidatorTest {

    @Test
    public void emptyInputReturnsError() {
        assertEquals(
                "Enter some text first",
                BarcodeValidator.validate(BarcodeFormat.QR_CODE, "")
        );

        assertEquals(
                "Enter some text first",
                BarcodeValidator.validate(BarcodeFormat.CODE_128, "")
        );
    }

    @Test
    public void qrCodeAcceptTextAndUrls() {
        assertNull(BarcodeValidator.validate(
                BarcodeFormat.QR_CODE, "https://google.com"
        ));

        assertNull(BarcodeValidator.validate(
                BarcodeFormat.QR_CODE, "Hello this is a test."
        ));
    }
}
