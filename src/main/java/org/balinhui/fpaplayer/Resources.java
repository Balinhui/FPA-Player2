package org.balinhui.fpaplayer;

import javafx.scene.image.Image;
import org.balinhui.fpaplayer.info.SystemInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public class Resources {
    public static class ImageRes {
        public static final Image cover = new Image(Resources.class.getResourceAsStream("/image/cover.png"));
        public static final Image pause_black = new Image(Resources.class.getResourceAsStream("/image/pause_black.png"));
        public static final Image pause_white = new Image(Resources.class.getResourceAsStream("/image/pause_white.png"));
        public static final Image play_black = new Image(Resources.class.getResourceAsStream("/image/play_black.png"));
        public static final Image play_white = new Image(Resources.class.getResourceAsStream("/image/play_white.png"));
        public static final Image next_black = new Image(Resources.class.getResourceAsStream("/image/next_black.png"));
        public static final Image next_white = new Image(Resources.class.getResourceAsStream("/image/next_white.png"));
        public static final Image setting_black = new Image(Resources.class.getResourceAsStream("/image/setting_black.png"));
        public static final Image setting_white = new Image(Resources.class.getResourceAsStream("/image/setting_white.png"));
        public static final Image full_screen_black = new Image(Resources.class.getResourceAsStream("/image/full_screen_black.png"));
        public static final Image full_screen_white = new Image(Resources.class.getResourceAsStream("/image/full_screen_white.png"));
        public static final Image cancel_full_screen_black = new Image(Resources.class.getResourceAsStream("/image/cancel_full_screen_black.png"));
        public static final Image cancel_full_screen_white = new Image(Resources.class.getResourceAsStream("/image/cancel_full_screen_white.png"));
        public static final Image fpa16 = new Image(Resources.class.getResourceAsStream("/image/FPA16.png"));
        public static final Image fpa32 = new Image(Resources.class.getResourceAsStream("/image/FPA32.png"));
        public static final Image fpa64 = new Image(Resources.class.getResourceAsStream("/image/FPA64.png"));
        public static final Image fpa128 = new Image(Resources.class.getResourceAsStream("/image/FPA128.png"));
        public static final Image fpa256 = new Image(Resources.class.getResourceAsStream("/image/FPA256.png"));
    }

    public static class StringRes {
        public static final String title = "FPA Player";
        public static final String choose_file = "选择文件";
        public static final String setting_title = "设置";

        public static class SettingStringRes {
            public static final String app = "基本设置";
            public static final String audio = "音频设置";
            public static final String lyric = "歌词设置";
            public static final String about = "关于";
            public static final String alwaysOnTop = "窗口置顶";
            public static final String darkMode = "暗黑模式";
            public static final String effectType = "背景效果类型";
            public static final String frameNum = "帧大小(0为自动决定)";
            public static final String openWasapi = "使用WAS API";
            public static final String binding = "歌词大小绑定界面";
            public static final String fontSize = "字体大小";
            public static final String position = "歌词位置";
            public static final String translate = "歌词翻译";
        }
    }

    public static class SuffixNameRes {
        public static final List<String> suffix_names = List.of(".mp3", ".flac", ".ogg", ".wav", ".m4a");
    }

    public static class LibraryRes {
        public static final Map<String, List<SystemInfo.Name>> libs = new LinkedHashMap<>(Map.of(
                "global", List.of(SystemInfo.Name.WINDOWS, SystemInfo.Name.MACOS, SystemInfo.Name.LINUX),
                "jportaudio", List.of(SystemInfo.Name.WINDOWS, SystemInfo.Name.MACOS, SystemInfo.Name.LINUX),
                "windows", List.of(SystemInfo.Name.WINDOWS)
        ));
    }
}
