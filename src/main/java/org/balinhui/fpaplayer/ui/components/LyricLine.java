package org.balinhui.fpaplayer.ui.components;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import org.balinhui.fpaplayer.util.Config;

import java.util.ArrayList;
import java.util.List;

public final class LyricLine extends VBox {
    private final long time;
    private final List<Label> labels = new ArrayList<>();
    private boolean highlight = false;
    private boolean darkMode;
    private final GaussianBlur blur = new GaussianBlur();

    private static final Paint GRAY_WHITE = Color.rgb(190, 190, 190);
    private static final Paint GRAY_DARK = Color.rgb(65, 65, 65);
    private static final double BLUR_RADIUS = 3.0;
    private static final double RATE = 0.06;
    private static final double MAX_FONT_SIZE = 40;
    private static final double MIN_FONT_SIZE = 20;

    /**
     * 创建一行歌词
     * @param time 歌词的时间戳，毫秒值
     * @param lyric 默认主要显示第一个歌词，其他为翻译，最多允许三行，多余将忽略
     */
    public LyricLine(boolean isFake, long time, String... lyric) {
        String position = Config.get("lyric.position").value().sValue;
        if (position.equals("left")) setAlignment(Pos.CENTER_LEFT);
        else if (position.equals("right")) setAlignment(Pos.CENTER_RIGHT);
        else setAlignment(Pos.CENTER);
        setPadding(new Insets(0, 36, 0, 0));
        blur.setRadius(BLUR_RADIUS);
        setEffect(blur);
        darkMode = Config.get("app.darkMode").value().bValue;
        if (!isFake) {
            setOnMouseEntered(mouseEvent -> {
                if (darkMode)
                    setBackground(Background.fill(Color.rgb(55, 55, 55, 0.2)));
                else setBackground(Background.fill(Color.rgb(155, 155, 155, 0.2)));
            });
            setOnMouseExited(mouseEvent -> setBackground(Background.fill(Color.TRANSPARENT)));
        }

        this.time = time;
        String[] addLyric;
        if (lyric.length <= 3) {
            addLyric = lyric;
        } else {
            addLyric = new String[3];
            System.arraycopy(lyric, 0, addLyric, 0, 3);
        }
        for (int i = 0; i < addLyric.length; i++) {
            Label l = new Label(addLyric[i]);
            l.setWrapText(true);

            if (position.equals("left")) l.setTextAlignment(TextAlignment.LEFT);
            else if (position.equals("right")) l.setTextAlignment(TextAlignment.RIGHT);
            else l.setTextAlignment(TextAlignment.CENTER);

            if (darkMode) l.setTextFill(GRAY_WHITE);
            else l.setTextFill(GRAY_DARK);

            //通过配置文件设置是否绑定布局容器
            if (Config.get("lyric.binding").value().bValue) {
                if (i == 0) l.fontProperty().bind(Bindings.createObjectBinding(() ->
                        Font.font(null, FontWeight.MEDIUM, Math.min(MAX_FONT_SIZE, getWidth() * RATE)),
                        widthProperty()));
                else l.fontProperty().bind(Bindings.createObjectBinding(() ->
                        Font.font(null, FontWeight.LIGHT, Math.min(MAX_FONT_SIZE - 5, getWidth() * RATE - 5)),
                        widthProperty()));
            } else {
                double size = Config.get("lyric.fontSize").value().dValue;
                size = Math.clamp(size, MIN_FONT_SIZE, MAX_FONT_SIZE);

                if (i == 0) l.setFont(Font.font(null, FontWeight.MEDIUM, size));
                else l.setFont(Font.font(null, FontWeight.LIGHT, size - 5));
            }
            getChildren().add(l);
            labels.add(l);
        }
        //如果配置中不显示翻译，则只保留第一行
        if (!Config.get("lyric.translate").value().bValue)
            getChildren().remove(1, getChildren().size());
    }

    public void changePosition(String position) {
        if (position.equals("left")) {
            setAlignment(Pos.CENTER_LEFT);
            labels.forEach(label -> label.setTextAlignment(TextAlignment.LEFT));
        } else if (position.equals("right")) {
            setAlignment(Pos.CENTER_RIGHT);
            labels.forEach(label -> label.setTextAlignment(TextAlignment.RIGHT));
        } else {
            setAlignment(Pos.CENTER);
            labels.forEach(label -> label.setTextAlignment(TextAlignment.CENTER));
        }
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        if (darkMode) {
            labels.forEach(label -> {
                if (highlight) label.setTextFill(Color.WHITE);
                else label.setTextFill(GRAY_WHITE);
            });
        } else {
            labels.forEach(label -> {
                if (highlight) label.setTextFill(Color.BLACK);
                else label.setTextFill(GRAY_DARK);
            });
        }
    }

    public void setHighlight(boolean highlight) {
        if (this.highlight == highlight) return;
        this.highlight = highlight;
        if (highlight) {
            this.blur.setRadius(0.0);
            this.labels.forEach(label -> {
                if (darkMode)
                    label.setTextFill(Color.WHITE);
                else label.setTextFill(Color.BLACK);
            });
        } else {
            this.blur.setRadius(BLUR_RADIUS);
            this.labels.forEach(label -> {
                if (darkMode)
                    label.setTextFill(GRAY_WHITE);
                else label.setTextFill(GRAY_DARK);
            });
        }
    }

    public void setDisplayTranslate(boolean translate) {
        if (translate) {
            if (labels.size() == 3)
                getChildren().addAll(labels.get(1), labels.get(2));
            else if (labels.size() == 2)
                getChildren().addAll(labels.get(1));
        } else
            getChildren().remove(1, getChildren().size());
    }

    public void setBinding(boolean binding) {
        if (labels.getFirst().fontProperty().isBound() == binding) return;
        if (binding) {
            for (int i = 0; i < labels.size(); i++) {
                labels.get(i).setFont(null);

                if (i == 0) labels.get(i).fontProperty().bind(Bindings.createObjectBinding(() ->
                                Font.font(null, FontWeight.MEDIUM, Math.min(MAX_FONT_SIZE, getWidth() * RATE)),
                        widthProperty()));
                else labels.get(i).fontProperty().bind(Bindings.createObjectBinding(() ->
                                Font.font(null, FontWeight.LIGHT, Math.min(MAX_FONT_SIZE - 5, getWidth() * RATE - 5)),
                        widthProperty()));
            }
        } else {
            double size = Config.get("lyric.fontSize").value().dValue;
            size = Math.clamp(size, MIN_FONT_SIZE, MAX_FONT_SIZE);
            for (int i = 0; i < labels.size(); i++) {
                labels.get(i).fontProperty().unbind();
                if (i == 0) labels.get(i).setFont(Font.font(null, FontWeight.MEDIUM, size));
                else labels.get(i).setFont(Font.font(null, FontWeight.LIGHT, size - 5));
            }
        }
    }

    public void setLyricFontSize(double size) {
        if (labels.getFirst().fontProperty().isBound()) return;
        size = Math.clamp(size, MIN_FONT_SIZE, MAX_FONT_SIZE);
        for (int i = 0; i < labels.size(); i++) {
            if (i == 0) labels.get(i).setFont(Font.font(null, FontWeight.MEDIUM, size));
            else labels.get(i).setFont(Font.font(null, FontWeight.LIGHT, size - 5));
        }
    }

    public long getTime() {
        return time;
    }

    public void release() {
        getChildren().clear();
        labels.clear();
    }
}
