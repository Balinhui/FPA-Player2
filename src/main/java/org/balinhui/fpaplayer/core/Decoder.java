package org.balinhui.fpaplayer.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpaplayer.FPAControl;
import org.balinhui.fpaplayer.info.OutputInfo;
import org.balinhui.fpaplayer.info.SongInfo;
import org.balinhui.fpaplayer.nativeapis.Global;
import org.balinhui.fpaplayer.nativeapis.MessageFlags;
import org.balinhui.fpaplayer.util.AudioUtil;
import org.balinhui.fpaplayer.util.FlacCoverExtractor;
import org.balinhui.fpaplayer.util.Resample;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.ShortPointer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class Decoder implements Runnable {
    private static final Logger log = LogManager.getLogger(Decoder.class);
    private static final Decoder instance = new Decoder();

    private final Buffer buffer;
    private String[] paths;
    private OutputInfo outputInfo;
    private Event onDecodeFinish;
    private Thread decodeThread;

    private Decoder() {
        buffer = Buffer.getInstance();
        try (BytePointer versionPointer = av_version_info()) {
            log.info("当前FFmpeg版本: {}", versionPointer.getString());
        }
        av_log_set_level(AV_LOG_ERROR);
    }

    public static Decoder getInstance() {
        return instance;
    }

    public SongInfo read(String path) {
        return read(new String[]{path});
    }

    public SongInfo read(String[] paths) {
        this.paths = paths;
        return onlyRead(paths[0]);
    }

    public SongInfo onlyRead(String path) {
        log.trace("解码器读取: {}", path);
        AVFormatContext fmtCtx = new AVFormatContext(null);
        int coverStream = -1;
        AVCodecParameters codecPar = null;
        AVPacket coverPkt;
        try {
            if (avformat_open_input(fmtCtx, path, null, null) < 0) {
                log.error("打开文件失败，是不是在macOS上选择了一个文件夹？");
                Global.messageOf(
                        FPAControl.hWnd,
                        "无法打开文件!",
                        "文件可能已经损坏，或者是在macOS上选择了一个文件夹",
                        MessageFlags.Buttons.OK | MessageFlags.Icons.ERROR
                );
                return null;
            }
            log.trace("打开文件");
            if (avformat_find_stream_info(fmtCtx, (PointerPointer<?>) null) < 0) {
                log.error("找不到流信息");
                throw new RuntimeException("Cant find stream info");
            }
            AVStream stream = null;
            for (int i = 0; i < fmtCtx.nb_streams(); i++) {
                if (fmtCtx.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO &&
                        (fmtCtx.streams(i).disposition() & AV_DISPOSITION_ATTACHED_PIC) != 0) {
                    coverStream = i;
                } else if (fmtCtx.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
                    stream = fmtCtx.streams(i);
                    codecPar = stream.codecpar();
                }
            }
            if (codecPar == null) {
                log.error("没有找到解码器参数，文件格式可能不符");
                //throw new RuntimeException("Doesn't find codec parameter");
                Global.messageOf(
                        FPAControl.hWnd,
                        "错误的文件类型!",
                        "请选择音频文件!!!",
                        MessageFlags.Buttons.OK | MessageFlags.Icons.ERROR
                );
                return null;
            }
            byte[] coverData = null;
            if (coverStream == -1) {
                try {
                    log.info("ffmpeg无法找到封面，尝试通过文件提取");
                    coverData = FlacCoverExtractor.extractFlacCover(path);
                } catch (IOException e) {
                    log.fatal(e.getMessage());
                    throw new RuntimeException(e);
                }
                if (coverData == null)
                    log.error("没有找到封面");
            } else {
                coverPkt = fmtCtx.streams(coverStream).attached_pic();
                if (coverPkt != null && coverPkt.data() != null && coverPkt.size() > 0) {
                    coverData = new byte[coverPkt.size()];
                    coverPkt.data().get(coverData);
                }
            }
            double totalDurationSeconds = fmtCtx.duration() / (double) AV_TIME_BASE;
            return new SongInfo(
                    codecPar.ch_layout().nb_channels(),
                    codecPar.format(),
                    codecPar.sample_rate(),
                    coverData,
                    getMetadata(fmtCtx, stream),
                    totalDurationSeconds,
                    (long) (totalDurationSeconds * codecPar.sample_rate()),
                    av_sample_fmt_is_planar(codecPar.format()) == 1
            );
        } finally {
            avformat_close_input(fmtCtx);
            avformat_free_context(fmtCtx);
            fmtCtx.deallocate();
            log.trace("释放资源");
        }
    }

    /**
     * 读取歌曲元数据
     * @param fmtCtx ffmpeg读取好的音频内容
     * @param stream ffmpeg读取好的音频流
     * @return 包含元数据的键值对
     */
    private Map<String, String> getMetadata(AVFormatContext fmtCtx, AVStream stream) {
        Map<String, String> metadata = new HashMap<>();
        AVDictionary dictionary = stream.metadata() == null ? fmtCtx.metadata() : stream.metadata();
        AVDictionaryEntry entry = null;
        while ((entry = av_dict_get(dictionary, "", entry, AV_DICT_IGNORE_SUFFIX)) != null) {
            metadata.put(
                    entry.key().getString(Charset.defaultCharset()),
                    entry.value().getString(Charset.defaultCharset())
            );
        }
        String[] l = {"lyrics", "LYRICS", "lyrics-XXX"};
        for (String s : l) {
            if (metadata.containsKey(s)) return metadata;
        }
        log.warn("总元数据中没有找到歌词信息，开始重点查找");
        if ((entry = av_dict_get(fmtCtx.metadata(), "lyrics", null, AV_DICT_IGNORE_SUFFIX)) != null) {
            metadata.put(
                    entry.key().getString(Charset.defaultCharset()),
                    entry.value().getString(Charset.defaultCharset())
            );
        }
        return metadata;
    }

    public void start(OutputInfo outputInfo, Event onDecodeFinish) {
        this.outputInfo = outputInfo;
        this.onDecodeFinish = onDecodeFinish;
        if (decodeThread == null || decodeThread.getState() == Thread.State.TERMINATED) {
            decodeThread = new Thread(this);
            decodeThread.setName("Decode Thread");
        }

        if (decodeThread.getState() == Thread.State.NEW) {
            decodeThread.start();
            log.info("解码线程启动");
        }
    }

    @Override
    public void run() {
        CurrentStatus.stateTo(CurrentStatus.States.PLAYING);
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] == null) continue;
            log.trace("解码开始");
            AVFormatContext fmtCtx = new AVFormatContext(null);

            avformat_open_input(fmtCtx, paths[i], null, null);
            avformat_find_stream_info(fmtCtx, (PointerPointer<?>) null);

            int streamIndex = -1;
            AVCodecParameters codecPar = null;
            AVCodec codec = null;
            for (int j = 0; j < fmtCtx.nb_streams(); j++) {
                if (fmtCtx.streams(j).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
                    streamIndex = j;
                    codecPar = fmtCtx.streams(j).codecpar();
                    codec = avcodec_find_decoder(codecPar.codec_id());
                }
            }

            AVCodecContext codecCtx = avcodec_alloc_context3(codec);
            if (codecCtx.isNull()) {
                log.fatal("Doest allocate context");
                throw new RuntimeException("Doest allocate context");
            }
            if (avcodec_parameters_to_context(codecCtx, codecPar) < 0) {
                log.fatal("Cant copy parameters to context");
                throw new RuntimeException("Cant copy parameters to context");
            }
            if (avcodec_open2(codecCtx, codec, (PointerPointer<?>) null) < 0) {
                log.fatal("Cant open decoder");
                throw new RuntimeException("Cant open decoder");
            }

            //受支持的格式只有flt和s16，如果歌曲的格式不是这两种之一，则一定会进行重采样，且格式会统一为flt
            //若不用重采样，歌曲的格式也就受支持，不必担心和portaudio的格式不统一
            //非平面格式也会重采样至平面格式
            //重采样所需
            int srcChannels = codecCtx.ch_layout().nb_channels(), dstChannels;
            int srcSampleFormat = codecCtx.sample_fmt(), dstSampleFormat;
            Resample resample = null;
            final boolean needsResample = outputInfo != null && outputInfo.resample;
            if (needsResample) {
                log.trace("初始化重采样");
                dstChannels = outputInfo.channels;
                dstSampleFormat = outputInfo.sampleFormat;
                resample = new Resample(
                        srcChannels,
                        codecCtx.sample_rate(),
                        srcSampleFormat,
                        outputInfo
                );
            } else {
                dstChannels = srcChannels;
                dstSampleFormat = srcSampleFormat;
            }

            String fmtName = AudioUtil.getSampleFormatName(dstSampleFormat);
            log.trace("歌曲样本格式: {} ", fmtName);

            AVPacket packet = av_packet_alloc();
            AVFrame frame = av_frame_alloc();
            if (packet.isNull() || frame.isNull()) {
                log.fatal("Cant allocate packet or frame");
                throw new RuntimeException("Cant allocate packet or frame");
            }
            BytePointer[] rawData = new BytePointer[1];
            mainloop:while (CurrentStatus.stateIs(CurrentStatus.States.PLAYING) ||
                    CurrentStatus.stateIs(CurrentStatus.States.PAUSE)) {
                if (av_read_frame(fmtCtx, packet) < 0)
                    break;
                if (packet.stream_index() == streamIndex) {
                    int ret = avcodec_send_packet(codecCtx, packet);
                    if (ret < 0)
                        break;
                    while (true) {
                        ret = avcodec_receive_frame(codecCtx, frame);
                        if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF)
                            break;
                        else if (ret < 0) {
                            log.error("解码出错或者结束解码");
                            break mainloop;
                        }

                        int samples = frame.nb_samples();
                        int oldSamples = samples;

                        if (needsResample) {
                            samples = resample.process(rawData, samples, frame.data());
                        } else {
                            rawData[0] = frame.data(0).position(0);
                        }
                        int arraySize = samples * dstChannels;

                        switch (dstSampleFormat) {
                            case AV_SAMPLE_FMT_S16 -> {
                                ShortPointer data = new ShortPointer(rawData[0]);
                                data.get(buffer.putShortData(samples, oldSamples, arraySize), 0, arraySize);
                            }
                            case AV_SAMPLE_FMT_FLT -> {
                                FloatPointer data = new FloatPointer(rawData[0]);
                                data.get(buffer.putFloatData(samples, oldSamples, arraySize), 0, arraySize);
                            }
                        }
                    }
                }
                av_packet_unref(packet);
            }
            log.trace("释放解码资源");
            if (needsResample)
                resample.free();
            avformat_close_input(fmtCtx);
            avformat_free_context(fmtCtx);
            avcodec_free_context(codecCtx);
            av_frame_free(frame);
            av_packet_free(packet);
            if (onDecodeFinish != null) {
                if (CurrentStatus.stateIs(CurrentStatus.States.PLAYING) ||
                 CurrentStatus.stateIs(CurrentStatus.States.NEXT)) {
                    onDecodeFinish.handle(i + 1);//对下一首歌预读
                    if (CurrentStatus.stateIs(CurrentStatus.States.NEXT)) {
                        CurrentStatus.stateTo(CurrentStatus.States.PLAYING);
                        buffer.clear();
                    }
                    buffer.putEndInfo(i + 1);
                } else if (CurrentStatus.stateIs(CurrentStatus.States.CLOSE)) {
                    buffer.clear();
                    break;
                }
            } else if (CurrentStatus.stateIs(CurrentStatus.States.CLOSE)) {
                buffer.clear();
            } else if (CurrentStatus.stateIs(CurrentStatus.States.NEXT)) {
                buffer.clear();
            }
        }
        if (!CurrentStatus.stateIs(CurrentStatus.States.CLOSE))
            CurrentStatus.stateTo(CurrentStatus.States.STOP);
        log.trace("当前解码结束");
    }
}
