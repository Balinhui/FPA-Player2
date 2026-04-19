package org.balinhui.fpaplayer.core;

import com.portaudio.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpaplayer.FPAControl;
import org.balinhui.fpaplayer.info.OutputInfo;
import org.balinhui.fpaplayer.info.SongInfo;
import org.balinhui.fpaplayer.nativeapis.Global;
import org.balinhui.fpaplayer.nativeapis.MessageFlags;
import org.balinhui.fpaplayer.util.AudioUtil;
import org.balinhui.fpaplayer.util.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.portaudio.PortAudio.*;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLT;

public class Player implements Runnable {
    private static final Logger log = LogManager.getLogger(Player.class);
    private static Player instance;

    private final Buffer buffer;
    private int deviceId;
    private int maxOutputChannels;
    private int maxOutputSampleRate;
    private BlockingStream stream;
    private Event onPerSongFinish;
    private final Event onPlayFinish;
    private final ExecutorService singleThread;//player的唯一线程，一切与portaudio有关的操作都将在这里进行

    public static boolean isWasapiSupported;

    private Player(Event onPlayFinish) {
        buffer = Buffer.getInstance();
        this.onPlayFinish = onPlayFinish;
        //初始化Play的线程
        ThreadFactory factory = r -> new Thread(r, "Play Thread");
        singleThread = Executors.newSingleThreadExecutor(factory);

        singleThread.submit(() -> {
            initialize();

            isWasapiSupported = hostApiTypeIdToHostApiIndex(HOST_API_TYPE_WASAPI) > 0;
            log.info("对 wasapi 的支持情况: {}", isWasapiSupported ? "支持" : "不支持");
            deviceId = getDefaultOutputDevice();
            if (isWasapiSupported && Config.get("audio.openWasapi").value().bValue)
                deviceId = getHostApiInfo(hostApiTypeIdToHostApiIndex(HOST_API_TYPE_WASAPI)).defaultOutputDevice;
            DeviceInfo deviceInfo = getDeviceInfo(deviceId);
            log.info("默认输出设备为: {}", deviceInfo.name);
            getDeviceChannelsAndSampleRateInfo(deviceId, deviceInfo);
        });
    }

    public static Player getInstance(Event onPlayFinish) {
        if (instance == null) instance = new Player(onPlayFinish);
        return instance;
    }

    private void getDeviceChannelsAndSampleRateInfo(int id, DeviceInfo deviceInfo) {
        this.maxOutputChannels = deviceInfo.maxOutputChannels;
        StreamParameters test = new StreamParameters();
        test.device = id;
        test.channelCount = this.maxOutputChannels;
        test.suggestedLatency = deviceInfo.defaultLowOutputLatency;
        int[] sampleRatesToTest = {384000, 192000, 96000, 88200, 48000, 44100, 8000};
        int maxOutputSampleRate = sampleRatesToTest[sampleRatesToTest.length - 1];
        for (int v : sampleRatesToTest) {
            if (isFormatSupported(null, test, v) == 0) {
                maxOutputSampleRate = v;
                break;
            }
        }
        this.maxOutputSampleRate = maxOutputSampleRate;
    }

    private void refreshDevice() {
        int id = getDefaultOutputDevice();
        if (isWasapiSupported && Config.get("audio.openWasapi").value().bValue)
            id = getHostApiInfo(hostApiTypeIdToHostApiIndex(HOST_API_TYPE_WASAPI)).defaultOutputDevice;
        if (id != deviceId) {
            log.info("检测到输出设备更改，由id: {} -> id: {}", deviceId, id);
            this.deviceId = id;
            DeviceInfo deviceInfo = getDeviceInfo(id);
            log.trace("取得当前输出设备: {}", deviceInfo.name);

            getDeviceChannelsAndSampleRateInfo(id, deviceInfo);
        }
    }

    public OutputInfo read(SongInfo songInfo) {
        refreshDevice();

        int channels = songInfo.channels, sampleRate = songInfo.sampleRate, sampleFormat = songInfo.sampleFormat;
        boolean resample = false;
        if (channels > maxOutputChannels || sampleRate > maxOutputSampleRate) {
            channels = Math.min(channels, maxOutputChannels);
            sampleRate = Math.min(sampleRate, maxOutputSampleRate);
            resample = true;
        }

        if (songInfo.isPlanar) {
            sampleFormat = AudioUtil.getSampleFormatNoPlanar(sampleFormat);
            log.info("格式 {} 为非平面格式，转化为平面格式",
                    AudioUtil.getSampleFormatName(sampleFormat));
            resample = true;
        }

        if (!AudioUtil.isSupport(sampleFormat)) {
            log.info("格式 {} 不支持，转化为 {}",
                    AudioUtil.getSampleFormatName(sampleFormat),
                    AudioUtil.getSampleFormatName(AV_SAMPLE_FMT_FLT)
            );
            sampleFormat = AV_SAMPLE_FMT_FLT;
            resample = true;
        }

        if (isWasapiSupported && Config.get("audio.openWasapi").value().bValue) {
            int defaultSampleRate = (int) getDeviceInfo(deviceId).defaultSampleRate;
            if (sampleRate != defaultSampleRate) {
                log.info("使用WASAPI，歌曲采样率必须为 {} HZ", defaultSampleRate);
                sampleRate = defaultSampleRate;
                resample = true;
            }
        }

        openStream(channels, sampleRate, AudioUtil.getPortAudioSampleFormat(sampleFormat));
        return new OutputInfo(resample, channels, sampleRate, sampleFormat);
    }

    public OutputInfo getTheSameOutput() {
        refreshDevice();

        int channels = 2;
        int sampleRate = 48000;
        if (isWasapiSupported && Config.get("audio.openWasapi").value().bValue) {
            int onlySampleRate = (int) getDeviceInfo(deviceId).defaultSampleRate;
            log.info("使用WASAPI，所有歌曲的采样率统一为: {}", onlySampleRate);
            sampleRate = onlySampleRate;
        }
        boolean resample = true;

        openStream(channels, sampleRate, FORMAT_FLOAT_32);
        return new OutputInfo(resample, channels, sampleRate, AV_SAMPLE_FMT_FLT);
    }

    private void openStream(int channels, int sampleRate, int sampleFormat) {
        DeviceInfo deviceInfo = getDeviceInfo(deviceId);

        int frames = (int) Config.get("audio.frameNum").value().dValue;
        log.debug("缓冲区大小设置: {}", frames);

        StreamParameters parameters = new StreamParameters();
        parameters.device = deviceId;
        parameters.channelCount = channels;
        parameters.sampleFormat = sampleFormat;
        if (frames >= 1024) {
            parameters.suggestedLatency = deviceInfo.defaultHighOutputLatency;
            log.trace("缓冲区大小超过1024，使用高延时输出");
        } else {
            parameters.suggestedLatency = deviceInfo.defaultLowOutputLatency;
            log.trace("缓冲区大小小于1024，使用低延时输出");
        }

        stream = PortAudio.openStream(
                null,
                parameters,
                sampleRate,
                frames,
                0
        );
        StreamInfo info = stream.getInfo();
        log.info("打开流。OutputLatency: {}, SampleRate: {}",
                info.outputLatency, info.sampleRate);
    }

    public void start(Event onPerSongFinish) {
        this.onPerSongFinish = onPerSongFinish;
        singleThread.submit(this);
    }

    @Override
    public void run() {
        if (stream == null) return;
        if (!stream.isActive())
            stream.start();

        int ret;
        if ((ret = stream.getWriteAvailable()) < 0) {
            log.fatal("当前流无法写入: {}", ret);
            ret = Global.messageOf(
                    FPAControl.hWnd,
                    "出错",
                    "歌曲的流无法写入设备: " + ret + "，可能是设备问题或请重试",
                    MessageFlags.Buttons.RETRY_CANCEL | MessageFlags.Icons.WARNING
            );
            if (ret == MessageFlags.ReturnValue.RETRY) run();
            CurrentStatus.stateTo(CurrentStatus.States.STOP);
            buffer.clear();
        }
        while (!buffer.isEmpty() ||
                (!CurrentStatus.stateIs(CurrentStatus.States.STOP) && !CurrentStatus.stateIs(CurrentStatus.States.CLOSE))) {
            boolean paused;//用于判断是否暂停了的标识，如果是从暂停中启动，则为true。防止提前退出
            try {
                paused = CurrentStatus.waitUntilNotPaused();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (paused) {
                if (CurrentStatus.stateIs(CurrentStatus.States.CLOSE)) {
                    buffer.clear();
                    break;
                }
            }

            Buffer.Data data = buffer.takeData();
            if (data.end()) {
                //解码器解码完成
                if (onPerSongFinish != null)
                    onPerSongFinish.handle(data.pos());
            } else {
                CurrentStatus.updateTime(data.oldSamplesNumber());
                boolean result = false;
                switch (data.currentDataType()) {
                    case SHORT -> result = stream.write(data.getShortArray(), data.samplesNumber());
                    case FLOAT -> result = stream.write(data.getFloatArray(), data.samplesNumber());
                }
                if (result) {
                    log.fatal("Write stream failed: output underflow");
                }
            }
        }
        stop();
        onPlayFinish.handle(0);
    }

    public void stop() {
        //先将流中数据完后暂停，然后停止
        stream.stop();

        stream.close();
    }

    public void terminate() {
        singleThread.submit(() -> {
            if (!stream.isStopped()) stop();

            PortAudio.terminate();
        });

        singleThread.shutdown();
        try {
            if (!singleThread.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                singleThread.shutdownNow();
            }
        } catch (InterruptedException e) {
            singleThread.shutdownNow();
        }
    }

}
