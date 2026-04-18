package org.balinhui.fpaplayer.info;

import org.balinhui.fpaplayer.util.AudioUtil;

public class OutputInfo {
    public boolean resample;
    public int channels;
    public int sampleRate;
    public int sampleFormat;

    public OutputInfo(boolean resample, int channels, int sampleRate, int sampleFormat) {
        this.resample = resample;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.sampleFormat = sampleFormat;
    }

    @Override
    public String toString() {
        return "OutputInfo{" +
                "resample=" + resample +
                ", channels=" + channels +
                ", sampleRate=" + sampleRate +
                ", sampleFormat=" + AudioUtil.getSampleFormatName(sampleFormat) +
                '}';
    }
}
