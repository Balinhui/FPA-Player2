package org.balinhui.fpaplayer.core;

import java.util.concurrent.atomic.AtomicReference;

public class CurrentStatus {
    private static final AtomicReference<States> state = new AtomicReference<>(States.STOP);

    public static synchronized void stateTo(States newState) {
        States tmp = state.get();
        state.set(newState);
        if (tmp == States.PAUSE)
            CurrentStatus.class.notify();
    }

    public static synchronized boolean stateIs(States check) {
        return state.get() == check;
    }

    public static synchronized boolean waitUntilNotPaused(Player player) throws InterruptedException {
        boolean flag = false;
        while (stateIs(States.PAUSE)) {
            flag = true;
            player.abortStream();
            CurrentStatus.class.wait();
        }
        return flag;
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
