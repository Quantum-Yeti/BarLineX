package me.theoria.barlinex.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class ScannerOverlay extends View {

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint instructionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF guideRect = new RectF();

    public ScannerOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ScannerOverlay(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        dimPaint.setColor(0x99000000);

        clearPaint.setXfermode(
                new PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        );

        cornerPaint.setColor(0xFF8B9EFF);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(dp(4));
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setStrokeJoin(Paint.Join.ROUND);

        instructionPaint.setColor(0xFFFFFFFF);
        instructionPaint.setTextSize(sp(13));
        instructionPaint.setTextAlign(Paint.Align.CENTER);
        instructionPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        if (width == 0 || height == 0) {
            return;
        }

        float frameWidth = width * 0.84f;
        float frameHeight = Math.min(
                height * 0.34f,
                frameWidth * 0.38f
        );

        float centerX = width / 2f;
        float centerY = height * 0.46f;

        guideRect.set(
                centerX - frameWidth / 2f,
                centerY - frameHeight / 2f,
                centerX + frameWidth / 2f,
                centerY + frameHeight / 2f
        );

        // Darken the camera preview.
        int saveCount = canvas.saveLayer(
                0,
                0,
                width,
                height,
                null
        );

        canvas.drawRect(0, 0, width, height, dimPaint);

        // Clear the center so the camera remains visible.
        canvas.drawRoundRect(
                guideRect,
                dp(12),
                dp(12),
                clearPaint
        );

        canvas.restoreToCount(saveCount);

        // Draw four corner brackets.
        float cornerLength = dp(30);
        float left = guideRect.left;
        float right = guideRect.right;
        float top = guideRect.top;
        float bottom = guideRect.bottom;

        // Top-left
        canvas.drawLine(left, top + cornerLength, left, top, cornerPaint);
        canvas.drawLine(left, top, left + cornerLength, top, cornerPaint);

        // Top-right
        canvas.drawLine(right - cornerLength, top, right, top, cornerPaint);
        canvas.drawLine(right, top, right, top + cornerLength, cornerPaint);

        // Bottom-left
        canvas.drawLine(left, bottom - cornerLength, left, bottom, cornerPaint);
        canvas.drawLine(left, bottom, left + cornerLength, bottom, cornerPaint);

        // Bottom-right
        canvas.drawLine(right - cornerLength, bottom, right, bottom, cornerPaint);
        canvas.drawLine(right, bottom - cornerLength, right, bottom, cornerPaint);

        // Instruction below the guide.
        float instructionY = Math.min(
                bottom + dp(34),
                height - dp(20)
        );

        canvas.drawText(
                "Align barcode within frame",
                centerX,
                instructionY,
                instructionPaint
        );
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}

