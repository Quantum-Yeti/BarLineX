package me.theoria.barlinex.ui.scanner;

import android.content.Context;
import android.net.Uri;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberScanner {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    public interface Listener {
        void onScanResult(String number);
        void onScanError(Exception error);
        void onScanComplete();
    }

    public void scan(Context context, Uri imageUri, Listener listener) {

        TextRecognizer recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
        );

        try {
            InputImage image = InputImage.fromFilePath(context, imageUri);

            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String number = findBestNumber(result.getText());
                        listener.onScanResult(number);
                    })
                    .addOnFailureListener(listener::onScanError)
                    .addOnCompleteListener(task -> {
                        recognizer.close();
                        listener.onScanComplete();
                    });

        } catch (IOException | RuntimeException e) {
            recognizer.close();
            listener.onScanError(e);
            listener.onScanComplete();
        }
    }

    private String findBestNumber(String recognizedText) {

        if (recognizedText == null || recognizedText.isEmpty()) {
            return "";
        }

        Matcher matcher = NUMBER_PATTERN.matcher(recognizedText);
        String bestNumber = "";

        while (matcher.find()) {
            String candidate = matcher.group();

            if (candidate.length() > bestNumber.length()) {
                bestNumber = candidate;
            }
        }

        return bestNumber;
    }
}

