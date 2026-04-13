package org.balinhui.fpaplayer.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.balinhui.fpaplayer.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LyricsPane extends ScrollPane {
    private final VBox lyricsContainer;
    private final List<LyricLine> realLyrics = new ArrayList<>();
    private Timeline scrollTimeLine;
    private int currentLine = -1;
    private long lastCall = 0;

    public LyricsPane(VBox box) {
        super(box);
        this.lyricsContainer = box;
        box.prefWidthProperty().bind(prefWidthProperty());
        layoutBoundsProperty().addListener((obs, old, bounds) -> {
            if (getViewportBounds() != null) {
                lookupAll(".viewport").forEach(node -> {
                    node.setStyle("-fx-background-color: transparent;");
                    if (node instanceof Region) {
                        ((Region) node).setBackground(Background.EMPTY);
                    }
                });
            }
        });
        setBorder(Border.EMPTY);
        setVbarPolicy(ScrollBarPolicy.NEVER);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setOnMouseEntered(event -> setVbarPolicy(ScrollBarPolicy.AS_NEEDED));
        setOnMouseExited(event -> setVbarPolicy(ScrollBarPolicy.NEVER));
        prefHeightProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (currentLine != -1) scrollToLine(currentLine, true, 100);
                }
        );
        prefWidthProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (currentLine != -1) scrollToLine(currentLine, true, 100);
                }
        );
    }

    public void setLyrics(TreeMap<Long, List<String>> lyrics) {
        this.lyricsContainer.getChildren().clear();
        addPlaceholderComponents();
        for (Map.Entry<Long, List<String>> entry : lyrics.entrySet()) {
            LyricLine lyricLine = new LyricLine(entry.getKey(), entry.getValue().toArray(value -> new String[0]));
            this.realLyrics.add(lyricLine);
            this.lyricsContainer.getChildren().add(lyricLine);
        }
        addPlaceholderComponents();
        realLyrics.getFirst().setHighlight(true);
    }

    private void addPlaceholderComponents() {
        for (int i = 0; i < 2; i++) {
            this.lyricsContainer.getChildren().add(new LyricLine(-1L, "", ""));
        }
    }

    public void setDarkMode(boolean darkMode) {
        realLyrics.forEach(lyricLine -> lyricLine.setDarkMode(darkMode));
    }

    /**
     * 改变歌词位置，此会将值储存起来
     * @param position 包含 {@code center, left, right}
     */
    public void changePosition(String position) {
        if (!position.matches("center") && !position.matches("left") && !position.matches("right"))
            throw new IllegalArgumentException("position的值未知");
        realLyrics.forEach(lyricLine -> lyricLine.changePosition(position));
        Config.set("lyric.position", position);
    }

    private void scrollToLine(int lineIndex, boolean isChangePosition, double ms) {
        if (isChangePosition && System.currentTimeMillis() - lastCall < 500)
            return;
        lastCall = System.currentTimeMillis();
        if (lineIndex >= realLyrics.size()) return;
        if (scrollTimeLine != null) {
            scrollTimeLine.stop();
            scrollTimeLine = null;
        }

        Node targetNode = lyricsContainer.getChildren().get(lineIndex + 2);

        double nodeTop = targetNode.getBoundsInParent().getMinY();
        double nodeHeight = targetNode.getBoundsInParent().getHeight();
        double containerHeight = lyricsContainer.getHeight();
        double viewportHeight = getViewportBounds().getHeight();

        double targetScrollTop = nodeTop - (viewportHeight - nodeHeight) / 2;

        double maxScroll = containerHeight - viewportHeight;
        targetScrollTop = Math.clamp(targetScrollTop, 0, maxScroll);

        double targetVValue = targetScrollTop / maxScroll;

        if (maxScroll <= 0) return;

        scrollTimeLine = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(vvalueProperty(), getVvalue())),
                new KeyFrame(Duration.millis(ms),
                        new KeyValue(vvalueProperty(), targetVValue))
        );

        scrollTimeLine.play();
        scrollTimeLine.setOnFinished(event -> {
            realLyrics.forEach(lyricLine -> lyricLine.setHighlight(false));
            realLyrics.get(lineIndex).setHighlight(true);
        });
    }

    public void scrollToTime(long time) {
        int left = 0;
        int right = realLyrics.size() - 1;
        int targetIndex = -1;

        while (left <= right) {
            int mid = (left + right) / 2;
            long midTime = realLyrics.get(mid).getTime();
            if (midTime <= time) {
                targetIndex = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        if (targetIndex == -1) {
            targetIndex = 0;
        }

        if (currentLine == targetIndex) return;
        currentLine = targetIndex;
        scrollToLine(targetIndex, false, 200);
    }

    /**
     * 启用，关闭歌词，此会将值储存起来
     */
    public void enableTranslate(boolean enable) {
        realLyrics.forEach(lyricLine -> lyricLine.setDisplayTranslate(enable));
        scrollToLine(currentLine, true, 100);
        Config.set("lyric.translate", enable);
    }
}
