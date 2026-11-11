package me.theoria.barlinex.ui.helper;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;

import java.util.Locale;

import me.theoria.barlinex.R;
import me.theoria.barlinex.ui.controller.BarcodeDecoderController;

public class ZoomControlsController {

    private static final String TAG = "ZoomControlsController";

    private static final float DEFAULT_ZOOM = 1.0f;
    private static final float MAX_DISPLAY_ZOOM = 5.0f;

    private final BarcodeDecoderController decoderController;

    private final View zoomControls;
    private final Slider sliderZoom;
    private final TextView textZoomLevel;
    private final View buttonResetZoom;

    private final ScaleGestureDetector scaleGestureDetector;

    private float maxZoomRatio = DEFAULT_ZOOM;

    public ZoomControlsController(
            AppCompatActivity activity,
            BarcodeDecoderController decoderController
    ) {
        this.decoderController = decoderController;

        zoomControls = activity.findViewById(R.id.zoomControls);
        sliderZoom = activity.findViewById(R.id.sliderZoom);
        textZoomLevel = activity.findViewById(R.id.textZoomLevel);
        buttonResetZoom = activity.findViewById(R.id.buttonResetZoom);

        setupSlider();
        setupResetButton();

        scaleGestureDetector = new ScaleGestureDetector(
                activity,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    @Override
                    public boolean onScale(
                            ScaleGestureDetector detector
                    ) {

                        if (!isVisible()) {
                            return false;
                        }

                        float requestedZoom =
                                decoderController.getZoomRatio()
                                        * detector.getScaleFactor();

                        applyZoom(requestedZoom);

                        return true;
                    }
                }
        );
    }

    // Slider

    private void setupSlider() {

        sliderZoom.addOnChangeListener(
                (slider, value, fromUser) -> {

                    if (fromUser) {
                        applyZoom(value);
                    }
                }
        );
    }

    // Reset button

    private void setupResetButton() {

        buttonResetZoom.setOnClickListener(
                view -> applyZoom(DEFAULT_ZOOM)
        );
    }

    // Zoom operations

    private void applyZoom(float requestedZoom) {

        float appliedZoom =
                decoderController.setZoomRatio(requestedZoom);

        updateZoomDisplay(appliedZoom);
    }

    private void updateZoomDisplay(float zoom) {

        textZoomLevel.setText(
                String.format(
                        Locale.US,
                        "%.1f×",
                        zoom
                )
        );

        if (zoom >= sliderZoom.getValueFrom()
                && zoom <= sliderZoom.getValueTo()) {

            if (Math.abs(sliderZoom.getValue() - zoom) > 0.001f) {
                sliderZoom.setValue(zoom);
            }
        }
    }

    // Camera capabilities

    public void onZoomCapabilitiesChanged(float deviceMaxZoom) {

        maxZoomRatio = Math.min(
                MAX_DISPLAY_ZOOM,
                deviceMaxZoom
        );

        if (maxZoomRatio <= DEFAULT_ZOOM) {
            hide();
            return;
        }

        float currentZoom = Math.min(
                decoderController.getZoomRatio(),
                maxZoomRatio
        );

        // Keep the decoder and slider within the displayed range.
        decoderController.setZoomRatio(currentZoom);

        sliderZoom.setValueTo(maxZoomRatio);
        sliderZoom.setValue(currentZoom);

        updateZoomDisplay(currentZoom);

        show();
    }

    // Pinch gestures

    public void onTouchEvent(MotionEvent event) {

        scaleGestureDetector.onTouchEvent(event);
    }

    // Visibility

    public void show() {

        if (maxZoomRatio > DEFAULT_ZOOM) {
            zoomControls.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {

        zoomControls.setVisibility(View.GONE);
    }

    public boolean isVisible() {

        return zoomControls.getVisibility() == View.VISIBLE;
    }
}
