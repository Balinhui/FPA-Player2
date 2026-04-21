package org.balinhui.fpaplayer.util;

import org.bytedeco.javacpp.BytePointer;

import static com.portaudio.PortAudio.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class AudioUtil {
    private AudioUtil() {}

    /**
     * 获取相应采样格式的平面格式
     * @param sampleFormat 采样格式 AudioInfo中的sampleFormat
     * @return 传递的sampleFormat对应的平面格式
     */
    public static int getSampleFormatNoPlanar(int sampleFormat) {
        return switch (sampleFormat) {
            case AV_SAMPLE_FMT_U8P -> AV_SAMPLE_FMT_U8;
            case AV_SAMPLE_FMT_S16P -> AV_SAMPLE_FMT_S16;
            case AV_SAMPLE_FMT_S32P -> AV_SAMPLE_FMT_S32;
            case AV_SAMPLE_FMT_FLTP -> AV_SAMPLE_FMT_FLT;
            default -> sampleFormat;
        };
    }

    /**
     * 获取ffmpeg中的采样格式在PortAudio中对应的采样格式（主动转化为平面格式）
     * @param sampleFormat 采样格式 AudioInfo中的sampleFormat
     * @return PortAudio的采样格式
     */
    public static int getPortAudioSampleFormat(int sampleFormat) {
        return switch (sampleFormat) {
            case AV_SAMPLE_FMT_U8, AV_SAMPLE_FMT_U8P -> FORMAT_UINT_8;//不支持
            case AV_SAMPLE_FMT_S16, AV_SAMPLE_FMT_S16P -> FORMAT_INT_16;
            case AV_SAMPLE_FMT_S32, AV_SAMPLE_FMT_S32P -> FORMAT_INT_32;//不支持
            case AV_SAMPLE_FMT_FLT, AV_SAMPLE_FMT_FLTP -> FORMAT_FLOAT_32;
            default -> -1;
        };
    }

    /**
     * 查询当前采样格式是否支持
     * @param sampleFormat 采样格式 AudioInfo中的sampleFormat
     * @return 是否支持，支持为true，不支持为false
     */
    public static boolean isSupport(int sampleFormat) {
        return switch (sampleFormat) {
            case AV_SAMPLE_FMT_S16, AV_SAMPLE_FMT_S16P, AV_SAMPLE_FMT_FLT, AV_SAMPLE_FMT_FLTP -> true;
            default -> false;
        };
    }

    /**
     * 取得int类型采样格式对应的String字符串
     * @param sampleFormat 采样格式 AudioInfo中的sampleFormat
     * @return 采样类型的字符串
     */
    public static String getSampleFormatName(int sampleFormat) {
        try(BytePointer namePointer = av_get_sample_fmt_name(sampleFormat)) {
            return namePointer.getString();
        }
    }
}
