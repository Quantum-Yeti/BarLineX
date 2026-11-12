package me.theoria.barlinex.ui.scanner;

import android.graphics.RectF;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

public class NumberScanner {

    private final TextRecognizer recognizer =
            TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
            );

    @OptIn(markerClass = ExperimentalGetImage.class)
    public Task<Text> process(ImageProxy imageProxy) {

        if (imageProxy.getImage() == null) {
            throw new IllegalStateException(
                    "Camera frame does not contain an image."
            );
        }

        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees()
        );

        return recognizer.process(image);
    }

    public String extractText(Text result, RectF scanRegion) {

        if (result == null || scanRegion == null) {
            return "";
        }

        StringBuilder extractedText = new StringBuilder();

        for (Text.TextBlock block : result.getTextBlocks()) {

            for (Text.Line line : block.getLines()) {

                if (line.getBoundingBox() == null) {
                    continue;
                }

                RectF lineBounds = new RectF(
                        line.getBoundingBox()
                );

                if (RectF.intersects(lineBounds, scanRegion)) {

                    if (extractedText.length() > 0) {
                        extractedText.append("\n");
                    }

                    extractedText.append(line.getText());
                }
            }
        }

        return extractedText.toString().trim();
    }

    public void close() {
        recognizer.close();
    }
}