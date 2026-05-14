package org.balinhui.fpaplayer.core;

public class Timer {
    private static double durationSeconds;
    private static long totalSamples;
    private static int playedSamples;
    private static double currentTimeMillis;

    private Timer() {}

    public static synchronized void updateTime(int samples) {
        Timer.playedSamples += samples;
        Timer.currentTimeMillis = (double) playedSamples / totalSamples * durationSeconds * 1000;
    }

    public static void resetTime(double durationSeconds, long totalSamples) {
        Timer.durationSeconds = durationSeconds;
        Timer.totalSamples = totalSamples;
        Timer.playedSamples = 0;
        Timer.currentTimeMillis = 0.0;
    }

    public static synchronized double getCurrentTimeMillis() {
        return currentTimeMillis;
    }
}
