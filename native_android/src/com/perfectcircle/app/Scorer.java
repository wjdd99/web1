package com.perfectcircle.app;

public final class Scorer {
    public static final class Result {
        public final boolean valid;
        public final String invalidReason;
        public final float score;
        public final float centerX;
        public final float centerY;
        public final float meanRadius;
        public final float stdDev;
        public final float closureError;

        public Result(boolean valid, String reason, float score, float cx, float cy,
                      float meanRadius, float stdDev, float closureError) {
            this.valid = valid;
            this.invalidReason = reason;
            this.score = score;
            this.centerX = cx;
            this.centerY = cy;
            this.meanRadius = meanRadius;
            this.stdDev = stdDev;
            this.closureError = closureError;
        }

        public static Result invalid(String reason) {
            return new Result(false, reason, 0f, 0f, 0f, 0f, 0f, 0f);
        }
    }

    private Scorer() {}

    public static Result score(float[] xs, float[] ys, int count) {
        if (count < 30) {
            return Result.invalid("Draw a longer circle");
        }
        double sumX = 0, sumY = 0;
        for (int i = 0; i < count; i++) {
            sumX += xs[i];
            sumY += ys[i];
        }
        float cx = (float) (sumX / count);
        float cy = (float) (sumY / count);

        double sumR = 0;
        float[] radii = new float[count];
        for (int i = 0; i < count; i++) {
            float dx = xs[i] - cx;
            float dy = ys[i] - cy;
            float r = (float) Math.sqrt(dx * dx + dy * dy);
            radii[i] = r;
            sumR += r;
        }
        float meanR = (float) (sumR / count);
        if (meanR < 30f) {
            return Result.invalid("Circle is too small");
        }

        double varSum = 0;
        for (int i = 0; i < count; i++) {
            double d = radii[i] - meanR;
            varSum += d * d;
        }
        float std = (float) Math.sqrt(varSum / count);
        float roundnessError = std / meanR;

        float dx = xs[0] - xs[count - 1];
        float dy = ys[0] - ys[count - 1];
        float gap = (float) Math.sqrt(dx * dx + dy * dy);
        float closureError = gap / meanR;

        double roundnessScore = 100.0 * Math.exp(-roundnessError * 9.0);
        double closurePenalty = Math.min(closureError / 0.15, 1.0) * 25.0;
        double raw = roundnessScore - closurePenalty;
        float clamped = (float) Math.max(0.0, Math.min(100.0, raw));

        return new Result(true, null, clamped, cx, cy, meanR, std, closureError);
    }

    public static String grade(float score) {
        if (score >= 98) return "S";
        if (score >= 92) return "A";
        if (score >= 80) return "B";
        if (score >= 65) return "C";
        if (score >= 45) return "D";
        return "F";
    }

    public static String gradeMessage(float score) {
        if (score >= 98) return "Inhuman. Are you a compass?";
        if (score >= 92) return "Masterful circle!";
        if (score >= 80) return "Very round.";
        if (score >= 65) return "Decent attempt.";
        if (score >= 45) return "Needs more practice.";
        return "Was that a circle?";
    }

    public static int gradeColor(float score) {
        if (score >= 98) return 0xFFFFD166;
        if (score >= 92) return 0xFF06D6A0;
        if (score >= 80) return 0xFF5B8DEF;
        if (score >= 65) return 0xFF118AB2;
        if (score >= 45) return 0xFFEF476F;
        return 0xFFBF3145;
    }
}
