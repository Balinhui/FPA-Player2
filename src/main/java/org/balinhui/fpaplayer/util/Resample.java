package org.balinhui.fpaplayer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpaplayer.info.OutputInfo;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_UP;
import static org.bytedeco.ffmpeg.global.avutil.av_freep;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_rnd;
import static org.bytedeco.ffmpeg.global.avutil.av_samples_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_samples_get_buffer_size;
import static org.bytedeco.ffmpeg.global.swresample.*;

public class Resample {
    private static final Logger logger = LogManager.getLogger(Resample.class);
    private final int dstChannels;
    private final int dstSampleFormat;
    private final int srcSampleRate, dstSampleRate;
    private int maxDstSamples = -1, dstSamples = -1;
    private final PointerPointer<BytePointer> dstData;
    private final IntPointer linSize;
    private final SwrContext swrCtx;

    public Resample(
            int srcChannels, int srcSampleRate, int srcSampleFormat, OutputInfo info
    ) {
        this.srcSampleRate = srcSampleRate;
        this.dstChannels = info.channels;
        this.dstSampleRate = info.sampleRate;
        this.dstSampleFormat = info.sampleFormat;
        dstData = new PointerPointer<>((Pointer) null);
        linSize = new IntPointer();
        AVChannelLayout srcLayout = new AVChannelLayout().nb_channels(srcChannels);
        AVChannelLayout dstLayout = new AVChannelLayout().nb_channels(dstChannels);
        swrCtx = swr_alloc();
        if (swrCtx == null) {
            logger.fatal("SwrContext分配失败");
            throw new RuntimeException("SwrContext分配失败");
        }
        int ret = swr_alloc_set_opts2(
                swrCtx,
                dstLayout, dstSampleFormat, dstSampleRate,
                srcLayout, srcSampleFormat, srcSampleRate,
                0, null
        );
        if (ret < 0) {
            logger.fatal("设置选项失败");
            throw new RuntimeException("设置选项失败");
        }
        ret = swr_init(swrCtx);
        if (ret < 0) {
            logger.fatal("SwrContext初始化失败");
            throw new RuntimeException("SwrContext初始化失败");
        }
        av_channel_layout_uninit(srcLayout);
        av_channel_layout_uninit(dstLayout);
    }

    public int process(BytePointer[] rawData, int samples, PointerPointer<?> srcData) {
        int ret;
        if (dstSamples == -1) {
            dstSamples = (int) av_rescale_rnd(samples, dstSampleRate, srcSampleRate, AV_ROUND_UP);
            maxDstSamples = dstSamples;
            ret = av_samples_alloc_array_and_samples(dstData, linSize, dstChannels, dstSamples, dstSampleFormat, 0);
            if (ret < 0) {
                logger.fatal("初始分配内存失败");
                throw new RuntimeException("分配内存失败");
            }
        }
        dstSamples = (int) av_rescale_rnd(swr_get_delay(swrCtx, srcSampleRate) + samples, dstSampleRate, srcSampleRate, AV_ROUND_UP);
        if (dstSamples > maxDstSamples) {
            av_freep(dstData.position(0));
            ret = av_samples_alloc(dstData, linSize, dstChannels, dstSamples, dstSampleFormat, 1);
            if (ret < 0) {
                logger.fatal("分配内存失败");
                throw new RuntimeException("分配内存失败");
            }
            maxDstSamples = dstSamples;
        }
        int newSamples = swr_convert(swrCtx, dstData, dstSamples, srcData, samples);
        int bufferSize = av_samples_get_buffer_size(linSize, dstChannels, newSamples, dstSampleFormat, 1);
        rawData[0] = dstData.get(BytePointer.class, 0);
        rawData[0].position(0).limit(bufferSize);
        return newSamples;
    }

    public void free() {
        if (!dstData.isNull()) {
            av_freep(dstData.position(0));
            av_freep(dstData);
        }
        linSize.deallocate();
        dstData.deallocate();
        swr_free(swrCtx);
        dstSamples = -1;
        maxDstSamples = -1;
        logger.trace("释放重采样资源");
    }
}
