package org.balinhui.fpaplayer.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import org.balinhui.fpaplayer.Config;

import java.util.ArrayList;
import java.util.List;

public class LyricLine extends VBox {
    private final long time;
    private final List<Label> labels = new ArrayList<>();
    private boolean highlight = false;
    private final GaussianBlur blur = new GaussianBlur();

    private static final Paint GRAY_WHITE = Color.rgb(190, 190, 190);
    private static final Paint GRAY_DARK = Color.rgb(65, 65, 65);
    private final double BLUE_RADIUS = 3.0;
    private final double RATE = 0.06;
    /**
     * 创建一行歌词
     * @param time 歌词的时间戳，毫秒值
     * @param lyric 默认主要显示第一个歌词，其他为翻译，最多允许三行，多余将忽略
     */
    public LyricLine(long time, String... lyric) {
        //setStyle("-fx-background-color:gray");
        String position = Config.get("lyric.position").value().sValue;
        if (position.equals("left")) setAlignment(Pos.CENTER_LEFT);
        else if (position.equals("right")) setAlignment(Pos.CENTER_RIGHT);
        else setAlignment(Pos.CENTER);
        setPadding(new Insets(0, 36, 0, 0));
        blur.setRadius(BLUE_RADIUS);
        setEffect(blur);

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
            //l.setStyle("-fx-background-color:white");
            if (position.equals("left")) l.setTextAlignment(TextAlignment.LEFT);
            else if (position.equals("right")) l.setTextAlignment(TextAlignment.RIGHT);
            else l.setTextAlignment(TextAlignment.CENTER);

            if (Config.get("app.darkMode").value().bValue) l.setTextFill(GRAY_WHITE);
            else l.setTextFill(GRAY_DARK);

            if (i == 0) l.fontProperty().bind(Bindings.createObjectBinding(() -> {
                double max = 40;
                return new Font(Math.min(max, getWidth() * RATE));
            }, widthProperty()));
            else l.fontProperty().bind(Bindings.createObjectBinding(() -> {
                double max = 30;
                return new Font(Math.min(max, getWidth() * RATE - 5));
            }, widthProperty()));
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
                if (Config.get("app.darkMode").value().bValue)
                    label.setTextFill(Color.WHITE);
                else label.setTextFill(Color.BLACK);
            });
        } else {
            this.blur.setRadius(BLUE_RADIUS);
            this.labels.forEach(label -> {
                if (Config.get("app.darkMode").value().bValue)
                    label.setTextFill(GRAY_WHITE);
                else label.setTextFill(GRAY_DARK);
            });
        }
    }

    public void setDisplayTranslate(boolean translate) {
        if (translate) {
            if (labels.size() == 3)
                getChildren().addAll(labels.get(1), labels.get(2));
            else
                getChildren().addAll(labels.get(1));
        } else
            getChildren().remove(1, getChildren().size());
    }

    public long getTime() {
        return time;
    }
}
