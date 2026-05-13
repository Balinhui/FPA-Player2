package org.balinhui.fpaplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpaplayer.core.CurrentStatus;
import org.balinhui.fpaplayer.core.Decoder;
import org.balinhui.fpaplayer.core.Event;
import org.balinhui.fpaplayer.core.Player;
import org.balinhui.fpaplayer.info.OutputInfo;
import org.balinhui.fpaplayer.info.SongInfo;
import org.balinhui.fpaplayer.info.SystemInfo;
import org.balinhui.fpaplayer.nativeapis.MessageFlags;
import org.balinhui.fpaplayer.nativeapis.NativeAPI;
import org.balinhui.fpaplayer.ui.UIPlayer;
import org.balinhui.fpaplayer.util.Config;
import org.balinhui.fpaplayer.util.Lyrics;
import org.balinhui.fpaplayer.util.NativeLibraryLoader;
import org.balinhui.fpaplayer.util.ThemeColorExtractor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FPAControl {
    private static final Logger log = LogManager.getLogger(FPAControl.class);
    private static final FPAControl control = new FPAControl();

    private Decoder decoder;
    private Player player;
    private UIPlayer uiPlayer;

    private SongInfo song;


    private FPAControl() {}
    public static FPAControl getControl() {
        return control;
    }

    public void init(Application app) {
        log.trace("初始化开始");
        app.notifyPreloader(new FPAPreloader.Notification("初始化开始...", 0.16));

        app.notifyPreloader(new FPAPreloader.Notification("加载配置", 0.32));
        Config.loadConfig();

        app.notifyPreloader(new FPAPreloader.Notification("加载本地库", 0.48));
        NativeLibraryLoader.load(Resources.LibraryRes.libs);

        app.notifyPreloader(new FPAPreloader.Notification("初始化ffmpeg", 0.64));
        decoder = Decoder.getInstance();

        app.notifyPreloader(new FPAPreloader.Notification("初始化音频设备", 0.8));
        player = Player.getInstance(createOnPlayFinishHandler());

        log.trace("初始化结束");
        app.notifyPreloader(new FPAPreloader.Notification("初始化结束", 1));
    }

    public void onWindowShow() {
        boolean succeed = NativeAPI.getHWND(FPAScreen.OperableControls.mainWindow.getTitle());
        if (!succeed) {
            if (SystemInfo.systemName == SystemInfo.Name.WINDOWS)
                log.error("global库未能正常加载，本机代码无法调用");
            else log.debug("非Windows操作系统，hWnd设置为-1");
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
            paths = NativeAPI.getChoseFiles(Resources.SuffixNameRes.suffix_names);
        } catch (UnsatisfiedLinkError e) {
            NativeAPI.displayMessage(
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
        if (CurrentStatus.stateIs(CurrentStatus.States.PLAYING) || CurrentStatus.stateIs(CurrentStatus.States.PAUSE))
            CurrentStatus.stateTo(CurrentStatus.States.NEXT);
    }

    public void onOpenSettingWindow() {
        //log.debug("Open Setting Button Pressed");
        SettingScreen.show();
    }

    public void onFullScreen() {
        boolean cValue = Config.get("app.fullScreen").value().bValue;
        FPAScreen.setFullScreen(!cValue);
        Config.set("app.fullScreen", !cValue);
    }

    public void onDragOver(DragEvent dragEvent) {
        if (!CurrentStatus.stateIs(CurrentStatus.States.STOP)) {
            dragEvent.consume();
            return;
        }
        Dragboard board = dragEvent.getDragboard();
        if (board.hasFiles()) {
            List<File> files = board.getFiles();
            if (files.size() == 1) { //如果为单个文件
                File cFile = files.getFirst();
                //防止文件夹
                if (!cFile.isDirectory()) {
                    for (String name : Resources.SuffixNameRes.suffix_names) {
                        //如果匹配上音乐文件后缀，就允许
                        if (cFile.getName().endsWith(name)) {
                            dragEvent.acceptTransferModes(TransferMode.MOVE);
                            break;
                        }
                    }
                }
            } else { //如果是多个文件，就直接允许
                dragEvent.acceptTransferModes(TransferMode.MOVE);
            }
        }
        dragEvent.consume();
    }

    public void onDragDropped(DragEvent dragEvent) {
        /*if (!CurrentStatus.stateIs(CurrentStatus.States.STOP)) {
            dragEvent.consume();
            return;
        }*/
        Dragboard board = dragEvent.getDragboard();
        boolean success = false;

        if (board.hasFiles()) {
            List<File> files = board.getFiles();
            if (files.size() == 1) { //如果为单个文件
                String[] path = { files.getFirst().getAbsolutePath() };
                inputPaths(path);
            } else { //多个文件进行分析
                List<File> permitted = new ArrayList<>();//符合条件的文件个数
                for (File file : files) {
                    for (String name : Resources.SuffixNameRes.suffix_names) {
                        if (!file.isDirectory() && file.getName().endsWith(name)) {
                            permitted.add(file);
                            break;
                        }
                    }
                }
                if (permitted.size() != files.size()) {
                    NativeAPI.displayMessage(
                            "请注意",
                            "您所选的文件中有部分可能不是音乐文件，已跳过。",
                            MessageFlags.Buttons.OK | MessageFlags.Icons.WARNING
                    );
                }
                if (!permitted.isEmpty()) {
                    //将List<File> 转化为 绝对路径 String[]
                    String[] filePaths = permitted.stream().map(File::getAbsolutePath).toArray(String[]::new);
                    inputPaths(filePaths);
                }
            }
            success = true;
        }
        dragEvent.setDropCompleted(success);
        dragEvent.consume();
    }

    private void inputPaths(String[] paths) {
        if (paths.length == 1) processOneFile(paths[0]);
        else processFiles(paths);
    }

    private void processOneFile(String path) {
        song = decoder.read(path);
        if (song == null) return;
        OutputInfo output = player.read(song);
        log.trace("OutputInfo: {}", output);
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
                    if (song.cover != null) {
                        Image iCover = new Image(new ByteArrayInputStream(song.cover));
                        updateCoverAndBackground(iCover);
                    }
                    FPAScreen.setPauseButton(true);
                    FPAScreen.showTitle(Lyrics.findTitle(song.metadata));
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
        //更新UI
        FPAScreen.OperableControls.lyricsPane.setLyrics(Lyrics.parse(song.metadata));
        FPAScreen.setDisplayLyrics(true);
        if (song.cover != null) {
            Image iCover = new Image(new ByteArrayInputStream(song.cover));
            updateCoverAndBackground(iCover);
        }
        FPAScreen.setPauseButton(true);
        FPAScreen.showTitle(Lyrics.findTitle(song.metadata));

        uiPlayer = new UIPlayer(song.durationSeconds * 1000,
                FPAScreen.OperableControls.lyricsPane,
                FPAScreen.OperableControls.progressBar
        );

        uiPlayer.play();
        player.start(onPerSongFinish);
    }

    private void updateCoverAndBackground(Image iCover) {
        FPAScreen.OperableControls.cover.setImage(iCover);
        Color progressColor = ThemeColorExtractor.extractDominantColor(iCover);
        FPAScreen.OperableControls.progressBar.setStyle(
                String.format(
                        "progress-color: rgb(%d, %d, %d);",
                        (int) (progressColor.getRed() * 255),
                        (int) (progressColor.getGreen() * 255),
                        (int) (progressColor.getBlue() * 255)
                )
        );
    }

    private Event createOnPlayFinishHandler() {
        return v -> {
            uiPlayer.stop();
            Platform.runLater(() -> {
                FPAScreen.setDisplayLyrics(false);
                FPAScreen.OperableControls.cover.setImage(Resources.ImageRes.cover);
                FPAScreen.OperableControls.progressBar.setStyle("progress-color: white;");
                FPAScreen.OperableControls.progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                FPAScreen.setPauseButton(false);
                FPAScreen.hideTitle();
                FPAScreen.OperableControls.lyricsPane.release();
            });
        };
    }
}
