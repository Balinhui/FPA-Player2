package org.balinhui.fpaplayer.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class ThemeColorExtractor {
    private ThemeColorExtractor() {}

    public static Color extractDominantColor(Image image) {
       List<Color> extractColor = extractPalette(image, 3);
       if (!extractColor.isEmpty()) return extractColor.getFirst();
       return Color.WHITE;
    }

    public static List<Color> extractPalette(Image image, int k) {
        PixelReader reader = image.getPixelReader();

        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        List<LabColor> samples = new ArrayList<>();

        //降采样
        int step = Math.max(1, Math.min(width, height) / 120);

        for (int y = 0; y < height; y += step) {
            for (int x = 0; x < width; x += step) {
                Color c = reader.getColor(x, y);

                LabColor lab = rgbToLab(
                        (int) (c.getRed() * 255),
                        (int) (c.getGreen() * 255),
                        (int) (c.getBlue() * 255)
                );

                if (!isGray(lab)) {
                    samples.add(lab);
                }
            }
        }

        List<LabColor> centers = kMeans(samples, k);

        // 排序
        centers.sort((a, b) -> Double.compare(score(b), score(a)));

        List<Color> result = new ArrayList<>();
        for (LabColor lab : centers) {
            result.add(labToColor(lab));
        }

        return result;
    }

    // =========================
    // KMeans
    // =========================

    private static List<LabColor> kMeans(List<LabColor> points, int k) {
        List<LabColor> centers = new ArrayList<>();

        for (int i = 0; i < k && i < points.size(); i++) {
            centers.add(points.get(i));
        }

        for (int iter = 0; iter < 12; iter++) {

            List<LabColor>[] groups = new ArrayList[k];
            for (int i = 0; i < k; i++) groups[i] = new ArrayList<>();

            for (LabColor p : points) {
                int best = 0;
                double bestDist = Double.MAX_VALUE;

                for (int i = 0; i < centers.size(); i++) {
                    double d = dist(p, centers.get(i));
                    if (d < bestDist) {
                        bestDist = d;
                        best = i;
                    }
                }

                groups[best].add(p);
            }

            for (int i = 0; i < k; i++) {
                if (!groups[i].isEmpty()) {
                    centers.set(i, average(groups[i]));
                }
            }
        }

        return centers;
    }

    private static LabColor average(List<LabColor> list) {
        double l = 0, a = 0, b = 0;
        for (LabColor c : list) {
            l += c.l;
            a += c.a;
            b += c.b;
        }
        int n = list.size();
        return new LabColor(l / n, a / n, b / n);
    }

    // =========================
    // 过滤灰色
    // =========================

    private static boolean isGray(LabColor c) {
        return Math.abs(c.a) < 8 && Math.abs(c.b) < 8;
    }

    // =========================
    // 评分（避免背景灰）
    // =========================

    private static double score(LabColor c) {
        double saturation = Math.sqrt(c.a * c.a + c.b * c.b);
        return saturation * c.l;
    }

    // =========================
    // 距离
    // =========================

    private static double dist(LabColor a, LabColor b) {
        double dl = a.l - b.l;
        double da = a.a - b.a;
        double db = a.b - b.b;
        return dl * dl + da * da + db * db;
    }

    // =========================
    // LAB 转换
    // =========================

    private static LabColor rgbToLab(int r, int g, int b) {
        double R = pivotRgb(r / 255.0);
        double G = pivotRgb(g / 255.0);
        double B = pivotRgb(b / 255.0);

        double X = R * 0.4124 + G * 0.3576 + B * 0.1805;
        double Y = R * 0.2126 + G * 0.7152 + B * 0.0722;
        double Z = R * 0.0193 + G * 0.1192 + B * 0.9505;

        X /= 0.95047;
        Y /= 1.00000;
        Z /= 1.08883;

        X = pivotXyz(X);
        Y = pivotXyz(Y);
        Z = pivotXyz(Z);

        double L = 116 * Y - 16;
        double A = 500 * (X - Y);
        double Bv = 200 * (Y - Z);

        return new LabColor(L, A, Bv);
    }

    private static double pivotRgb(double c) {
        return (c > 0.04045)
                ? Math.pow((c + 0.055) / 1.055, 2.4)
                : c / 12.92;
    }

    private static double pivotXyz(double c) {
        return c > 0.008856 ? Math.cbrt(c) : (7.787 * c + 16.0 / 116.0);
    }

    private static Color labToColor(LabColor lab) {
        double y = (lab.l + 16) / 116.0;
        double x = lab.a / 500.0 + y;
        double z = y - lab.b / 200.0;

        x = invPivot(x) * 0.95047;
        y = invPivot(y) * 1.00000;
        z = invPivot(z) * 1.08883;

        double r = x * 3.2406 + y * -1.5372 + z * -0.4986;
        double g = x * -0.9689 + y * 1.8758 + z * 0.0415;
        double b = x * 0.0557 + y * -0.2040 + z * 1.0570;

        r = clamp(rgbGamma(r));
        g = clamp(rgbGamma(g));
        b = clamp(rgbGamma(b));

        return Color.color(r, g, b);
    }

    private static double invPivot(double c) {
        double c3 = c * c * c;
        return c3 > 0.008856 ? c3 : (c - 16.0 / 116.0) / 7.787;
    }

    private static double rgbGamma(double c) {
        return c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055;
    }

    private static double clamp(double v) {
        return Math.clamp(v, 0, 1);
    }

    // =========================
    // 数据结构
    // =========================

    private static class LabColor {
        double l, a, b;

        LabColor(double l, double a, double b) {
            this.l = l;
            this.a = a;
            this.b = b;
        }
    }
}
