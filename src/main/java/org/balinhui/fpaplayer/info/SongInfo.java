package org.balinhui.fpaplayer.info;

import java.util.Map;

public class SongInfo {
    public int channels;
    public int sampleFormat;
    public int sampleRate;
    public byte[] cover;
    public Map<String, String> metadata;
    public float durationSeconds;
    public boolean isPlanar;

    public SongInfo(int channels, int sampleFormat, int sampleRate, byte[] cover, Map<String, String> metadata, float durationSeconds, boolean isPlanar) {
        this.channels = channels;
        this.sampleFormat = sampleFormat;
        this.sampleRate = sampleRate;
        this.cover = cover;
        this.metadata = metadata;
        this.durationSeconds = durationSeconds;
        this.isPlanar = isPlanar;
    }

    @Override
    public String toString() {
        return "AudioInfo{" +
                "channels=" + channels +
                ", sampleFormat=" + sampleFormat +
                ", sampleRate=" + sampleRate +
                ", metadata=" + metadata +
                ", durationSeconds=" + durationSeconds +
                '}';
    }
}
