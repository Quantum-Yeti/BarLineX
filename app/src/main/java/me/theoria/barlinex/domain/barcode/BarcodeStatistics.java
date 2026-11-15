package me.theoria.barlinex.domain.barcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.encoder.QRCode;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class BarcodeStatistics {

    private final List<Section> sections;
    private final int characterCount;
    private final int byteCount;

    private BarcodeStatistics(
            List<Section> sections,
            int characterCount,
            int byteCount
    ) {
        this.sections = sections;
        this.characterCount = characterCount;
        this.byteCount = byteCount;
    }

    public List<Section> getSections() {
        return sections;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public int getByteCount() {
        return byteCount;
    }

    public static BarcodeStatistics analyze(
            String content,
            BarcodeFormat format,
            int bitmapWidth,
            int bitmapHeight,
            QRCode qrCode,
            int code128Modules,
            long generatedAt
    ) {
        List<Section> sections = new ArrayList<>();

        int characters = content.codePointCount(
                0,
                content.length()
        );

        int bytes = content.getBytes(
                StandardCharsets.UTF_8
        ).length;

        int letters = 0;
        int digits = 0;
        int whitespace = 0;
        int special = 0;

        Set<Integer> uniqueCharacters = new LinkedHashSet<>();

        for (int i = 0; i < content.length();) {
            int codePoint = content.codePointAt(i);
            uniqueCharacters.add(codePoint);

            if (Character.isLetter(codePoint)) {
                letters++;
            } else if (Character.isDigit(codePoint)) {
                digits++;
            } else if (Character.isWhitespace(codePoint)) {
                whitespace++;
            } else {
                special++;
            }

            i += Character.charCount(codePoint);
        }

        int words = content.trim().isEmpty()
                ? 0
                : content.trim().split("\\s+").length;

        int lines = content.split("\\R", -1).length;

        Section general = new Section("General Statistics");

        general.add("Characters", String.valueOf(characters));
        general.add("UTF-8 Size", bytes + " bytes");
        general.add("Words", String.valueOf(words));
        general.add("Lines", String.valueOf(lines));
        general.add("Unique Characters",
                String.valueOf(uniqueCharacters.size()));

        sections.add(general);

        Section composition = new Section("Character Analysis");

        composition.add("Letters", String.valueOf(letters));
        composition.add("Numbers", String.valueOf(digits));
        composition.add("Special Characters", String.valueOf(special));
        composition.add("Whitespace", String.valueOf(whitespace));

        String characterType;

        if (letters == 0 && digits > 0 && special == 0) {
            characterType = "Numeric";
        } else if (digits == 0 && letters > 0 && special == 0) {
            characterType = "Alphabetic";
        } else if (letters > 0 && digits > 0 && special == 0) {
            characterType = "Alphanumeric";
        } else {
            characterType = "Mixed";
        }

        composition.add("Character Composition", characterType);

        sections.add(composition);

        Section identification = new Section("Content Analysis");

        identification.add(
                "Content Type",
                identifyContentType(content)
        );

        sections.add(identification);

        Section encoding = new Section("Encoding Details");

        encoding.add(
                "Barcode Format",
                format == BarcodeFormat.QR_CODE
                        ? "QR Code"
                        : "CODE 128"
        );

        if (format == BarcodeFormat.QR_CODE && qrCode != null) {
            encoding.add(
                    "Encoding",
                    qrCode.getMode().toString()
            );

            encoding.add(
                    "Error Correction",
                    qrCode.getECLevel().toString()
            );

            encoding.add(
                    "QR Version",
                    String.valueOf(
                            qrCode.getVersion().getVersionNumber()
                    )
            );

            int modules = qrCode.getMatrix().getWidth();

            encoding.add(
                    "Module Dimensions",
                    modules + " × " + modules
            );

            encoding.add(
                    "Mask Pattern",
                    String.valueOf(qrCode.getMaskPattern())
            );

            int dataCodewords =
                    qrCode.getVersion().getTotalCodewords()
                            - qrCode.getVersion()
                            .getECBlocksForLevel(qrCode.getECLevel())
                            .getTotalECCodewords();

            encoding.add(
                    "Data Codewords",
                    dataCodewords + " bytes"
            );

            encoding.add(
                    "Character Encoding",
                    "UTF-8"
            );

        } else if (format == BarcodeFormat.CODE_128) {

            encoding.add(
                    "Code Set",
                    "Automatically selected by ZXing"
            );

            encoding.add(
                    "Checksum",
                    "Modulo 103"
            );

            encoding.add(
                    "Symbol Width",
                    code128Modules + " modules"
            );

            encoding.add(
                    "Character Encoding",
                    "7-bit ASCII"
            );
        }

        sections.add(encoding);

        Section generation = new Section("Generation Details");

        generation.add(
                "Image Dimensions",
                bitmapWidth + " × " + bitmapHeight + " px"
        );

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "MMM d, yyyy · h:mm a",
                Locale.getDefault()
        );

        generation.add(
                "Generated",
                dateFormat.format(new Date(generatedAt))
        );

        sections.add(generation);

        return new BarcodeStatistics(
                sections,
                characters,
                bytes
        );
    }

    private static String identifyContentType(String content) {

        String value = content.trim();

        if (value.isEmpty()) {
            return "Empty";
        }

        if (value.matches("(?i)^https?://.+")) {
            return "URL";
        }

        if (value.matches(
                "(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$"
        )) {
            return "Email Address";
        }

        if (value.matches("^\\+?[0-9() .-]{7,}$")) {
            return "Phone Number";
        }

        if (value.matches("(?i)^WIFI:.*")) {
            return "Wi-Fi Configuration";
        }

        if (value.matches("^geo:.*")) {
            return "Geographic Location";
        }

        if (value.matches("(?i)^mailto:.*")) {
            return "Email Link";
        }

        if (value.matches("(?i)^tel:.*")) {
            return "Telephone Link";
        }

        if (value.matches("(?i)^sms:.*")) {
            return "SMS Link";
        }

        if (value.matches("(?i)^BEGIN:VCARD.*")) {
            return "Contact Card";
        }

        return "Plain Text";
    }

    public static final class Section {

        private final String title;
        private final List<Metric> metrics = new ArrayList<>();

        Section(String title) {
            this.title = title;
        }

        void add(String label, String value) {
            metrics.add(new Metric(label, value));
        }

        public String getTitle() {
            return title;
        }

        public List<Metric> getMetrics() {
            return metrics;
        }
    }

    public static final class Metric {

        private final String label;
        private final String value;

        Metric(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }
}
