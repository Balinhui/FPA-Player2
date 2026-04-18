module org.balinhui.fpaplayer {
    requires javafx.controls;
    requires org.apache.logging.log4j;
    requires org.bytedeco.javacpp;
    requires org.bytedeco.ffmpeg;

    exports org.balinhui.fpaplayer;
    exports org.balinhui.fpaplayer.ui;
    exports org.balinhui.fpaplayer.util;
    exports org.balinhui.fpaplayer.info;
}