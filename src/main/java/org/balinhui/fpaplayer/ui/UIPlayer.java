package org.balinhui.fpaplayer.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.control.ProgressBar;
import org.balinhui.fpaplayer.core.CurrentStatus;

import java.util.concurrent.atomic.AtomicLong;

public class UIPlayer {

    private AnimationTimer timer;
    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicLong pausedTime = new AtomicLong(0);

    private final LyricsPane lyricsPane;
    private final ProgressBar progressBar;

    private final double totalTime;
    private volatile long currentPosition = 0;
    private volatile boolean playing = false;

    private static final long UPDATE_INTERVAL_MS = 10;//ms

    private int counter = 0;

    public UIPlayer(double totalTime, LyricsPane lyricsPane, ProgressBar progressBar) {
        this.totalTime = totalTime;
        this.lyricsPane = lyricsPane;
        this.progressBar = progressBar;
        initTimer();
    }

    private void initTimer() {
        timer = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long l) {
                if (!playing) return;

                long currentNanoTime = System.nanoTime();
                long elapsed = (currentNanoTime - startTime.get()) / 1000000;
                currentPosition = elapsed + pausedTime.get();

                if (currentPosition >= totalTime) {
                    currentPosition = (long) totalTime;
                    stop();
                }

                if (l - lastUpdate >= UPDATE_INTERVAL_MS * 1000000) {
                    lastUpdate = l;

                    lyricsPane.scrollToTime(currentPosition);
                    progressBar.setProgress(currentPosition / totalTime);

                    if (counter > 3000) {
                        syncTime((long) CurrentStatus.getCurrentTimeMillis());
                        counter = 0;
                    }
                    counter++;
                }
            }
        };
    }

    public void play() {
        if (playing) return;

        playing = true;
        startTime.set(System.nanoTime());
        pausedTime.set(currentPosition);
        timer.start();
    }

    public void pause() {
        if (!playing) return;

        playing = false;
        timer.stop();

        // 保存暂停时的位置
        long elapsed = (System.nanoTime() - startTime.get()) / 1000000;
        currentPosition = elapsed + pausedTime.get();
        pausedTime.set(currentPosition);
    }

    public void resume() {
        if (playing) return;

        playing = true;
        startTime.set(System.nanoTime());
        timer.start();
    }

    public void stop() {
        playing = false;
        timer.stop();
        currentPosition = 0;
        pausedTime.set(0);
        startTime.set(0);
    }

    /**
     * 同步时间
     */
    public void syncTime(long position) {
        if (position < 0) position = 0;
        if (position > totalTime) position = (long) totalTime;

        this.currentPosition = position;
        this.pausedTime.set(position);

        if (!playing) {
            // 暂停状态下直接更新时间

            lyricsPane.scrollToTime(position);
            progressBar.setProgress(position / totalTime);
        } else {
            // 播放状态下重置计时器起点
            this.startTime.set(System.nanoTime());
        }
    }


    /**
     * 跳转到指定位置(ms)
     */
    public void seekTo(long position) {
        syncTime(position);
    }

    public long getCurrentPosition() {
        if (playing) {
            long elapsed = (System.nanoTime() - startTime.get()) / 1000000;
            return elapsed + pausedTime.get();
        }
        return currentPosition;
    }
}
