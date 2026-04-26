package org.balinhui.fpaplayer;

import javafx.application.Preloader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class FPAPreloader extends Preloader {
    private Stage preloaderStage;
    private Label text;
    private ProgressBar progressBar;


    @Override
    public void start(Stage primaryStage) throws Exception {
        preloaderStage = primaryStage;

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color:gray");
        root.setAlignment(Pos.CENTER);

        ImageView view = new ImageView(Resources.ImageRes.fpa128);

        text = new Label("加载中...");
        text.setFont(new Font(15));
        text.setTextFill(Color.WHITE);

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(400);
        progressBar.setPrefHeight(10);
        root.getChildren().addAll(view, text, progressBar);

        Scene scene = new Scene(root, 600, 400);

        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setOpacity(0.9);
        primaryStage.setTitle("FPA Player Preloader");
        primaryStage.setAlwaysOnTop(true);
        primaryStage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
            preloaderStage.hide();
        }
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof Notification(String message, double progress)) {
            text.setText(message);
            progressBar.setProgress(progress);
        }
    }

    public record Notification(String message, double progress) implements PreloaderNotification {
    }
}
