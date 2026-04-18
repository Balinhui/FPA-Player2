package org.balinhui.fpaplayer.core;

import java.util.concurrent.atomic.AtomicReference;

public class CurrentStatus {
    private static final AtomicReference<States> state = new AtomicReference<>(States.STOP);

    private static double durationSeconds;

    private static int playedSamples;
    private static double currentTimeSeconds;

    public static synchronized void stateTo(States newState) {
        state.set(newState);
        CurrentStatus.class.notifyAll();
    }

    public static synchronized boolean stateIs(States check) {
        return state.get() == check;
    }

    public static synchronized boolean waitUntilNotPaused() throws InterruptedException {
        boolean flag = false;
        while (stateIs(States.PAUSE)) {
            flag = true;
            CurrentStatus.class.wait();
        }
        return flag;
    }

    public static void setDurationSeconds(double durationSeconds) {
        CurrentStatus.durationSeconds = durationSeconds;
    }

    public static void updateTime(int samples) {
        CurrentStatus.playedSamples += samples;
        CurrentStatus.currentTimeSeconds = (double) playedSamples / durationSeconds;
    }

    public static void resetTime() {
        CurrentStatus.playedSamples = 0;
        CurrentStatus.currentTimeSeconds = 0.0;
    }

    public static double getCurrentTimeSeconds() {
        return currentTimeSeconds;
    }

    public enum States {
        PLAYING, NEXT, PAUSE, STOP, CLOSE
    }
}
