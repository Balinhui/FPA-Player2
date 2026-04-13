package org.balinhui.fpaplayer;

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
import org.balinhui.fpaplayer.ui.LyricsPane;
import org.balinhui.fpaplayer.ui.PButton;

import java.util.List;
import java.util.TreeMap;

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

    private FPAControl control;

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
                        bindCover(OperableControls.cover, leftPane, 0.8);
                    } else if (newValue.doubleValue() >= 500 && !isRightPaneVisible) {
                        isRightPaneVisible = true;
                        showRightPane(leftPane, rightPane, mainPane);
                        bindCover(OperableControls.cover, leftPane, 0.6);
                    }
                }));
        stage.setMinHeight(370);
        stage.setMinWidth(280);
        stage.show();
        OperableControls.mainWindow = stage;
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


        LyricsPane lyricsPane = createLyricsPane(rightPane);
        OperableControls.lyricsPane = lyricsPane;

        rightPane.getChildren().add(lyricsPane);//choose);
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

        bindCover(cover, parent, 0.6);
        return cover;
    }

    private void bindCover(ImageView cover, Pane pane, double scaleFactor) {
        cover.fitWidthProperty().bind(
                Bindings.createDoubleBinding(() -> pane.getWidth() * scaleFactor,
                        pane.widthProperty())
        );
        cover.fitHeightProperty().bind(
                Bindings.createDoubleBinding(() -> pane.getHeight() * scaleFactor,
                        pane.heightProperty())
        );
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
        fullScreen.setOnAction(event -> control.onFullScreen(fullScreen));
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
        TreeMap<Long, List<String>> map = new TreeMap<>();
        map.put(0L, List.of("はぐ (feat. 初音ミク & 可不) - MIMI/初音未来 (初音ミク)/可不 (KAFU)", "QQ音乐享有本翻译作品的著作权"));
        map.put(10L, List.of("ねぇねぇ神様聞いてくれ", "呐 呐 神啊 请听我说"));
        map.put(20L, List.of("心にぽっかり空いちゃった", "我的心突然就空出一个洞来"));
        map.put(30L, List.of("相当辛いな今日だって", "今天也过得颇为艰辛"));
        map.put(40L, List.of("泣かないように目を瞑る", "只为不让泪水落下而闭上眼睛"));
        map.put(50L, List.of("なんなんなんにもできないし", "一切都让我感到无能为力"));
        map.put(60L, List.of("どうやったって笑えないし", "不论如何都笑不出来"));
        map.put(70L, List.of("責任転嫁は自己嫌悪", "推诿责任只会让我厌恶自己"));
        map.put(80L, List.of("嗚呼 独りで夢の中", "啊啊 独自一人徜徉于梦境"));
        map.put(90L, List.of("パっていつかパって", "某天 你在突然间"));
        map.put(100L, List.of("君が呼び止める", "便喊住了我"));
        map.put(110L, List.of("ただ夜の奥鼓動の音", "只在深夜感受这份悸动"));
        map.put(120L, List.of("寂しさ2人で分け合った", "让两人一起分担寂寞"));
        map.put(130L, List.of("そしたらそしたら大丈夫", "然后告诉彼此已经没关系了"));
        map.put(140L, List.of("って優しく明日を笑えるの？", "这样就能温柔地笑对明天吗？"));
        lyricsPane.setLyrics(map);


        return lyricsPane;
    }

    private void initSlideAnimations(Pane leftPane, Pane rightPane, Pane mainPane) {
        rightPaneSlideOut = new TranslateTransition(Duration.millis(120), rightPane);
        rightPaneSlideOut.setOnFinished(event -> {
            leftPane.prefWidthProperty().unbind();
            leftPane.prefWidthProperty().bind(mainPane.widthProperty());
            rightPane.prefWidthProperty().unbind();
            rightPane.setVisible(false);
            rightPane.setManaged(false);
        });

        rightPaneSlideIn = new TranslateTransition(Duration.millis(120), rightPane);
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

    public static void setDarkMode(boolean darkMode) {
        if (Config.get("app.darkMode").value().bValue == darkMode) return;
        Config.get("app.darkMode").value().bValue = darkMode;
        choose.setDarkMode(darkMode);
        pause.setDarkMode(darkMode);
        next.setDarkMode(darkMode);
        openSetting.setDarkMode(darkMode);
        fullScreen.setDarkMode(darkMode);
        OperableControls.lyricsPane.setDarkMode(darkMode);
        //TODO 背景深色
    }
}
