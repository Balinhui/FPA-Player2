package org.balinhui.fpaplayer.core;

import java.util.*;

public class CurrentEvents {

    private static final Deque<Event> events = new ArrayDeque<>();

    public static void put(Event event) {
        synchronized (events) {
            events.offerLast(event);
        }
    }

    public static Event poll() {
        synchronized (events) {
            return events.pollFirst();
        }
    }

    public static boolean hasEvents() {
        synchronized (events) {
            return !events.isEmpty();
        }
    }

    public enum Event {
        NEXT, //PREVIOUS
    }
}
