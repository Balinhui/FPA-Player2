package org.balinhui.fpaplayer;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.balinhui.fpaplayer.info.SystemInfo;
import org.balinhui.fpaplayer.ui.LyricsPane;
import org.balinhui.fpaplayer.ui.PButton;
import org.balinhui.fpaplayer.util.Config;

public class FPAScreen extends Application {

    public static class OperableControls {
        public static Stage mainWindow;
        public static VBox root;
        public static ImageView cover;
        public static ProgressBar progressBar;
        public static LyricsPane lyricsPane;
    }

    private static PButton choose;
    private static PButton pause;
    private static PButton next;
    private static PButton openSetting;
    private static PButton fullScreen;

    private TranslateTransition rightPaneSlideOut;
    private TranslateTransition rightPaneSlideIn;
    private boolean isRightPaneVisible = true;

    private Timeline coverTimeLine;

    private FPAControl control;

    private static final double ANIMATION_TIME = 120;//ms

    @Override
    public void init() throws Exception {
        control = FPAControl.getControl();
        control.init();
    }

    @Override
    public void start(Stage stage) throws Exception {
        VBox root = new VBox();
        OperableControls.root = root;

        HBox mainPane = new HBox();
        VBox.setVgrow(mainPane, Priority.ALWAYS);

        VBox leftPane = createLeftPane(mainPane);
        VBox rightPane = createRightPane(mainPane);
        initSlideAnimations(leftPane, rightPane, mainPane);

        mainPane.getChildren().addAll(leftPane, rightPane);

        VBox bar = new VBox();
        bar.setPrefHeight(60);

        ProgressBar progressBar = new ProgressBar();
        OperableControls.progressBar = progressBar;
        progressBar.setPrefHeight(9);
        progressBar.prefWidthProperty().bind(bar.widthProperty());

        BorderPane bottom = createBottom();

        bar.getChildren().addAll(progressBar, bottom);

        root.getChildren().addAll(mainPane, bar);

        double width = Config.get("app.width").value().dValue;
        double height = Config.get("app.height").value().dValue;
        Scene scene = new Scene(root, width == -1 ? 600 : width, height == -1 ? 400 : height);
        if (SystemInfo.systemName == SystemInfo.Name.WINDOWS)
            stage.initStyle(StageStyle.UNIFIED);
        stage.setTitle(Resources.StringRes.title);
        stage.setScene(scene);
        double x = Config.get("app.x").value().dValue;
        double y = Config.get("app.y").value().dValue;
        if (x != -1) stage.setX(x);
        if (y != -1) stage.setY(y);
        stage.setFullScreen(Config.get("app.fullScreen").value().bValue);
        stage.setFullScreenExitHint("");
        stage.widthProperty().addListener((
                (observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() < 500 && isRightPaneVisible) {
                        isRightPaneVisible = false;
                        hideRightPane(rightPane);
                        bindCover(OperableControls.cover, leftPane, 0.8, false);
                    } else if (newValue.doubleValue() >= 500 && !isRightPaneVisible) {
                        isRightPaneVisible = true;
                        bindCover(OperableControls.cover, leftPane, 0.6, false);
                        showRightPane(leftPane, rightPane, mainPane);
                    }
                }));
        stage.setMinHeight(370);
        stage.setMinWidth(280);
        stage.show();
        OperableControls.mainWindow = stage;
        control.onWindowShow();
        stage.setOnCloseRequest(control::closeWindow);
        //TODO 设置背景
    }

    @Override
    public void stop() throws Exception {
        control.stop();
    }

    private VBox createLeftPane(Pane parent) {
        VBox leftPane = new VBox();
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        leftPane.prefWidthProperty().bind(parent.widthProperty().divide(2));
        //leftPane.setStyle("-fx-background-color:blue");
        leftPane.setAlignment(Pos.CENTER);

        ImageView cover = createImageView(leftPane);
        OperableControls.cover = cover;
        leftPane.getChildren().addAll(cover);
        return leftPane;
    }

    private VBox createRightPane(Pane parent) {
        VBox rightPane = new VBox();
        rightPane.setPadding(new Insets(10, 10, 10, 10));
        HBox.setHgrow(rightPane, Priority.ALWAYS);
        rightPane.prefWidthProperty().bind(parent.widthProperty().divide(2));
        //rightPane.setStyle("-fx-background-color:red");
        rightPane.setAlignment(Pos.CENTER);

        choose = new PButton(Resources.StringRes.choose_file);
        choose.setDarkMode(Config.get("app.darkMode").value().bValue);
        choose.setFont(new Font(15));
        choose.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
            double calculatedWidth = rightPane.getWidth() * 0.4;
            double minWidth = 130;
            double maxWidth = 200;
            return Math.clamp(calculatedWidth, minWidth, maxWidth);
        }, rightPane.widthProperty()));
        choose.setPrefHeight(45);
        choose.setOnAction(event -> control.onChooseFile());

        OperableControls.lyricsPane = createLyricsPane(rightPane);

        rightPane.getChildren().add(choose);
        return rightPane;
    }

    private ImageView createImageView(Pane parent) {
        ImageView cover = new ImageView(Resources.ImageRes.cover);
        cover.setPreserveRatio(true);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(cover.fitWidthProperty());
        clip.heightProperty().bind(cover.fitHeightProperty());
        clip.setArcWidth(10);
        clip.setArcHeight(10);

        cover.setClip(clip);

        bindCover(cover, parent, 0.6, true);
        return cover;
    }

    private void bindCover(ImageView cover, Pane pane, double scaleFactor, boolean isInit) {
        if (isInit) {
            cover.fitWidthProperty().bind(
                    Bindings.createDoubleBinding(() -> pane.getWidth() * scaleFactor,
                            pane.widthProperty())
            );

            cover.fitHeightProperty().bind(
                    Bindings.createDoubleBinding(() -> pane.getHeight() * scaleFactor,
                            pane.heightProperty())
            );
        } else {
            if (coverTimeLine != null) {
                coverTimeLine.stop();
                coverTimeLine = null;
            }

            cover.fitWidthProperty().unbind();
            cover.fitHeightProperty().unbind();

            coverTimeLine = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(cover.fitWidthProperty(), cover.getFitWidth()),
                            new KeyValue(cover.fitHeightProperty(), cover.getFitHeight())
                    ),
                    new KeyFrame(Duration.millis(ANIMATION_TIME),
                            new KeyValue(cover.fitWidthProperty(), pane.getWidth() * scaleFactor),
                            new KeyValue(cover.fitHeightProperty(), pane.getHeight() * scaleFactor)
                    )
            );

            coverTimeLine.setOnFinished(event -> {
                cover.fitWidthProperty().bind(
                        Bindings.createDoubleBinding(() -> pane.getWidth() * scaleFactor,
                                pane.widthProperty())
                );

                cover.fitHeightProperty().bind(
                        Bindings.createDoubleBinding(() -> pane.getHeight() * scaleFactor,
                                pane.heightProperty())
                );
            });

            coverTimeLine.play();
        }
    }

    private HBox createLeftButtonBar(Pane parent) {
        HBox buttonBar = new HBox(10);
        //buttonBar.setStyle("-fx-background-color:purple");
        buttonBar.prefHeightProperty().bind(parent.heightProperty());
        buttonBar.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> {
                    double width = parent.getWidth() * 0.2;
                    double minWidth = 130;
                    double maxWidth = parent.getWidth() / 2 < minWidth ? minWidth + 1 : parent.getWidth() / 2;
                    return Math.clamp(width, minWidth, maxWidth);
                },
                parent.widthProperty()
        ));
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10, 10, 10, 10));

        pause = new PButton(Resources.ImageRes.play_black, Resources.ImageRes.play_white);
        pause.setDarkMode(Config.get("app.darkMode").value().bValue);
        pause.setIconHeight(20);
        pause.setIconWidth(20);
        pause.setOnAction(event -> control.onPause(pause));
        createBindingForBottomButtons(pause, buttonBar);

        next = new PButton(Resources.ImageRes.next_black, Resources.ImageRes.next_white);
        next.setDarkMode(Config.get("app.darkMode").value().bValue);
        next.setIconWidth(20);
        next.setIconHeight(20);
        next.setOnAction(event -> control.onNext());
        createBindingForBottomButtons(next, buttonBar);
        buttonBar.getChildren().addAll(pause, next);
        return buttonBar;
    }

    private HBox createRightButtonBar(Pane parent) {
        HBox buttonBar = new HBox(10);
        //buttonBar.setStyle("-fx-background-color:gray");
        buttonBar.prefHeightProperty().bind(parent.heightProperty());
        buttonBar.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> {
                    double width = parent.getWidth() * 0.2;
                    double minWidth = 130;
                    double maxWidth = parent.getWidth() * 0.5 < minWidth ? minWidth + 1 : parent.getWidth() * 0.5;
                    return Math.clamp(width, minWidth, maxWidth);
                },
                parent.widthProperty()
        ));
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10, 10, 10, 10));

        openSetting = new PButton(Resources.ImageRes.setting_black, Resources.ImageRes.setting_white);
        openSetting.setDarkMode(Config.get("app.darkMode").value().bValue);
        openSetting.setIconWidth(20);
        openSetting.setIconHeight(20);
        openSetting.setOnAction(event -> control.onOpenSettingWindow());
        createBindingForBottomButtons(openSetting, buttonBar);

        if (Config.get("app.fullScreen").value().bValue) {
            fullScreen =
                    new PButton(Resources.ImageRes.cancel_full_screen_black, Resources.ImageRes.cancel_full_screen_white);
        } else {
            fullScreen =
                    new PButton(Resources.ImageRes.full_screen_black, Resources.ImageRes.full_screen_white);
        }
        fullScreen.setDarkMode(Config.get("app.darkMode").value().bValue);
        fullScreen.setIconWidth(20);
        fullScreen.setIconHeight(20);
        fullScreen.setOnAction(event -> control.onFullScreen());
        createBindingForBottomButtons(fullScreen, buttonBar);
        buttonBar.getChildren().addAll(openSetting, fullScreen);
        return buttonBar;
    }

    private void createBindingForBottomButtons(PButton button, Pane parent) {
        button.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> {
                    double width = parent.getWidth() * 0.3;
                    double minWidth = 100;
                    return Math.max(minWidth, width);
                },
                parent.widthProperty()
        ));
    }

    private BorderPane createBottom() {
        BorderPane bottom = new BorderPane();
        //bottom.setStyle("-fx-background-color:yellow");
        VBox.setVgrow(bottom, Priority.ALWAYS);

        HBox leftButtonBar = createLeftButtonBar(bottom);

        HBox rightButtonBar = createRightButtonBar(bottom);
        bottom.setLeft(leftButtonBar);
        bottom.setRight(rightButtonBar);
        return bottom;
    }


    private LyricsPane createLyricsPane(Pane parent) {
        LyricsPane lyricsPane = new LyricsPane(new VBox(15));
        lyricsPane.setStyle("-fx-background-color:transparent");
        lyricsPane.prefWidthProperty().bind(parent.widthProperty());
        lyricsPane.prefHeightProperty().bind(Bindings.createDoubleBinding(
                () -> parent.getHeight() * 0.6,
                parent.heightProperty()
        ));

        return lyricsPane;
    }

    private void initSlideAnimations(Pane leftPane, Pane rightPane, Pane mainPane) {
        rightPaneSlideOut = new TranslateTransition(Duration.millis(ANIMATION_TIME), rightPane);
        rightPaneSlideOut.setOnFinished(event -> {
            leftPane.prefWidthProperty().unbind();
            leftPane.prefWidthProperty().bind(mainPane.widthProperty());
            rightPane.prefWidthProperty().unbind();
            rightPane.setVisible(false);
            rightPane.setManaged(false);
        });

        rightPaneSlideIn = new TranslateTransition(Duration.millis(ANIMATION_TIME), rightPane);
    }

    private void hideRightPane(Pane pane) {
        rightPaneSlideOut.setFromX(0);
        rightPaneSlideOut.setToX(pane.getWidth());
        rightPaneSlideOut.play();
    }

    private void showRightPane(Pane leftPane, Pane rightPane, Pane mainPane) {
        leftPane.prefWidthProperty().unbind();
        leftPane.prefWidthProperty().bind(mainPane.widthProperty().divide(2));
        rightPane.prefWidthProperty().bind(mainPane.widthProperty().divide(2));
        rightPane.setVisible(true);
        rightPane.setManaged(true);
        rightPaneSlideIn.setFromX(rightPane.getWidth());
        rightPaneSlideIn.setToX(0);
        rightPaneSlideIn.play();
    }

    /**
     * 设置界面暗黑模式，不会将值储存
     */
    public static void setDarkMode(boolean darkMode) {
        if (Config.get("app.darkMode").value().bValue == darkMode) return;
        choose.setDarkMode(darkMode);
        pause.setDarkMode(darkMode);
        next.setDarkMode(darkMode);
        openSetting.setDarkMode(darkMode);
        fullScreen.setDarkMode(darkMode);
        OperableControls.lyricsPane.setDarkMode(darkMode);
        //TODO 背景深色
    }

    /**
     * 设置界面全屏，不会将值储存
     */
    public static void setFullScreen(boolean fullScreenValue) {
        if (Config.get("app.fullScreen").value().bValue == fullScreenValue) return;
        FPAScreen.OperableControls.mainWindow.setFullScreen(fullScreenValue);
        if (fullScreenValue) {
            fullScreen.setImages(
                    Resources.ImageRes.cancel_full_screen_black,
                    Resources.ImageRes.cancel_full_screen_white
            );
        } else {
            fullScreen.setImages(
                    Resources.ImageRes.full_screen_black,
                    Resources.ImageRes.full_screen_white
            );
        }
    }
}
