package org.balinhui.fpaplayer;

import javafx.application.Preloader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class FPAPreloader extends Preloader {
    private Stage preloaderStage;
    @Override
    public void start(Stage primaryStage) throws Exception {
        preloaderStage = primaryStage;

        VBox root = new VBox();
        root.setStyle("-fx-background-color:black");
        root.setAlignment(Pos.CENTER);

        Scene scene = new Scene(root, 600, 400);

        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("FPA Player Preloader");
        primaryStage.show();
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
            preloaderStage.hide();
        }
    }
}
