package com.example.simplewallclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;

/**
 * Custom View rendering a clean, minimal, full-screen analog wall clock.
 */
public class ClockView extends View {

    private Paint backgroundPaint;
    private Paint borderPaint;
    private Paint minuteTickPaint;
    private Paint hourTickPaint;
    private Paint numberPaint;
    private Paint hourHandPaint;
    private Paint minuteHandPaint;
    private Paint secondHandPaint;
    private Paint pivotPaint;

    private float centerX;
    private float centerY;
    private float clockRadius;

    private final Rect textBounds = new Rect();

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
        backgroundPaint.setColor(Color.WHITE);
        backgroundPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);

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

        hourHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hourHandPaint.setColor(Color.BLACK);
        hourHandPaint.setStyle(Paint.Style.STROKE);
        hourHandPaint.setStrokeCap(Paint.Cap.ROUND);

        minuteHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minuteHandPaint.setColor(Color.BLACK);
        minuteHandPaint.setStyle(Paint.Style.STROKE);
        minuteHandPaint.setStrokeCap(Paint.Cap.ROUND);

        secondHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        secondHandPaint.setColor(Color.BLACK);
        secondHandPaint.setStyle(Paint.Style.STROKE);
        secondHandPaint.setStrokeCap(Paint.Cap.ROUND);

        pivotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pivotPaint.setColor(Color.BLACK);
        pivotPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;

        float padding = Math.min(w, h) * 0.05f;
        clockRadius = (Math.min(w, h) / 2f) - padding;

        if (clockRadius <= 0) return;

        // Dynamic stroke widths & font dimensions relative to radius
        borderPaint.setStrokeWidth(clockRadius * 0.035f);
        minuteTickPaint.setStrokeWidth(clockRadius * 0.012f);
        hourTickPaint.setStrokeWidth(clockRadius * 0.024f);

        numberPaint.setTextSize(clockRadius * 0.15f);

        hourHandPaint.setStrokeWidth(clockRadius * 0.045f);
        minuteHandPaint.setStrokeWidth(clockRadius * 0.030f);
        secondHandPaint.setStrokeWidth(clockRadius * 0.014f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw background
        canvas.drawColor(Color.WHITE);

        if (clockRadius <= 0) return;

        // 2. Draw circular outer border
        canvas.drawCircle(centerX, centerY, clockRadius, borderPaint);

        // 3. Draw 60 minute tick positions (small radial line segments / sticks)
        // Each tick line is rotated at angle (i * 6 degrees) pointing directly towards the center.
        for (int i = 0; i < 60; i++) {
            boolean isHourTick = (i % 5 == 0);
            float tickLength = isHourTick ? (clockRadius * 0.08f) : (clockRadius * 0.04f);
            Paint paint = isHourTick ? hourTickPaint : minuteTickPaint;

            canvas.save();
            // Rotate canvas around center by exact minute angle (i * 6 degrees)
            canvas.rotate(i * 6f, centerX, centerY);
            
            // Calculate top radial line segment coordinates from just inside border inward
            float startY = centerY - clockRadius + (borderPaint.getStrokeWidth() / 2f);
            float endY = startY + tickLength;
            
            // Draw straight stick line segment
            canvas.drawLine(centerX, startY, centerX, endY, paint);
            canvas.restore();
        }

        // 4. Draw numbers 1-12
        float numberRadius = clockRadius * 0.77f;
        for (int number = 1; number <= 12; number++) {
            // Angle in radians (12 is top, so offset by -PI/2 or number - 3)
            double angleRad = Math.PI / 6 * (number - 3);
            float x = (float) (centerX + numberRadius * Math.cos(angleRad));
            float y = (float) (centerY + numberRadius * Math.sin(angleRad));

            String text = String.valueOf(number);
            numberPaint.getTextBounds(text, 0, text.length(), textBounds);
            // Center vertical alignment via font metrics offset
            float correctedY = y + (textBounds.height() / 2f);

            canvas.drawText(text, x, correctedY, numberPaint);
        }

        // 5. Calculate smooth hand angles from local time
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY) % 12;
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);

        float secondAngle = (second + millis / 1000f) * 6f; // 360deg / 60sec = 6deg/sec
        float minuteAngle = (minute + second / 60f + millis / 60000f) * 6f; // 360deg / 60min = 6deg/min
        float hourAngle = (hour + minute / 60f + second / 3600f) * 30f; // 360deg / 12hr = 30deg/hr

        // 6. Draw Hour Hand
        canvas.save();
        canvas.rotate(hourAngle, centerX, centerY);
        float hourHandLength = clockRadius * 0.50f;
        float hourHandTail = clockRadius * 0.08f;
        canvas.drawLine(centerX, centerY + hourHandTail, centerX, centerY - hourHandLength, hourHandPaint);
        canvas.restore();

        // 7. Draw Minute Hand
        canvas.save();
        canvas.rotate(minuteAngle, centerX, centerY);
        float minuteHandLength = clockRadius * 0.72f;
        float minuteHandTail = clockRadius * 0.10f;
        canvas.drawLine(centerX, centerY + minuteHandTail, centerX, centerY - minuteHandLength, minuteHandPaint);
        canvas.restore();

        // 8. Draw Second Hand
        canvas.save();
        canvas.rotate(secondAngle, centerX, centerY);
        float secondHandLength = clockRadius * 0.85f;
        float secondHandTail = clockRadius * 0.12f;
        canvas.drawLine(centerX, centerY + secondHandTail, centerX, centerY - secondHandLength, secondHandPaint);
        canvas.restore();

        // 9. Draw Center Pivot
        float pivotRadius = clockRadius * 0.035f;
        canvas.drawCircle(centerX, centerY, pivotRadius, pivotPaint);

        // 10. Schedule continuous 60 FPS animation frame
        postInvalidateOnAnimation();
    }
}
