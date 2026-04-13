package org.balinhui.fpaplayer;

import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.balinhui.fpaplayer.ui.PButton;

public class FPAControl {
    private static final Logger log = LogManager.getLogger(FPAControl.class);
    private static final FPAControl control = new FPAControl();

    private FPAControl() {}
    public static FPAControl getControl() {
        return control;
    }

    public void init() {
        log.trace("初始化开始");
        Config.loadConfig();
        log.trace("初始化结束");
    }

    public void closeWindow(WindowEvent event) {
        if (!FPAScreen.OperableControls.mainWindow.isMaximized() &&
        !FPAScreen.OperableControls.mainWindow.isFullScreen()) {
            Config.set("app.x", FPAScreen.OperableControls.mainWindow.getX());
            Config.set("app.y", FPAScreen.OperableControls.mainWindow.getY());
            Config.set("app.width", FPAScreen.OperableControls.mainWindow.getScene().getWidth());
            Config.set("app.height", FPAScreen.OperableControls.mainWindow.getScene().getHeight());
        }
    }

    public void stop() {
        Config.storeConfig();
    }

    public void onChooseFile() {
        log.debug("Choose File Button Pressed");
    }

    public void onPause(PButton button) {
        log.debug("Pause Button Pressed");
        button.setImages(
                Resources.ImageRes.pause_black,
                Resources.ImageRes.pause_white
        );
        FPAScreen.OperableControls.lyricsPane.enableTranslate(!Config.get("lyric.translate").value().bValue);
    }

    int i = 0;
    public void onNext() {
        log.debug("Next Button Pressed");
        FPAScreen.OperableControls.lyricsPane.scrollToTime(i);
        //System.out.println(i);
        i += 10;
    }

    public void onOpenSettingWindow() {
        log.debug("Open Setting Button Pressed");
        FPAScreen.setDarkMode(!Config.get("app.darkMode").value().bValue);
    }

    public void onFullScreen(PButton button) {
        if (Config.get("app.fullScreen").value().bValue) {
            Config.set("app.fullScreen", false);
            FPAScreen.OperableControls.mainWindow.setFullScreen(false);
            button.setImages(
                    Resources.ImageRes.full_screen_black,
                    Resources.ImageRes.full_screen_white
            );
        } else {
            Config.set("app.fullScreen", true);
            FPAScreen.OperableControls.mainWindow.setFullScreen(true);
            button.setImages(
                    Resources.ImageRes.cancel_full_screen_black,
                    Resources.ImageRes.cancel_full_screen_white
            );
        }
    }
}
