package me.theoria.barlinex.ui.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

public class ScannerOverlay extends View {

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint instructionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF guideRect = new RectF();

    private static final int NONE = 0;
    private static final int TOP_LEFT = 1;
    private static final int TOP_RIGHT = 2;
    private static final int BOTTOM_LEFT = 3;
    private static final int BOTTOM_RIGHT = 4;
    private static final int MOVE = 5;

    private int activeTouch = NONE;

    private float lastTouchX;
    private float lastTouchY;

    private boolean frameInitialized = false;

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
        setClickable(true);

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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w <= 0 || h <= 0) {
            return;
        }

        if (!frameInitialized) {

            float frameWidth = w * 0.84f;

            float frameHeight = Math.min(
                    h * 0.34f,
                    frameWidth * 0.38f
            );

            float centerX = w / 2f;
            float centerY = h * 0.46f;

            guideRect.set(
                    centerX - frameWidth / 2f,
                    centerY - frameHeight / 2f,
                    centerX + frameWidth / 2f,
                    centerY + frameHeight / 2f
            );

            frameInitialized = true;
        }
    }

    /**
     * Returns a copy of the current scanning frame.
     */
    public RectF getGuideRect() {
        return new RectF(guideRect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        if (width == 0 || height == 0 || !frameInitialized) {
            return;
        }

        // Darken the camera preview.
        int saveCount = canvas.saveLayer(
                0, 0, width, height, null
        );

        canvas.drawRect(0, 0, width, height, dimPaint);

        // Keep the scanning area transparent.
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
                "Drag corners to resize",
                width / 2f,
                instructionY,
                instructionPaint
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                activeTouch = getTouchedArea(x, y);

                if (activeTouch == NONE) {
                    return false;
                }

                lastTouchX = x;
                lastTouchY = y;

                return true;

            case MotionEvent.ACTION_MOVE:

                float dx = x - lastTouchX;
                float dy = y - lastTouchY;

                if (activeTouch == MOVE) {
                    moveFrame(dx, dy);
                } else {
                    resizeFrame(x, y);
                }

                lastTouchX = x;
                lastTouchY = y;

                invalidate();

                return true;

            case MotionEvent.ACTION_UP:

                activeTouch = NONE;
                performClick();

                return true;

            case MotionEvent.ACTION_CANCEL:

                activeTouch = NONE;
                return true;
        }

        return true;
    }

    private int getTouchedArea(float x, float y) {

        float touchRadius = dp(40);

        // Check corners first.
        if (isNear(x, y, guideRect.left, guideRect.top, touchRadius)) {
            return TOP_LEFT;
        }

        if (isNear(x, y, guideRect.right, guideRect.top, touchRadius)) {
            return TOP_RIGHT;
        }

        if (isNear(x, y, guideRect.left, guideRect.bottom, touchRadius)) {
            return BOTTOM_LEFT;
        }

        if (isNear(x, y, guideRect.right, guideRect.bottom, touchRadius)) {
            return BOTTOM_RIGHT;
        }

        // Touching inside the frame moves it.
        if (guideRect.contains(x, y)) {
            return MOVE;
        }

        return NONE;
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

        float newLeft = guideRect.left + dx;
        float newTop = guideRect.top + dy;

        float frameWidth = guideRect.width();
        float frameHeight = guideRect.height();

        newLeft = Math.max(
                0,
                Math.min(newLeft, getWidth() - frameWidth)
        );

        newTop = Math.max(
                0,
                Math.min(newTop, getHeight() - frameHeight)
        );

        guideRect.set(
                newLeft,
                newTop,
                newLeft + frameWidth,
                newTop + frameHeight
        );
    }

    private void resizeFrame(float x, float y) {

        float minWidth = dp(120);
        float minHeight = dp(55);

        float maxWidth = getWidth();
        float maxHeight = getHeight();

        switch (activeTouch) {

            case TOP_LEFT:

                guideRect.left = Math.max(
                        0,
                        Math.min(x, guideRect.right - minWidth)
                );

                guideRect.top = Math.max(
                        0,
                        Math.min(y, guideRect.bottom - minHeight)
                );

                break;

            case TOP_RIGHT:

                guideRect.right = Math.min(
                        maxWidth,
                        Math.max(x, guideRect.left + minWidth)
                );

                guideRect.top = Math.max(
                        0,
                        Math.min(y, guideRect.bottom - minHeight)
                );

                break;

            case BOTTOM_LEFT:

                guideRect.left = Math.max(
                        0,
                        Math.min(x, guideRect.right - minWidth)
                );

                guideRect.bottom = Math.min(
                        maxHeight,
                        Math.max(y, guideRect.top + minHeight)
                );

                break;

            case BOTTOM_RIGHT:

                guideRect.right = Math.min(
                        maxWidth,
                        Math.max(x, guideRect.left + minWidth)
                );

                guideRect.bottom = Math.min(
                        maxHeight,
                        Math.max(y, guideRect.top + minHeight)
                );

                break;
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
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
