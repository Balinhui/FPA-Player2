package org.balinhui.fpaplayer.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpaplayer.info.OutputInfo;
import org.balinhui.fpaplayer.info.SongInfo;
import org.balinhui.fpaplayer.nativeapis.MessageFlags;
import org.balinhui.fpaplayer.nativeapis.NativeAPI;
import org.balinhui.fpaplayer.util.AudioUtil;
import org.balinhui.fpaplayer.util.ErrorHandler;
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
import java.util.*;

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
                NativeAPI.displayMessage(
                        "无法打开文件!",
                        "文件可能已经损坏，或者是在macOS上选择了一个文件夹",
                        MessageFlags.Buttons.OK | MessageFlags.Icons.ERROR
                );
                return null;
            }
            if (avformat_find_stream_info(fmtCtx, (PointerPointer<?>) null) < 0) {
                log.error("找不到流信息");
                ErrorHandler.displayErrorMessage((Exception) null, "Cant find stream info");
                return null;
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
                NativeAPI.displayMessage(
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
                    ErrorHandler.displayErrorMessage(e, null);
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
        }
    }

    /**
     * 读取歌曲元数据，并选择更优的元数据
     * @param fmtCtx ffmpeg读取好的音频内容
     * @param stream ffmpeg读取好的音频流
     * @return 包含元数据的键值对
     */
    private Map<String, String> getMetadata(AVFormatContext fmtCtx, AVStream stream) {
        Map<String, List<String>> metadata = new HashMap<>();
        addMetadataToMap(fmtCtx.metadata(), metadata);
        if (stream.metadata() != null) {
            addMetadataToMap(stream.metadata(), metadata);
        }

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, List<String>> e : metadata.entrySet()) {
            String best = e.getValue().stream()
                    .max(Comparator.comparingInt(this::scoreMetadata))
                    .orElse(null);

            if (best != null) {
                result.put(e.getKey(), best);
            } else {
                result.put(e.getKey(), e.getValue().getFirst());
            }
        }

        return result;
    }

    private void addMetadataToMap(AVDictionary dict, Map<String, List<String>> target) {
        AVDictionaryEntry entry = null;
        while ((entry = av_dict_get(dict, "", entry, AV_DICT_IGNORE_SUFFIX)) != null) {
            String key = entry.key().getString();
            String value = entry.value().getString();

            if (key != null && value != null) {
                target.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
        }
    }

    private int scoreMetadata(String s) {
        if (s == null || s.isBlank()) return Integer.MIN_VALUE;

        int score = 0;
        if (s.contains("\uFFFD")) score -= 100;

        for (char c : s.toCharArray()) {
            if (
                    Character.isLetterOrDigit(c)
                    || Character.isWhitespace(c)
                    || Character.isIdeographic(c)
                    || Character.isAlphabetic(c)
            ) {
                score += 2;
            }
        }

        for (char c : s.toCharArray()) {
            if (Character.isISOControl(c)) {
                score -= 2;
            }
        }

        log.trace("{} 得分情况: {}", s, score);

        return score;
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
        CurrentStatus.play();
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
                ErrorHandler.displayErrorMessageAndExit((Exception) null, "Doest allocate context", -5);
            }
            if (avcodec_parameters_to_context(codecCtx, codecPar) < 0) {
                log.fatal("Cant copy parameters to context");
                ErrorHandler.displayErrorMessageAndExit((Exception) null,
                        "Cant copy parameters to context", -5);
            }
            if (avcodec_open2(codecCtx, codec, (PointerPointer<?>) null) < 0) {
                log.fatal("Cant open decoder");
                ErrorHandler.displayErrorMessageAndExit((Exception) null, "Cant open decoder", -5);
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
                ErrorHandler.displayErrorMessageAndExit((Exception) null, "Cant allocate packet or frame",
                        -5);
            }
            boolean draining = false;
            BytePointer[] rawData = new BytePointer[1];
            mainLoop: while (CurrentStatus.allowDecode()) {
                if (CurrentEvents.hasEvents() && CurrentEvents.poll() == CurrentEvents.Event.NEXT) {
                    buffer.clearArray();
                    break;
                }
                int ret;
                if (av_read_frame(fmtCtx, packet) < 0) {
                    draining = true;
                    avcodec_send_packet(codecCtx, null);
                } else if (packet.stream_index() == streamIndex) {
                    ret = avcodec_send_packet(codecCtx, packet);
                    if (ret < 0 && ret != AVERROR_EAGAIN()) {
                        log.error("发送包失败, {}", ret);
                        av_packet_unref(packet);
                        break;
                    }
                } else {
                    av_packet_unref(packet);
                    continue;
                }

                while (true) {
                    ret = avcodec_receive_frame(codecCtx, frame);
                    if (ret == AVERROR_EOF() || ret == AVERROR_EAGAIN()) {
                        if (draining) break mainLoop;
                        av_frame_unref(frame);
                        break;
                    }
                    if (ret < 0) {
                        log.error("接受帧失败, {}", ret);
                        if (ret == AVERROR_INVALIDDATA()) {
                            log.warn("跳过无效数据");
                            av_frame_unref(frame);
                            continue;
                        }

                        if (draining) break mainLoop;
                        av_frame_unref(frame);
                        av_packet_unref(packet);
                        break mainLoop;
                    }
                    int samples = frame.nb_samples();
                    if (needsResample) {
                        samples = resample.process(rawData, samples, frame.data());
                    } else {
                        rawData[0] = frame.data(0);
                    }
                    int arraySize = samples * dstChannels;

                    switch (dstSampleFormat) {
                        case AV_SAMPLE_FMT_S16 -> {
                            ShortPointer data = new ShortPointer(rawData[0]);
                            data.get(buffer.putShortData(samples, frame.nb_samples(), arraySize), 0, arraySize);
                        }
                        case AV_SAMPLE_FMT_FLT -> {
                            FloatPointer data = new FloatPointer(rawData[0]);
                            data.get(buffer.putFloatData(samples, frame.nb_samples(), arraySize), 0, arraySize);
                        }
                    }
                    av_frame_unref(frame);
                }
                av_packet_unref(packet);
            }

            log.trace("释放解码资源");
            if (needsResample)
                resample.free();
            avformat_close_input(fmtCtx);
            avcodec_free_context(codecCtx);
            av_frame_free(frame);
            av_packet_free(packet);

            if (onDecodeFinish != null) {//onDecodeFinish不为null，说明有多首歌需要播放
                if (CurrentStatus.isPlaying()) {
                    onDecodeFinish.handle(i + 1);//对下一首歌预读
                    buffer.putEndInfo(i + 1);
                } else if (CurrentStatus.isClosed()) {
                    buffer.clear();
                    break;
                }
            } else if (CurrentStatus.isClosed())
                buffer.clear();
            else buffer.putEndInfo(-1);
        }
        if (!CurrentStatus.isClosed())
            CurrentStatus.stop();
        paths = null;
        log.trace("当前解码结束");
    }

    public static String getDecoderInfo() {
        try(BytePointer avVersion = av_version_info()) {
            return "FFmpeg Version: \n" + avVersion.getString();
        }
    }
}
