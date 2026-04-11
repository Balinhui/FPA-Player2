package org.balinhui.fpaplayer;

import javafx.scene.image.Image;

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
    }

    public static class StringRes {
        public static final String title = "FPA Player";
        public static final String choose_file = "选择文件";
    }
}
