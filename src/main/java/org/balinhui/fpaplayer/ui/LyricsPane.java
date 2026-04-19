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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LyricsPane extends ScrollPane {
    private final VBox lyricsContainer;
    private final List<LyricLine> realLyrics = new ArrayList<>();
    private Timeline scrollTimeLine;
    private int currentLine = -1;
    private long latestCall = 0;

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
        addPlaceholderComponents();
        for (Map.Entry<Long, List<String>> entry : lyrics.entrySet()) {
            LyricLine lyricLine = new LyricLine(entry.getKey(), entry.getValue().toArray(value -> new String[0]));
            this.realLyrics.add(lyricLine);
            this.lyricsContainer.getChildren().add(lyricLine);
        }
        addPlaceholderComponents();
    }

    private void addPlaceholderComponents() {
        for (int i = 0; i < 2; i++) {
            this.lyricsContainer.getChildren().add(new LyricLine(-1L, "", ""));
        }
    }

    public void setDarkMode(boolean darkMode) {
        if (realLyrics.isEmpty()) return;
        realLyrics.forEach(lyricLine -> lyricLine.setDarkMode(darkMode));
    }

    /**
     * 改变歌词位置，不会将值储存起来
     * @param position 包含 {@code center, left, right}
     */
    public void changePosition(String position) {
        if (realLyrics.isEmpty()) return;
        if (!position.matches("center") && !position.matches("left") && !position.matches("right"))
            throw new IllegalArgumentException("position的值未知");
        realLyrics.forEach(lyricLine -> lyricLine.changePosition(position));
    }

    private void scrollToLine(int lineIndex, boolean isChangePosition, double ms) {
        if (isChangePosition && System.nanoTime() - latestCall < 500000000)
            return;
        latestCall = System.nanoTime();
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
        if (maxScroll < 0) maxScroll = 0;
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
        /*scrollTimeLine.setOnFinished(event -> {

        });*/
        if (!realLyrics.isEmpty()) {
            realLyrics.forEach(lyricLine -> lyricLine.setHighlight(false));
            realLyrics.get(lineIndex).setHighlight(true);
        }
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
        scrollToLine(targetIndex, false, 150);
    }

    /**
     * 启用，关闭歌词，不会将值储存起来
     */
    public void enableTranslate(boolean enable) {
        if (realLyrics.isEmpty()) return;
        realLyrics.forEach(lyricLine -> lyricLine.setDisplayTranslate(enable));
        if (currentLine > -1)
            scrollToLine(currentLine, true, 100);
    }

    public void bindLyrics(boolean binding) {
        if (realLyrics.isEmpty()) return;
        realLyrics.forEach(lyricLine -> lyricLine.setBinding(binding));
        if (currentLine > -1)
            scrollToLine(currentLine, true, 100);
    }

    public void setLyricsSize(double size) {
        if (realLyrics.isEmpty()) return;
        realLyrics.forEach(lyricLine -> lyricLine.setLyricFontSize(size));
        if (currentLine > -1)
            scrollToLine(currentLine, true, 100);
    }

    public void release() {
        this.realLyrics.forEach(LyricLine::release);
        this.lyricsContainer.getChildren().clear();
        this.realLyrics.clear();
        this.scrollTimeLine = null;
        this.currentLine = -1;
        this.latestCall = 0;
    }
}
