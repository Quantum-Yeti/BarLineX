package me.theoria.barlinex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.zxing.BarcodeFormat;

import org.junit.Test;

import me.theoria.barlinex.ui.controller.BarcodeInputController;

public class BarcodeValidatorTest {

    @Test
    public void emptyInputReturnsError() {
        assertEquals(
                "Enter some text first",
                BarcodeInputController.validate(BarcodeFormat.QR_CODE, "")
        );

        assertEquals(
                "Enter some text first",
                BarcodeInputController.validate(BarcodeFormat.CODE_128, "")
        );
    }

    @Test
    public void qrCodeAcceptTextAndUrls() {
        assertNull(BarcodeInputController.validate(
                BarcodeFormat.QR_CODE, "https://google.com"
        ));

        assertNull(BarcodeInputController.validate(
                BarcodeFormat.QR_CODE, "Hello this is a test."
        ));
    }
}
