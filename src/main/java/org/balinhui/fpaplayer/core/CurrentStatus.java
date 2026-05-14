package org.balinhui.fpaplayer.core;

import java.util.concurrent.atomic.AtomicReference;

public class CurrentStatus {
    private static final AtomicReference<States> state = new AtomicReference<>(States.STOP);

    protected static synchronized void stateTo(States newState) {
        States tmp = state.get();
        state.set(newState);
        if (tmp == States.PAUSE)
            CurrentStatus.class.notify();
    }

    protected static synchronized boolean stateIs(States check) {
        return state.get() == check;
    }

    protected static synchronized boolean waitUntilNotPaused(Player player) throws InterruptedException {
        boolean flag = false;
        while (stateIs(States.PAUSE)) {
            flag = true;
            player.abortStream();
            CurrentStatus.class.wait();
        }
        return flag;
    }

    public static boolean isPlaying() {
        return stateIs(States.PLAYING);
    }

    public static boolean isPausing() {
        return stateIs(States.PAUSE);
    }

    public static boolean isStopping() {
        return stateIs(States.STOP);
    }

    public static void pause() {
        stateTo(States.PAUSE);
    }

    public static void play() {
        stateTo(States.PLAYING);
    }

    protected static boolean allowDecode() {
        return stateIs(States.PLAYING) || stateIs(States.PAUSE);
    }

    protected static boolean allowPlay() {
        return !stateIs(States.STOP) && !stateIs(States.CLOSE);
    }

    public static void setNext() {
        if (stateIs(States.PLAYING) || stateIs(States.PAUSE))
            stateTo(States.NEXT);
    }

    public static void closeApp() {
        if (stateIs(States.PLAYING) || stateIs(States.PAUSE))
            stateTo(States.CLOSE);
    }

    public enum States {
        //指示播放中状态
        PLAYING,
        //当点击下一首时，设置为此状态
        NEXT, 
        //当暂停时，设置为此状态
        PAUSE,
        //当未处于播放时在此状态
        STOP,
        //关闭窗口时指示此状态
        CLOSE
    }
}
