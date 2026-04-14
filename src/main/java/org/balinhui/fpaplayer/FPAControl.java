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
        log.trace("停止开始");
        Config.storeConfig();
        log.trace("停止结束");
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
    }

    public void onNext() {
        log.debug("Next Button Pressed");
    }

    public void onOpenSettingWindow() {
        log.debug("Open Setting Button Pressed");
    }

    public void onFullScreen() {
        boolean cValue = Config.get("app.fullScreen").value().bValue;
        FPAScreen.setFullScreen(!cValue);
        Config.set("app.fullScreen", !cValue);
    }
}
