package org.balinhui.fpaplayer.ui.components;

import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Skin;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public final class PSliderToggle extends ToggleButton {
    private final PSliderToggleSkin skin;

    public PSliderToggle() {
        skin = new PSliderToggleSkin(this);
        setSkin(skin);

        setStyle(
                "-fx-background-color: transparent;" +
                "-fx-background-insets: 0;" +
                "-fx-background-radius: 0;" +
                "-fx-padding: 0;" +
                "-fx-border-color: transparent;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;"
        );

        setMinSize(42, 20);
        setMaxSize(42, 20);
    }

    public void disable(boolean flag) {
        setDisable(flag);
        if (flag) {
            skin.getThumb().setOnMouseEntered(null);
            skin.getThumb().setOnMouseEntered(null);
            skin.getContainer().setOnMouseClicked(null);
        } else {
            skin.getThumb().setOnMouseEntered(mouseEvent ->
                    skin.getThumb().setRadius(11));
            skin.getThumb().setOnMouseExited(mouseEvent ->
                    skin.getThumb().setRadius(10));
            skin.getContainer().setOnMouseClicked(e -> setSelected(!isSelected()));
        }
    }

    public void setDarkMode(boolean darkMode) {
        skin.setDarkMode(darkMode);
    }

    private static class PSliderToggleSkin implements Skin<PSliderToggle> {

        private final PSliderToggle control;
        private final StackPane container;
        private final Rectangle track;
        private final Circle thumb;

        // 动画参数
        private static final double TRACK_WIDTH = 42;
        private static final double TRACK_HEIGHT = 20;
        private static final double THUMB_RADIUS = 10;
        private static final double ANIMATION_DURATION = 100; // 毫秒

        private static final Color LIGHT_BLUE = Color.rgb(0, 103, 192);
        private static final Color DARK_BLUE = Color.rgb(76, 194, 255);
        private static final Color DARK_WHITE = Color.rgb(69, 69, 69);

        private boolean darkMode = false;

        public PSliderToggleSkin(PSliderToggle control) {
            this.control = control;

            // 创建轨道
            track = new Rectangle(TRACK_WIDTH, TRACK_HEIGHT);
            track.setArcHeight(TRACK_HEIGHT); // 完全圆角
            track.setArcWidth(TRACK_HEIGHT);
            track.setFill(control.isSelected() ? LIGHT_BLUE : Color.GRAY);
            track.setStroke(Color.LIGHTGRAY);

            // 创建滑块
            thumb = new Circle(THUMB_RADIUS);
            thumb.setFill(Color.WHITE);
            thumb.setStroke(Color.LIGHTGRAY);
            thumb.setStrokeWidth(1);
            thumb.setOnMouseEntered(mouseEvent -> thumb.setRadius(11));
            thumb.setOnMouseExited(mouseEvent -> thumb.setRadius(10));

            // 设置小球的初始位置
            updateThumbPosition(control.isSelected(), false);

            container = new StackPane(track, thumb);
            container.setAlignment(Pos.CENTER_LEFT);

            container.setOnMouseClicked(e -> control.setSelected(!control.isSelected()));

            control.setGraphic(container);

            control.selectedProperty().addListener((obs, oldV, newV) -> {
                updateThumbPosition(newV, true);
                track.setFill(newV ? (darkMode ? DARK_BLUE : LIGHT_BLUE) : Color.GRAY);
            });
        }

        /**
         * 更新小球位置
         * @param selected 是否选中
         * @param animated 是否使用动画
         */
        private void updateThumbPosition(boolean selected, boolean animated) {
            // 计算小球的目标位置
            double targetX = selected ? 21.2 : 0.0;

            if (animated) {
                TranslateTransition transition = new TranslateTransition(
                        Duration.millis(ANIMATION_DURATION), thumb);
                transition.setToX(targetX);
                transition.play();
            } else {
                thumb.setTranslateX(targetX);
            }
        }

        public Circle getThumb() {
            return thumb;
        }

        public StackPane getContainer() {
            return container;
        }

        public void setDarkMode(boolean darkMode) {
            this.darkMode = darkMode;
            if (darkMode) {
                thumb.setFill(DARK_WHITE);
                track.setFill(control.isSelected() ? DARK_BLUE : Color.GRAY);
            } else {
                thumb.setFill(Color.WHITE);
                track.setFill(control.isSelected() ? LIGHT_BLUE : Color.GRAY);
            }
        }

        @Override
        public PSliderToggle getSkinnable() {
            return control;
        }

        @Override
        public StackPane getNode() {
            return container;
        }

        @Override
        public void dispose() {

        }
    }
}
