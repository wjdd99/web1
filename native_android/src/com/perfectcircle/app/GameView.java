package com.perfectcircle.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {
    private static final String PREFS = "perfect_circle";
    private static final String KEY_BEST = "best_score_v1";
    private static final int MAX_POINTS = 4096;

    private final float[] xs = new float[MAX_POINTS];
    private final float[] ys = new float[MAX_POINTS];
    private int count = 0;

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint refPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gradePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Scorer.Result result;
    private float bestScore;
    private final SharedPreferences prefs;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        bestScore = prefs.getFloat(KEY_BEST, 0f);

        setBackgroundColor(0xFF0F1115);
        strokePaint.setColor(0xFF5B8DEF);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(8f);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        refPaint.setColor(0xFFFFD166);
        refPaint.setStyle(Paint.Style.STROKE);
        refPaint.setStrokeWidth(3f);

        guidePaint.setColor(0x66FFFFFF);
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(2f);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setFakeBoldText(true);

        gradePaint.setTextSize(140f);
        gradePaint.setFakeBoldText(true);

        subPaint.setColor(0xFFBBBBBB);
        subPaint.setTextSize(28f);

        cardPaint.setColor(0xFF1E2030);
        cardPaint.setStyle(Paint.Style.FILL);

        bestPaint.setColor(0xFFFFD166);
        bestPaint.setTextSize(32f);
        bestPaint.setFakeBoldText(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                count = 0;
                result = null;
                addPoint(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                addPoint(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                addPoint(x, y);
                result = Scorer.score(xs, ys, count);
                if (result.valid) {
                    if (result.score > bestScore) {
                        bestScore = result.score;
                        prefs.edit().putFloat(KEY_BEST, bestScore).apply();
                    }
                    if (result.score >= 80) {
                        vibrate(60);
                    } else {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                } else {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void addPoint(float x, float y) {
        if (count >= MAX_POINTS) return;
        xs[count] = x;
        ys[count] = y;
        count++;
    }

    private void vibrate(long ms) {
        Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            v.vibrate(ms);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;

        canvas.drawCircle(cx, cy, 8f, guidePaint);
        canvas.drawLine(cx - 18f, cy, cx + 18f, cy, guidePaint);
        canvas.drawLine(cx, cy - 18f, cx, cy + 18f, guidePaint);

        canvas.drawText("Perfect Circle", 32f, 70f, textPaint);
        String bestText = "Best " + format1(bestScore);
        float btW = bestPaint.measureText(bestText);
        canvas.drawText(bestText, getWidth() - btW - 32f, 70f, bestPaint);

        if (count < 2 && result == null) {
            canvas.drawText("Draw a circle with one finger", 32f, getHeight() / 2f + 100f, subPaint);
        }

        if (count >= 2) {
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(xs[0], ys[0]);
            for (int i = 1; i < count; i++) {
                path.lineTo(xs[i], ys[i]);
            }
            canvas.drawPath(path, strokePaint);
        }

        if (result != null && result.valid) {
            canvas.drawCircle(result.centerX, result.centerY, result.meanRadius, refPaint);

            float cardW = getWidth() - 64f;
            float cardH = 320f;
            float left = 32f;
            float top = getHeight() - cardH - 32f;
            RectF card = new RectF(left, top, left + cardW, top + cardH);
            canvas.drawRoundRect(card, 28f, 28f, cardPaint);

            gradePaint.setColor(Scorer.gradeColor(result.score));
            String grade = Scorer.grade(result.score);
            float gw = gradePaint.measureText(grade);
            canvas.drawText(grade, left + 50f - gw / 4f, top + 150f, gradePaint);

            textPaint.setColor(Color.WHITE);
            canvas.drawText(format1(result.score), left + 200f, top + 110f, textPaint);
            subPaint.setColor(0xFFBBBBBB);
            canvas.drawText(Scorer.gradeMessage(result.score), left + 200f, top + 150f, subPaint);

            canvas.drawText("Mean radius: " + format1(result.meanRadius) + " px",
                    left + 30f, top + 210f, subPaint);
            canvas.drawText("Roundness σ: " + format2(result.stdDev),
                    left + 30f, top + 245f, subPaint);
            canvas.drawText("Closure: " + format1(result.closureError * 100f) + " %",
                    left + 30f, top + 280f, subPaint);
        } else if (result != null && !result.valid) {
            float cardW = getWidth() - 64f;
            float cardH = 180f;
            float left = 32f;
            float top = getHeight() - cardH - 32f;
            RectF card = new RectF(left, top, left + cardW, top + cardH);
            canvas.drawRoundRect(card, 28f, 28f, cardPaint);
            textPaint.setColor(Color.WHITE);
            canvas.drawText("Try again", left + 30f, top + 70f, textPaint);
            subPaint.setColor(0xFFBBBBBB);
            canvas.drawText(result.invalidReason, left + 30f, top + 120f, subPaint);
        }
    }

    private static String format1(float f) {
        return String.format(java.util.Locale.US, "%.1f", f);
    }

    private static String format2(float f) {
        return String.format(java.util.Locale.US, "%.2f", f);
    }
}
