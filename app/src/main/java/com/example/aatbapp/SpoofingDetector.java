package com.example.aatbapp;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Client-side anti-spoofing checks run before sending an image to the server.
 * Catches obvious screen/paper spoofs early to save a round-trip.
 */
public class SpoofingDetector {

    public static class Result {
        public final boolean spoofed;
        public final String reason;
        Result(boolean spoofed, String reason) {
            this.spoofed = spoofed;
            this.reason  = reason;
        }
    }

    /**
     * Run all checks on a captured bitmap.
     * Returns a Result indicating whether spoofing was detected.
     */
    public static Result analyze(Bitmap bitmap) {
        if (hasScreenGlare(bitmap))   return new Result(true, "Screen glare detected");
        if (hasFlatTexture(bitmap))   return new Result(true, "Flat texture detected (printed paper or screen)");
        if (hasBanding(bitmap))       return new Result(true, "Banding pattern detected (screen)");
        return new Result(false, "");
    }

    // ── Checks ───────────────────────────────────────────────────────────────

    /** >25% of pixels are near-white (blown-out screen light) */
    private static boolean hasScreenGlare(Bitmap bmp) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        int total = w * h, bright = 0;
        int step = 4; // sample every 4th pixel for speed
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                int px = bmp.getPixel(x, y);
                int r = Color.red(px), g = Color.green(px), b = Color.blue(px);
                if (r > 240 && g > 240 && b > 240) bright++;
            }
        }
        int sampled = (w / step) * (h / step);
        return (float) bright / sampled > 0.25f;
    }

    /**
     * Low Laplacian variance = flat surface (paper/screen).
     * Real plates have embossed characters with sharp edges.
     */
    private static boolean hasFlatTexture(Bitmap bmp) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        int[] gray = toGrayscaleArray(bmp, w, h);
        double variance = laplacianVariance(gray, w, h);
        return variance < 40.0;
    }

    /**
     * Detect horizontal banding (moire) by checking row-brightness periodicity.
     * Screens often produce alternating bright/dark scan lines.
     */
    private static boolean hasBanding(Bitmap bmp) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        double[] rowBrightness = new double[h];
        for (int y = 0; y < h; y++) {
            long sum = 0;
            for (int x = 0; x < w; x++) {
                int px = bmp.getPixel(x, y);
                sum += (Color.red(px) + Color.green(px) + Color.blue(px)) / 3;
            }
            rowBrightness[y] = (double) sum / w;
        }
        // Count zero-crossings of the derivative — many crossings = periodic banding
        int crossings = 0;
        for (int i = 1; i < h - 1; i++) {
            double prev = rowBrightness[i] - rowBrightness[i - 1];
            double next = rowBrightness[i + 1] - rowBrightness[i];
            if (prev * next < 0) crossings++;
        }
        // More than 30% of rows alternating = suspicious
        return (float) crossings / h > 0.30f;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static int[] toGrayscaleArray(Bitmap bmp, int w, int h) {
        int[] gray = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int px = bmp.getPixel(x, y);
                gray[y * w + x] = (Color.red(px) + Color.green(px) + Color.blue(px)) / 3;
            }
        }
        return gray;
    }

    private static double laplacianVariance(int[] gray, int w, int h) {
        // 3x3 Laplacian kernel: [0,1,0],[1,-4,1],[0,1,0]
        double sum = 0, sumSq = 0;
        int count = 0;
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int lap = -4 * gray[y * w + x]
                        + gray[(y - 1) * w + x]
                        + gray[(y + 1) * w + x]
                        + gray[y * w + (x - 1)]
                        + gray[y * w + (x + 1)];
                sum   += lap;
                sumSq += (double) lap * lap;
                count++;
            }
        }
        double mean = sum / count;
        return (sumSq / count) - (mean * mean);
    }
}
