package com.example.simplewallclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.Calendar;

/**
 * Custom View rendering an analog wall clock with:
 * - Solid black outer background
 * - Pure white circular clock dial (#FFFFFF)
 * - Glossy 3D glass dial shine around the clock face (without hand shadows)
 * - Dark charcoal/black outer bezel rim with outline
 * - High-contrast black numbers and radial tick marks
 * - Luxurious warm copper / rose-gold metallic hands & pivot
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

    // Glossy Glass Dial Paints
    private Paint glassSheenPaint;
    private Paint glassVignettePaint;
    private Paint glassRimHighlightPaint;

    private float centerX;
    private float centerY;
    private float clockRadius;

    private final Rect textBounds = new Rect();
    private final Path hourHandPath = new Path();
    private final Path minuteHandPath = new Path();
    private final Path clipDialPath = new Path();

    // Color palette
    private static final int BACKGROUND_COLOR = Color.BLACK;
    private static final int PURE_WHITE_DIAL = Color.WHITE;
    private static final int BEZEL_COLOR = Color.parseColor("#1C1C1C");
    private static final int BEZEL_OUTLINE = Color.parseColor("#D0D0D0"); // Brighter silver-white accent outline

    // Luxurious warm copper / rose-gold metallic material color
    private static final int LUXURY_COPPER_GOLD = Color.parseColor("#D4703B");
    private static final int LUXURY_PIVOT_COLOR = Color.parseColor("#B85A28");

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
        facePaint.setColor(PURE_WHITE_DIAL);
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

        // Luxurious warm copper/rose-gold metallic hands
        handPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        handPaint.setColor(LUXURY_COPPER_GOLD);
        handPaint.setStyle(Paint.Style.FILL);

        secondHandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        secondHandPaint.setColor(LUXURY_COPPER_GOLD);
        secondHandPaint.setStyle(Paint.Style.STROKE);
        secondHandPaint.setStrokeCap(Paint.Cap.ROUND);

        pivotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pivotPaint.setColor(LUXURY_PIVOT_COLOR);
        pivotPaint.setStyle(Paint.Style.FILL);

        // Glass Sheen & Lens Paints
        glassSheenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassSheenPaint.setStyle(Paint.Style.FILL);

        glassVignettePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassVignettePaint.setStyle(Paint.Style.FILL);

        glassRimHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassRimHighlightPaint.setColor(Color.parseColor("#A0FFFFFF"));
        glassRimHighlightPaint.setStyle(Paint.Style.STROKE);
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
        glassRimHighlightPaint.setStrokeWidth(clockRadius * 0.014f);

        // 2. Tick mark stroke widths
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

        // Configure Glass Sheen Linear Gradient across the round dial face
        float innerRadius = clockRadius - (bezelWidth / 2f);
        LinearGradient sheenGradient = new LinearGradient(
                centerX - innerRadius, centerY - innerRadius,
                centerX + innerRadius * 0.8f, centerY + innerRadius * 0.6f,
                new int[]{
                        Color.parseColor("#B0FFFFFF"), // Bright specular sheen at top-left
                        Color.parseColor("#45FFFFFF"), // Mid sheen reflection
                        Color.parseColor("#0AFFFFFF"), // Fading glare
                        Color.parseColor("#00FFFFFF")  // Transparent
                },
                new float[]{0.0f, 0.35f, 0.65f, 1.0f},
                Shader.TileMode.CLAMP
        );
        glassSheenPaint.setShader(sheenGradient);

        // Configure Glass Vignette Inner Shadow Radial Gradient (3D depth shadow around dial rim)
        RadialGradient vignetteGradient = new RadialGradient(
                centerX, centerY, innerRadius,
                new int[]{
                        Color.parseColor("#00000000"), // Clear center
                        Color.parseColor("#08000000"), // Soft inner gradient
                        Color.parseColor("#33000000")  // Pronounced glass depth shadow near bezel
                },
                new float[]{0.0f, 0.80f, 1.0f},
                Shader.TileMode.CLAMP
        );
        glassVignettePaint.setShader(vignetteGradient);

        clipDialPath.reset();
        clipDialPath.addCircle(centerX, centerY, innerRadius, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw solid black screen background
        canvas.drawColor(BACKGROUND_COLOR);

        if (clockRadius <= 0) return;

        // 2. Draw pure white round clock dial face
        float bezelWidth = bezelPaint.getStrokeWidth();
        float innerRadius = clockRadius - (bezelWidth / 2f);
        canvas.drawCircle(centerX, centerY, innerRadius, facePaint);

        // 3. Draw 60 minute tick positions (radial line sticks)
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

        // 4. Draw numbers 1 to 12
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

        // 5. Get local time
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY) % 12;
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millis = calendar.get(Calendar.MILLISECOND);

        float secondAngle = (second + millis / 1000f) * 6f;
        float minuteAngle = (minute + second / 60f + millis / 60000f) * 6f;
        float hourAngle = (hour + minute / 60f + second / 3600f) * 30f;

        // 6. Draw Hour Hand (luxurious warm copper/gold)
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(hourAngle);
        canvas.drawPath(hourHandPath, handPaint);
        canvas.restore();

        // 7. Draw Minute Hand (luxurious warm copper/gold)
        canvas.save();
        canvas.translate(centerX, centerY);
        canvas.rotate(minuteAngle);
        canvas.drawPath(minuteHandPath, handPaint);
        canvas.restore();

        // 8. Draw Second Hand (luxurious warm copper/gold)
        canvas.save();
        canvas.rotate(secondAngle, centerX, centerY);
        float secondHandLength = clockRadius * 0.84f;
        float secondHandTail = clockRadius * 0.14f;
        canvas.drawLine(centerX, centerY + secondHandTail, centerX, centerY - secondHandLength, secondHandPaint);
        canvas.restore();

        // 9. Draw Center Pivot (luxurious warm copper/gold circle)
        float pivotRadius = clockRadius * 0.038f;
        canvas.drawCircle(centerX, centerY, pivotRadius, pivotPaint);

        // 10. Draw Glossy 3D Glass Lens Overlay ON THE ROUND DIAL FACE
        // A) Inner rim depth shadow vignette on round dial face
        canvas.drawCircle(centerX, centerY, innerRadius, glassVignettePaint);

        // B) Specular curved glass glare overlay across upper portion of round dial face
        canvas.save();
        canvas.clipPath(clipDialPath);
        canvas.drawCircle(centerX - innerRadius * 0.2f, centerY - innerRadius * 0.2f, innerRadius * 1.1f, glassSheenPaint);
        canvas.restore();

        // C) Inner glass rim highlight ring around dial perimeter
        canvas.drawCircle(centerX, centerY, innerRadius - (glassRimHighlightPaint.getStrokeWidth() / 2f), glassRimHighlightPaint);

        // 11. Draw outer dark bezel rim & edge outline on top
        canvas.drawCircle(centerX, centerY, innerRadius, bezelPaint);
        canvas.drawCircle(centerX, centerY, clockRadius, bezelOutlinePaint);

        // 12. Schedule next frame
        postInvalidateOnAnimation();
    }
}
