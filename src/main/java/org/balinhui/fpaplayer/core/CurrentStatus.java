package org.balinhui.fpaplayer.core;

import org.balinhui.fpaplayer.info.SongInfo;

import java.util.concurrent.atomic.AtomicReference;

public class CurrentStatus {
    private static final AtomicReference<States> state = new AtomicReference<>(States.STOP);

    private static SongInfo currentSong;
    private static int playedSamples;
    private static double currentTimeSeconds;

    public static void stateTo(States newState) {
        state.set(newState);
        CurrentStatus.class.notifyAll();
    }

    public static boolean stateIs(States check) {
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

    public static void setCurrentSong(SongInfo info) {
        currentSong = info;
    }

    public static SongInfo getCurrentSong() {
        return currentSong;
    }

    public static void updateTime(int samples) {
        playedSamples += samples;
        currentTimeSeconds = (double) playedSamples / currentSong.durationSeconds;
    }

    public static void resetTime() {
        playedSamples = 0;
        currentTimeSeconds = 0.0;
    }

    public static double getCurrentTimeSeconds() {
        return currentTimeSeconds;
    }

    public enum States {
        PLAYING, NEXT, PAUSE, STOP, CLOSE
    }
}
