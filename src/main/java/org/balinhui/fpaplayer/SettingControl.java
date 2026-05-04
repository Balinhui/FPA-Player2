package org.balinhui.fpaplayer;

import org.balinhui.fpaplayer.nativeapis.MessageFlags;
import org.balinhui.fpaplayer.nativeapis.NativeAPI;
import org.balinhui.fpaplayer.nativeapis.Win32;
import org.balinhui.fpaplayer.util.Config;

public class SettingControl {

    private static final SettingControl control = new SettingControl();
    private SettingControl() {}

    public static SettingControl getControl() {
        return control;
    }

    public void onBooleanConfigChange(String name, boolean newValue) {
        Config.set(name, newValue);
        if (name.contains("darkMode")) {
            SettingScreen.setDarkMode(newValue);
            FPAScreen.setDarkMode(newValue);
        } else if (name.contains("openWasapi")) {
            NativeAPI.displayMessage(
                    "提示",
                    "下次播放时生效",
                    MessageFlags.Buttons.OK | MessageFlags.Icons.INFORMATION
            );
        } else if (name.contains("binding")) {
            FPAScreen.OperableControls.lyricsPane.bindLyrics(newValue);
        } else if (name.contains("translate")) {
            FPAScreen.OperableControls.lyricsPane.enableTranslate(newValue);
        } else if (name.contains("alwaysOnTop")) {
            FPAScreen.OperableControls.mainWindow.setAlwaysOnTop(newValue);
            SettingScreen.setAlwaysOnTop(newValue);
        }
    }

    public void onDoubleConfigChange(String name, double newValue) {
        Config.set(name, newValue);
        if (name.contains("fontSize")) {
            FPAScreen.OperableControls.lyricsPane.setLyricsSize(newValue);
        }
    }

    public void onStringConfigChange(String name, String newValue) {
        Config.set(name, newValue);
        if (name.contains("effectType") && Config.get("app.supportMica").value().bValue) {
            Win32.Effects effect = Win32.Effects.MICA;
            if (newValue.equals("trans")) effect = Win32.Effects.TRANS;
            else if (newValue.equals("tabbed")) effect = Win32.Effects.TABBED;
            NativeAPI.applyWindowsEffect(effect);
        } else if (name.contains("position")) {
            FPAScreen.OperableControls.lyricsPane.changePosition(newValue);
        }
    }
}
