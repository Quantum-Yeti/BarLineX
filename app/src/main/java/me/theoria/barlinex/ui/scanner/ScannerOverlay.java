package me.theoria.barlinex.ui.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ScannerOverlay extends View {

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scanLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint instructionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF guideRect = new RectF();

    private boolean frameInitialized = false;

    private static final int ACCENT_COLOR = 0xFF5278FF;
    private static final int LIGHT_ACCENT = 0xFF91A8FF;

    private static final float CORNER_LENGTH_DP = 28f;
    private static final float CORNER_RADIUS_DP = 14f;
    private static final float MIN_FRAME_WIDTH_DP = 120f;
    private static final float MIN_FRAME_HEIGHT_DP = 55f;
    private static final float TOUCH_RADIUS_DP = 40f;

    private enum TouchMode {
        NONE,
        MOVE,
        RESIZE
    }

    private TouchMode touchMode = TouchMode.NONE;

    private float lastTouchX;
    private float lastTouchY;

    public ScannerOverlay(Context context) {
        super(context);
        initialize();
    }

    public ScannerOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ScannerOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Background dimming
        dimPaint.setColor(0x55000000);
        dimPaint.setStyle(Paint.Style.FILL);

        // Transparent scanner window
        clearPaint.setColor(0xFFFFFFFF);
        clearPaint.setStyle(Paint.Style.FILL);
        clearPaint.setXfermode(
                new PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        );

        // Scanner corner brackets
        cornerPaint.setColor(ACCENT_COLOR);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(dp(3f));
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setStrokeJoin(Paint.Join.ROUND);

        // Subtle corner glow
        glowPaint.setColor(ACCENT_COLOR);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dp(6f));
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);
        glowPaint.setShadowLayer(dp(7f), 0f, 0f, ACCENT_COLOR);

        // Animated scan line
        scanLinePaint.setColor(LIGHT_ACCENT);
        scanLinePaint.setStyle(Paint.Style.STROKE);
        scanLinePaint.setStrokeWidth(dp(1.5f));
        scanLinePaint.setStrokeCap(Paint.Cap.ROUND);
        scanLinePaint.setShadowLayer(dp(5f), 0f, 0f, ACCENT_COLOR);

        // Scanner instructions
        instructionPaint.setColor(0xFFE0E0E0);
        instructionPaint.setTextSize(sp(12f));
        instructionPaint.setTextAlign(Paint.Align.CENTER);
        instructionPaint.setAntiAlias(true);

    }

    @Override
    protected void onSizeChanged(
            int width,
            int height,
            int oldWidth,
            int oldHeight
    ) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        if (!frameInitialized && width > 0 && height > 0) {

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

            frameInitialized = true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!frameInitialized) {
            return;
        }

        // Dim the camera preview
        canvas.drawRect(
                0f,
                0f,
                getWidth(),
                getHeight(),
                dimPaint
        );

        // Cut out the scanner window
        canvas.drawRoundRect(
                guideRect,
                dp(CORNER_RADIUS_DP),
                dp(CORNER_RADIUS_DP),
                clearPaint
        );

        // Draw scanner frame
        drawScannerFrame(canvas);

        // Draw animated scanning line
        drawScanLine(canvas);

        // Scanner instructions
        drawInstruction(canvas);

        // Continue animation while visible
        if (getVisibility() == VISIBLE) {
            postInvalidateOnAnimation();
        }
    }

    private void drawScannerFrame(Canvas canvas) {

        float left = guideRect.left;
        float top = guideRect.top;
        float right = guideRect.right;
        float bottom = guideRect.bottom;

        float cornerLength = dp(CORNER_LENGTH_DP);
        float radius = dp(CORNER_RADIUS_DP);

        // Glow layer
        drawCornerLines(
                canvas,
                left,
                top,
                right,
                bottom,
                cornerLength,
                radius,
                glowPaint
        );

        // Main corner layer
        drawCornerLines(
                canvas,
                left,
                top,
                right,
                bottom,
                cornerLength,
                radius,
                cornerPaint
        );
    }

    private void drawCornerLines(
            Canvas canvas,
            float left,
            float top,
            float right,
            float bottom,
            float cornerLength,
            float radius,
            Paint paint
    ) {

        // Top-left
        canvas.drawLine(
                left,
                top + cornerLength,
                left,
                top + radius,
                paint
        );

        canvas.drawLine(
                left + radius,
                top,
                left + cornerLength,
                top,
                paint
        );

        // Top-right
        canvas.drawLine(
                right - cornerLength,
                top,
                right - radius,
                top,
                paint
        );

        canvas.drawLine(
                right,
                top + radius,
                right,
                top + cornerLength,
                paint
        );

        // Bottom-left
        canvas.drawLine(
                left,
                bottom - cornerLength,
                left,
                bottom - radius,
                paint
        );

        canvas.drawLine(
                left + radius,
                bottom,
                left + cornerLength,
                bottom,
                paint
        );

        // Bottom-right
        canvas.drawLine(
                right - cornerLength,
                bottom,
                right - radius,
                bottom,
                paint
        );

        canvas.drawLine(
                right,
                bottom - radius,
                right,
                bottom - cornerLength,
                paint
        );
    }

    private void drawScanLine(Canvas canvas) {

        float scanHeight = guideRect.height();

        long animationDuration = 2200L;
        long elapsed = android.os.SystemClock.uptimeMillis() % animationDuration;

        float progress = elapsed / (float) animationDuration;

        // Ping-pong movement
        float movement = progress < 0.5f
                ? progress * 2f
                : (1f - progress) * 2f;

        float scanY = guideRect.top + scanHeight * movement;

        float horizontalInset = dp(14f);

        float startX = guideRect.left + horizontalInset;
        float endX = guideRect.right - horizontalInset;

        // Soft scan line
        canvas.drawLine(
                startX,
                scanY,
                endX,
                scanY,
                scanLinePaint
        );
    }

    private void drawInstruction(Canvas canvas) {

        float textY = guideRect.bottom + dp(32f);

        String instruction = "Align barcode within the frame";

        canvas.drawText(
                instruction,
                getWidth() / 2f,
                textY,
                instructionPaint
        );
    }

    /**
     * Returns a copy of the scanner guide rectangle.
     * BarcodeDecoderController uses this rectangle
     * to determine the image crop region.
     */
    public RectF getGuideRect() {
        return new RectF(guideRect);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!frameInitialized) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                lastTouchX = x;
                lastTouchY = y;

                if (isNearCorner(x, y)) {
                    touchMode = TouchMode.RESIZE;
                } else if (guideRect.contains(x, y)) {
                    touchMode = TouchMode.MOVE;
                } else {
                    touchMode = TouchMode.NONE;
                    return false;
                }

                return true;

            case MotionEvent.ACTION_MOVE:

                float dx = x - lastTouchX;
                float dy = y - lastTouchY;

                if (touchMode == TouchMode.MOVE) {
                    moveFrame(dx, dy);
                } else if (touchMode == TouchMode.RESIZE) {
                    resizeFrame(dx, dy);
                }

                lastTouchX = x;
                lastTouchY = y;

                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                touchMode = TouchMode.NONE;
                performClick();
                return true;
        }

        return true;
    }

    private boolean isNearCorner(float x, float y) {

        float radius = dp(TOUCH_RADIUS_DP);

        return isNear(x, y, guideRect.left, guideRect.top, radius)
                || isNear(x, y, guideRect.right, guideRect.top, radius)
                || isNear(x, y, guideRect.left, guideRect.bottom, radius)
                || isNear(x, y, guideRect.right, guideRect.bottom, radius);
    }

    private boolean isNear(
            float x,
            float y,
            float cornerX,
            float cornerY,
            float radius
    ) {

        float dx = x - cornerX;
        float dy = y - cornerY;

        return dx * dx + dy * dy <= radius * radius;
    }

    private void moveFrame(float dx, float dy) {

        float width = guideRect.width();
        float height = guideRect.height();

        float newLeft = guideRect.left + dx;
        float newTop = guideRect.top + dy;

        // Keep the frame inside the overlay
        newLeft = Math.max(
                0f,
                Math.min(getWidth() - width, newLeft)
        );

        newTop = Math.max(
                0f,
                Math.min(getHeight() - height, newTop)
        );

        guideRect.set(
                newLeft,
                newTop,
                newLeft + width,
                newTop + height
        );
    }

    private void resizeFrame(float dx, float dy) {

        float minWidth = dp(MIN_FRAME_WIDTH_DP);
        float minHeight = dp(MIN_FRAME_HEIGHT_DP);

        float newRight = guideRect.right + dx;
        float newBottom = guideRect.bottom + dy;

        newRight = Math.max(
                guideRect.left + minWidth,
                Math.min(getWidth(), newRight)
        );

        newBottom = Math.max(
                guideRect.top + minHeight,
                Math.min(getHeight(), newBottom)
        );

        guideRect.right = newRight;
        guideRect.bottom = newBottom;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private float dp(float value) {
        return value * getResources()
                .getDisplayMetrics()
                .density;
    }

    private float sp(float value) {
        return value * getResources()
                .getDisplayMetrics()
                .scaledDensity;
    }
}
