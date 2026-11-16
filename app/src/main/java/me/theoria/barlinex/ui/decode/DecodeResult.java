package me.theoria.barlinex.ui.decode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecodeResult {

    private final String content;
    private final BarcodeFormat format;
    private final long scannedAt;
    private final Map<ResultMetadataType, Object> metadata;

    public DecodeResult(Result result) {
        this.content = result.getText();
        this.format = result.getBarcodeFormat();
        this.scannedAt = System.currentTimeMillis();

        Map<ResultMetadataType, Object> resultMetadata =
                result.getResultMetadata();

        this.metadata = resultMetadata == null
                ? new HashMap<>()
                : new HashMap<>(resultMetadata);
    }

    public String getContent() {
        return content;
    }

    public BarcodeFormat getFormat() {
        return format;
    }

    public long getScannedAt() {
        return scannedAt;
    }

    public int getCharacterCount() {
        return content.codePointCount(0, content.length());
    }

    public int getByteCount() {
        return content.getBytes(StandardCharsets.UTF_8).length;
    }

    public int getLetterCount() {
        return countCharacters(Character::isLetter);
    }

    public int getNumberCount() {
        return countCharacters(Character::isDigit);
    }

    public int getWhitespaceCount() {
        return countCharacters(Character::isWhitespace);
    }

    public int getSymbolCount() {
        return getCharacterCount()
                - getLetterCount()
                - getNumberCount()
                - getWhitespaceCount();
    }

    private int countCharacters(
            java.util.function.IntPredicate predicate
    ) {
        return (int) content.codePoints()
                .filter(predicate)
                .count();
    }

    public boolean isTwoDimensional() {
        return format == BarcodeFormat.QR_CODE;
    }

    public boolean isProductBarcode() {
        return format == BarcodeFormat.EAN_13
                || format == BarcodeFormat.EAN_8
                || format == BarcodeFormat.UPC_A
                || format == BarcodeFormat.UPC_E;
    }

    public String getErrorCorrectionLevel() {
        Object value = metadata.get(
                ResultMetadataType.ERROR_CORRECTION_LEVEL
        );

        return value == null ? null : value.toString();
    }

    public String getSymbologyIdentifier() {
        Object value = metadata.get(
                ResultMetadataType.SYMBOLOGY_IDENTIFIER
        );

        return value == null ? null : value.toString();
    }

    public int getByteSegmentCount() {
        Object value = metadata.get(
                ResultMetadataType.BYTE_SEGMENTS
        );

        if (!(value instanceof List)) {
            return 0;
        }

        return ((List<?>) value).size();
    }

    public int getByteSegmentSize() {
        Object value = metadata.get(
                ResultMetadataType.BYTE_SEGMENTS
        );

        if (!(value instanceof List)) {
            return 0;
        }

        int total = 0;

        for (Object segment : (List<?>) value) {
            if (segment instanceof byte[]) {
                total += ((byte[]) segment).length;
            }
        }

        return total;
    }

    public boolean hasStructuredAppend() {
        return metadata.containsKey(
                ResultMetadataType.STRUCTURED_APPEND_SEQUENCE
        );
    }

    public String getStructuredAppendSequence() {
        Object value = metadata.get(
                ResultMetadataType.STRUCTURED_APPEND_SEQUENCE
        );

        return value == null ? null : value.toString();
    }

    public String getStructuredAppendParity() {
        Object value = metadata.get(
                ResultMetadataType.STRUCTURED_APPEND_PARITY
        );

        return value == null ? null : value.toString();
    }

    public String getInterpretation() {
        String value = content.trim();

        if (value.isEmpty()) {
            return "Empty content";
        }

        if (value.startsWith("WIFI:")) {
            return "Wi-Fi network information";
        }

        if (value.startsWith("BEGIN:VCARD")
                || value.startsWith("MECARD:")
                || value.startsWith("BIZCARD:")) {
            return "Contact information";
        }

        if (value.startsWith("mailto:")) {
            return "Email address";
        }

        if (value.startsWith("tel:")) {
            return "Telephone number";
        }

        if (value.startsWith("sms:")
                || value.startsWith("smsto:")) {
            return "SMS message";
        }

        if (value.startsWith("geo:")) {
            return "Geographic location";
        }

        if (android.util.Patterns.WEB_URL.matcher(value).matches()) {
            return "Web address";
        }

        if (android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            return "Email address";
        }

        if (android.util.Patterns.PHONE.matcher(value).matches()) {
            return "Telephone number";
        }

        return "Plain text";
    }

    public String getValidationStatus() {
        if (isProductBarcode()) {
            return "Check digit: "
                    + me.theoria.barlinex.domain.barcode.BarcodeValidator
                    .getCheckDigitStatus(content, format);
        }

        if (format == BarcodeFormat.ITF) {
            return content.matches("[0-9]+")
                    && content.length() % 2 == 0
                    ? "Valid numeric structure"
                    : "Unexpected ITF structure";
        }

        if (format == BarcodeFormat.CODE_39) {
            return content.matches("[A-Z0-9 .$/+%-]+")
                    ? "Supported Code 39 characters"
                    : "Contains characters outside the standard Code 39 set";
        }

        return "No check-digit validation available for this format";
    }

    public boolean hasMetadata(ResultMetadataType type) {
        return metadata.containsKey(type);
    }
}
