package org.balinhui.fpaplayer;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpaplayer.core.CurrentStatus;
import org.balinhui.fpaplayer.core.Decoder;
import org.balinhui.fpaplayer.core.Event;
import org.balinhui.fpaplayer.core.Player;
import org.balinhui.fpaplayer.info.OutputInfo;
import org.balinhui.fpaplayer.info.SongInfo;
import org.balinhui.fpaplayer.info.SystemInfo;
import org.balinhui.fpaplayer.nativeapis.Global;
import org.balinhui.fpaplayer.nativeapis.MessageFlags;
import org.balinhui.fpaplayer.ui.UIPlayer;
import org.balinhui.fpaplayer.util.Config;
import org.balinhui.fpaplayer.util.Lyrics;
import org.balinhui.fpaplayer.util.NativeLibraryLoader;

import java.io.ByteArrayInputStream;

public class FPAControl {
    private static final Logger log = LogManager.getLogger(FPAControl.class);
    public static long hWnd;
    private static final FPAControl control = new FPAControl();

    private Decoder decoder;
    private Player player;
    private UIPlayer uiPlayer;

    private SongInfo song;


    private FPAControl() {}
    public static FPAControl getControl() {
        return control;
    }

    public void init() {
        log.trace("初始化开始");
        Config.loadConfig();
        NativeLibraryLoader.load(Resources.LibraryRes.libs);

        decoder = Decoder.getInstance();
        player = Player.getInstance(createOnPlayFinishHandler());
        log.trace("初始化结束");
    }

    public void onWindowShow() {
        try {
            hWnd = Global.getHwnd(FPAScreen.OperableControls.mainWindow.getTitle());
        } catch (UnsatisfiedLinkError e) {
            if (SystemInfo.systemName == SystemInfo.Name.WINDOWS)
                log.error("global库未能正常加载，本机代码无法调用");
            else log.info("非Windows操作系统，hWnd设置为-1");
            hWnd = -1;
        }
    }

    public void closeWindow() {
        if (!FPAScreen.OperableControls.mainWindow.isMaximized() &&
        !FPAScreen.OperableControls.mainWindow.isFullScreen()) {
            Config.set("app.x", FPAScreen.OperableControls.mainWindow.getX());
            Config.set("app.y", FPAScreen.OperableControls.mainWindow.getY());
            Config.set("app.width", FPAScreen.OperableControls.mainWindow.getScene().getWidth());
            Config.set("app.height", FPAScreen.OperableControls.mainWindow.getScene().getHeight());
        }
    }

    public void stop() {
        log.trace("停止开始");
        if (CurrentStatus.stateIs(CurrentStatus.States.PLAYING) || CurrentStatus.stateIs(CurrentStatus.States.PAUSE)) {
            CurrentStatus.stateTo(CurrentStatus.States.CLOSE);
            if (uiPlayer != null)
                uiPlayer.stop();
        }
        Config.storeConfig();
        player.terminate();
        Runtime.getRuntime().addShutdownHook(new Thread(NativeLibraryLoader::cleanup));
        log.trace("停止结束");
    }

    public void onChooseFile() {
        if (!CurrentStatus.stateIs(CurrentStatus.States.STOP))
            return;
        String[] paths;
        try {
            paths = Global.chooseFiles(hWnd, Resources.SuffixNameRes.suffix_names);
        } catch (UnsatisfiedLinkError e) {
            Global.messageOf(
                    hWnd,
                    "未链接库",
                    "没有链接到file_chooser",
                    MessageFlags.Buttons.OK | MessageFlags.Icons.ERROR
            );
            return;
        }
        if (paths == null) return;
        inputPaths(paths);
    }

    public void onPause() {
        if (CurrentStatus.stateIs(CurrentStatus.States.PLAYING)) {
            CurrentStatus.stateTo(CurrentStatus.States.PAUSE);
            uiPlayer.pause();
            FPAScreen.setPauseButton(false);
        } else if (CurrentStatus.stateIs(CurrentStatus.States.PAUSE)) {
            CurrentStatus.stateTo(CurrentStatus.States.PLAYING);
            uiPlayer.syncTime( (long) CurrentStatus.getCurrentTimeMillis());
            uiPlayer.resume();
            FPAScreen.setPauseButton(true);
        }
    }

    public void onNext() {
        CurrentStatus.stateTo(CurrentStatus.States.NEXT);
    }

    public void onOpenSettingWindow() {
        log.debug("Open Setting Button Pressed");
    }

    public void onFullScreen() {
        boolean cValue = Config.get("app.fullScreen").value().bValue;
        FPAScreen.setFullScreen(!cValue);
        Config.set("app.fullScreen", !cValue);
    }

    private void inputPaths(String[] paths) {
        if (paths.length == 1) processOneFile(paths[0]);
        else processFiles(paths);
    }

    private void processOneFile(String path) {
        song = decoder.read(path);
        if (song == null) return;
        OutputInfo output = player.read(song);
        playSong(song, output, null, null);
    }

    private void processFiles(String[] paths) {
        song = decoder.read(paths);
        if (song == null) return;
        OutputInfo output = player.getTheSameOutput();
        playSong(song, output, index -> {
            if (index < paths.length)
                song = decoder.onlyRead(paths[index]);
        }, index -> {
            if (index < paths.length) {
                uiPlayer.stop();
                CurrentStatus.resetTime(song.durationSeconds, song.totalSamples);
                Platform.runLater(() -> {
                    FPAScreen.OperableControls.lyricsPane.release();
                    FPAScreen.OperableControls.lyricsPane.setLyrics(Lyrics.parse(song.metadata));
                    FPAScreen.OperableControls.cover.setImage(new Image(new ByteArrayInputStream(song.cover)));

                });
                uiPlayer = new UIPlayer(
                        song.durationSeconds * 1000,
                        FPAScreen.OperableControls.lyricsPane,
                        FPAScreen.OperableControls.progressBar
                );
                uiPlayer.play();
            }
        });
    }

    private void playSong(SongInfo song, OutputInfo output, Event onDecodeFinish,
                          Event onPerSongFinish) {
        CurrentStatus.resetTime(song.durationSeconds, song.totalSamples);
        decoder.start(output, onDecodeFinish);
        FPAScreen.OperableControls.lyricsPane.setLyrics(Lyrics.parse(song.metadata));
        FPAScreen.setDisplayLyrics(true);
        FPAScreen.OperableControls.cover.setImage(new Image(new ByteArrayInputStream(song.cover)));
        FPAScreen.setPauseButton(true);
        uiPlayer = new UIPlayer(song.durationSeconds * 1000,
                FPAScreen.OperableControls.lyricsPane,
                FPAScreen.OperableControls.progressBar);


        uiPlayer.play();
        player.start(onPerSongFinish);
    }

    private Event createOnPlayFinishHandler() {
        return v -> {
            if (uiPlayer != null)
                uiPlayer.stop();
            Platform.runLater(() -> {
                FPAScreen.setDisplayLyrics(false);
                FPAScreen.OperableControls.cover.setImage(Resources.ImageRes.cover);
                FPAScreen.OperableControls.progressBar.setProgress(-1);
                FPAScreen.setPauseButton(false);
                FPAScreen.OperableControls.lyricsPane.release();
            });
        };
    }
}
