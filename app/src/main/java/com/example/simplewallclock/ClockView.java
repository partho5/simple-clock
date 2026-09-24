package com.example.simplewallclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;

/**
 * Custom View rendering an analog wall clock with:
 * - Solid black screen background
 * - Distinct, true medium-gray round clock dial (#A6A6A6)
 * - Dark charcoal/black outer bezel rim with sharp outline edge
 * - High-contrast black numbers and radial tick marks
 * - Vintage brass golden hands and pivot
 */
public class ClockView extends View {

    private Paint backgroundPaint;
    private Paint bezelPaint;
    private Paint bezelOutlinePaint;
    private Paint facePaint;
    private Paint minuteTickPaint;
    private Paint hourTickPaint;
    private Paint numberPaint;
    private Paint handPaint;
    private Paint secondHandPaint;
    private Paint pivotPaint;

    private float centerX;
    private float centerY;
    private float clockRadius;

    private final Rect textBounds = new Rect();
    private final Path hourHandPath = new Path();
    private final Path minuteHandPath = new Path();

    // Color palette
    private static final int BACKGROUND_COLOR = Color.BLACK;
    private static final int MEDIUM_GRAY_DIAL = Color.parseColor("#A6A6A6"); // True, distinct gray (not white)
    private static final int BEZEL_COLOR = Color.parseColor("#1C1C1C");
    private static final int BEZEL_OUTLINE = Color.parseColor("#444444");
    private static final int BRASS_GOLD_COLOR = Color.parseColor("#D4AF37");
    private static final int BRASS_PIVOT_COLOR = Color.parseColor("#B8860B");

    public ClockView(Context context) {
        super(context);
        init();
    }

    public ClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(BACKGROUND_COLOR);
        backgroundPaint.setStyle(Paint.Style.FILL);

        bezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bezelPaint.setColor(BEZEL_COLOR);
        bezelPaint.setStyle(Paint.Style.STROKE);

        bezelOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bezelOutlinePaint.setColor(BEZEL_OUTLINE);
        bezelOutlinePaint.setStyle(Paint.Style.STROKE);

        facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        facePaint.setColor(MEDIUM_GRAY_DIAL);
        facePaint.setStyle(Paint.Style.FILL);

        minuteTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minuteTickPaint.setColor(Color.BLACK);
        minuteTickPaint.setStyle(Paint.Style.STROKE);
        minuteTickPaint.setStrokeCap(Paint.Cap.ROUND);

        hourTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hourTickPaint.setColor(Color.BLACK);
        hourTickPaint.setStyle(Paint.Style.STROKE);
        hourTickPaint.setStrokeCap(Paint.Cap.ROUND);

        numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numberPaint.setColor(Color.BLACK);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        numberPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        // Vintage Brass/Golden hands
        handPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handPaint.setColor(BRASS_GOLD_COLOR);
        handPaint.setStyle(Paint.Style.FILL);

        secondHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        secondHandPaint.setColor(BRASS_GOLD_COLOR);
        secondHandPaint.setStyle(Paint.Style.STROKE);
        secondHandPaint.setStrokeCap(Paint.Cap.ROUND);

        pivotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pivotPaint.setColor(BRASS_PIVOT_COLOR);
        pivotPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;

        float padding = Math.min(w, h) * 0.04f;
        clockRadius = (Math.min(w, h) / 2f) - padding;

        if (clockRadius <= 0) return;

        // 1. Outer bezel rim stroke width (~8% of clock radius)
        float bezelWidth = clockRadius * 0.08f;
        bezelPaint.setStrokeWidth(bezelWidth);
        bezelOutlinePaint.setStrokeWidth(clockRadius * 0.008f);

        // 2. Refined tick mark stroke widths
        minuteTickPaint.setStrokeWidth(clockRadius * 0.007f);
        hourTickPaint.setStrokeWidth(clockRadius * 0.016f);

        // 3. Bigger numbers font size (~18% of clock radius)
        numberPaint.setTextSize(clockRadius * 0.18f);

        // 4. Second hand stroke width
        secondHandPaint.setStrokeWidth(clockRadius * 0.014f);

        // Build Hour Hand Path (tapered spade shape)
        float hLength = clockRadius * 0.48f;
        float hTail = clockRadius * 0.09f;
        float hBaseW = clockRadius * 0.025f;
        float hShoulderW = clockRadius * 0.060f;
        float hShoulderY = -hLength * 0.75f;

        hourHandPath.reset();
        hourHandPath.moveTo(-hBaseW / 2f, hTail);
        hourHandPath.lineTo(hBaseW / 2f, hTail);
        hourHandPath.lineTo(hShoulderW / 2f, hShoulderY);
        hourHandPath.lineTo(0, -hLength);
        hourHandPath.lineTo(-hShoulderW / 2f, hShoulderY);
        hourHandPath.close();

        // Build Minute Hand Path (longer tapered spade shape)
        float mLength = clockRadius * 0.72f;
        float mTail = clockRadius * 0.11f;
        float mBaseW = clockRadius * 0.020f;
        float mShoulderW = clockRadius * 0.048f;
        float mShoulderY = -mLength * 0.80f;

        minuteHandPath.reset();
        minuteHandPath.moveTo(-mBaseW / 2f, mTail);
        minuteHandPath.lineTo(mBaseW / 2f, mTail);
        minuteHandPath.lineTo(mShoulderW / 2f, mShoulderY);
        minuteHandPath.lineTo(0, -mLength);
        minuteHandPath.lineTo(-mShoulderW / 2f, mShoulderY);
        minuteHandPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw solid black screen background
        canvas.drawColor(BACKGROUND_COLOR);

        if (clockRadius <= 0) return;

        // 2. Draw true medium-gray round clock dial face
        float bezelWidth = bezelPaint.getStrokeWidth();
        float innerRadius = clockRadius - (bezelWidth / 2f);
        canvas.drawCircle(centerX, centerY, innerRadius, facePaint);

        // 3. Draw outer dark bezel rim & subtle edge outline so clock stands out on black background
        canvas.drawCircle(centerX, centerY, innerRadius, bezelPaint);
        canvas.drawCircle(centerX, centerY, clockRadius, bezelOutlinePaint);

        // 4. Draw 60 minute tick positions (radial line sticks)
        float tickOuterY = centerY - innerRadius + (bezelWidth / 2f);
        for (int i = 0; i < 60; i++) {
            boolean isHourTick = (i % 5 == 0);
            float tickLength = isHourTick ? (clockRadius * 0.075f) : (clockRadius * 0.038f);
            Paint p = isHourTick ? hourTickPaint : minuteTickPaint;

            canvas.save();
            canvas.rotate(i * 6f, centerX, centerY);
            canvas.drawLine(centerX, tickOuterY, centerX, tickOuterY + tickLength, p);
            canvas.restore();
        }

        // 5. Draw numbers 1 to 12
        float numberRadius = clockRadius * 0.75f;
        for (int number = 1; number <= 12; number++) {
            double angleRad = Math.PI / 6 * (number - 3);
            float x = (float) (centerX + numberRadius * Math.cos(angleRad));
            float y = (float) (centerY + numberRadius * Math.sin(angleRad));

            String text = String.valueOf(number);
            numberPaint.getTextBounds(text, 0, text.length(), textBounds);
            float correctedY = y + (textBounds.height() / 2f);

            canvas.drawText(text, x, correctedY, numberPaint);
        }

        // 6. Get local time
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY) % 12;
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);

        float secondAngle = (second + millis / 1000f) * 6f;
        float minuteAngle = (minute + second / 60f + millis / 60000f) * 6f;
        float hourAngle = (hour + minute / 60f + second / 3600f) * 30f;

        // 7. Draw Hour Hand (vintage brass/gold)
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(hourAngle);
        canvas.drawPath(hourHandPath, handPaint);
        canvas.restore();

        // 8. Draw Minute Hand (vintage brass/gold)
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(minuteAngle);
        canvas.drawPath(minuteHandPath, handPaint);
        canvas.restore();

        // 9. Draw Second Hand (vintage brass/gold)
        canvas.save();
        canvas.rotate(secondAngle, centerX, centerY);
        float secondHandLength = clockRadius * 0.84f;
        float secondHandTail = clockRadius * 0.14f;
        canvas.drawLine(centerX, centerY + secondHandTail, centerX, centerY - secondHandLength, secondHandPaint);
        canvas.restore();

        // 10. Draw Center Pivot (vintage brass/gold circle)
        float pivotRadius = clockRadius * 0.038f;
        canvas.drawCircle(centerX, centerY, pivotRadius, pivotPaint);

        // 11. Schedule next frame
        postInvalidateOnAnimation();
    }
}
