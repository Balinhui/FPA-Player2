package org.balinhui.fpaplayer.core;

public class CurrentStatus {
    private static State state = State.STOPPED;
    private static final Object LOCK = new Object();

    public static void play() {
        synchronized (LOCK) {
            if (state == State.STOPPED || state == State.PAUSED) {
                state = State.PLAYING;
                LOCK.notifyAll();
            }
        }
    }

    public static void pause() {
        synchronized (LOCK) {
            if (state == State.PLAYING) {
                state = State.PAUSED;
            }
        }
    }

    public static void stop() {
        synchronized (LOCK) {
            state = State.STOPPED;
            LOCK.notifyAll();
        }
    }

    public static void close() {
        synchronized (LOCK) {
            state = State.CLOSED;
            LOCK.notifyAll();
        }
    }

    public static boolean waitIfPaused(Player player) throws InterruptedException {
        boolean flag = false;
        synchronized (LOCK) {
            while (state == State.PAUSED) {
                flag = true;
                player.abortStream();
                LOCK.wait();
            }
        }
        return flag;
    }

    public static boolean isPlaying() {
        synchronized (LOCK) {
            return state == State.PLAYING;
        }
    }

    public static boolean isPausing() {
        synchronized (LOCK) {
            return state == State.PAUSED;
        }
    }

    public static boolean isStopped() {
        synchronized (LOCK) {
            return state == State.STOPPED;
        }
    }

    public static boolean isClosed() {
        synchronized (LOCK) {
            return state == State.CLOSED;
        }
    }

    protected static boolean allowDecode() {
        return isPlaying() || isPausing();
    }

    protected static boolean allowPlay() {
        return !isStopped() && !isClosed();
    }

    public enum State {
        PLAYING, STOPPED, PAUSED, CLOSED
    }
}
