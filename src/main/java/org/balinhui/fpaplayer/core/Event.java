package org.balinhui.fpaplayer.core;

@FunctionalInterface
public interface Event {
    void handle(int v);
}
