package org.balinhui.fpaplayer.core;

import java.util.concurrent.atomic.AtomicReference;

public class CurrentStatus {
    private static final AtomicReference<States> state = new AtomicReference<>(States.STOP);

    public void to(States newState) {
        state.set(newState);
    }

    public boolean is(States check) {
        return state.get() == check;
    }

    public enum States {
        PLAYING, PAUSE, STOP, CLOSE
    }
}
