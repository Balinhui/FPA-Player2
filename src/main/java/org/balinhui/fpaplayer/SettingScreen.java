package org.balinhui.fpaplayer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.balinhui.fpaplayer.core.Decoder;
import org.balinhui.fpaplayer.core.Player;
import org.balinhui.fpaplayer.info.SystemInfo;
import org.balinhui.fpaplayer.nativeapis.NativeAPI;
import org.balinhui.fpaplayer.nativeapis.Win32;
import org.balinhui.fpaplayer.ui.components.PButton;
import org.balinhui.fpaplayer.ui.components.PMenuBar;
import org.balinhui.fpaplayer.ui.components.PSliderToggle;
import org.balinhui.fpaplayer.util.Config;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class SettingScreen {
    private static SettingScreen screen;
    private final Stage stage;
    private final BorderPane root;
    private final PMenuBar menuBar;

    private final Map<String, Node> scenes = new LinkedHashMap<>();
    private final List<Label> texts = new ArrayList<>();
    private final List<PSliderToggle> toggles = new ArrayList<>();
    private final List<ChoiceBox<?>> choiceBoxes = new ArrayList<>();
    private final SettingControl control;

    private final Class<Resources.StringRes.SettingStringRes> settingStringResClass;
    private long tmpHwnd;

    private SettingScreen() {
        control = SettingControl.getControl();
        stage = new Stage();

        settingStringResClass = Resources.StringRes.SettingStringRes.class;

        root = new BorderPane();

        LinkedHashMap<String, List<String>> config = readConfig();

        config.forEach((key, value) -> scenes.put(key, createPane(key, value)));
        scenes.put("about", createAboutPane());

        menuBar = createMenuBar(root);
        menuBar.setDarkMode(Config.get("app.darkMode").value().bValue);
        ((PButton) menuBar.getChildren().getFirst()).setColor(PButton.ButtonColor.BLUE);

        root.setTop(menuBar);
        root.setCenter(scenes.get("app"));


        Scene scene = new Scene(root, 350, 250);
        URL url = getClass().getResource("/fpa-style.css");
        if (url != null)
            scene.getStylesheets().add(url.toExternalForm());

        if (SystemInfo.systemName == SystemInfo.Name.WINDOWS && Config.get("app.supportMica").value().bValue) {
            stage.initStyle(StageStyle.UNIFIED);
            scene.setFill(Color.TRANSPARENT);
            root.setBackground(Background.fill(Color.TRANSPARENT));
        }
        stage.setTitle(Resources.StringRes.setting_title);
        stage.getIcons().addAll(
                Resources.ImageRes.fpa16,
                Resources.ImageRes.fpa32,
                Resources.ImageRes.fpa64,
                Resources.ImageRes.fpa128,
                Resources.ImageRes.fpa256
        );
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(FPAScreen.OperableControls.mainWindow);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setAlwaysOnTop(Config.get("app.alwaysOnTop").value().bValue);
        stage.setOnShown(event -> Platform.runLater(() -> {
            tmpHwnd = NativeAPI.getHWNDForOthers(stage.getTitle());
            if (tmpHwnd > 0 && Config.get("app.supportMica").value().bValue) {
                NativeAPI.applyWindowsEffectForOthers(tmpHwnd, Win32.Effects.TRANS);
            }

            if (!NativeAPI.setDarkModeForOthers(tmpHwnd, Config.get("app.darkMode").value().bValue)) {
                if (Config.get("app.darkMode").value().bValue)
                    root.setBackground(Background.fill(Color.BLACK));
                else root.setBackground(Background.fill(Color.WHITE));
            }
        }));
    }

    private PMenuBar createMenuBar(BorderPane root) {
        PMenuBar menuBar = new PMenuBar();
        for (String s : scenes.keySet()) {
            String tmp;
            try {
                tmp = (String) settingStringResClass.getDeclaredField(s).get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                tmp = s;
            }
            String name = tmp;
            menuBar.addItem(name, actionEvent -> {
                root.setCenter(scenes.get(s));
                for (PButton item : menuBar.getItems()) {
                    item.setColor(PButton.ButtonColor.DEFAULT);
                }
                menuBar.getItemButton(name).setColor(PButton.ButtonColor.BLUE);
            });
        }
        return menuBar;
    }

    public static void show() {
        if (screen == null) {
            screen = new SettingScreen();
        }
        screen.stage.showAndWait();
    }

    private LinkedHashMap<String, List<String>> readConfig() {
        Set<String> configsSet = Config.getConfigsSet();
        LinkedHashMap<String, List<String>> configsList = configsSet.stream()
                .sorted()
                .collect(Collectors.groupingBy(
                        s -> s.split("\\.")[0],
                        LinkedHashMap::new,
                        Collectors.mapping(
                                s -> s.substring(s.indexOf('.') + 1),
                                Collectors.toList()
                        )
                ));
        List<String> app = configsList.get("app");
        app.removeAll(List.of("fullScreen", "height", "supportMica", "width", "tempLib", "x", "y"));
        return configsList;
    }

    private VBox createPane(String paneName, List<String> items) {
        VBox root = new VBox(5);
        root.setPadding(new Insets(10));
        for (String item : items) {
            String configName = paneName + "." + item;
            HBox region = new HBox();
            //region.setStyle("-fx-background-color:orange");

            String localName;
            try {
                localName = (String) settingStringResClass.getDeclaredField(item).get(null);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                localName = item;
            }
            Label text = new Label(localName);
            texts.add(text);
            text.setFont(Font.font(15));
            if (Config.get("app.darkMode").value().bValue)
                text.setTextFill(Color.WHITE);
            else
                text.setTextFill(Color.BLACK);

            Region space = new Region();
            HBox.setHgrow(space, Priority.ALWAYS);

            Config.ConfigPreference preference = Config.get(configName);
            switch (preference.type()) {
                case BOOL -> {
                    PSliderToggle toggle = new PSliderToggle();
                    toggles.add(toggle);
                    toggle.setSelected(preference.value().bValue);
                    toggle.setDarkMode(Config.get("app.darkMode").value().bValue);
                    toggle.selectedProperty().addListener(
                            (obs, oldV, newV) -> {
                                control.onBooleanConfigChange(configName, newV);
                    });
                    if (item.equals("openWasapi") && SystemInfo.systemName != SystemInfo.Name.WINDOWS) {
                        toggle.disable(true);
                        text.setTextFill(Color.GRAY);
                    }
                    region.getChildren().addAll(text, space, toggle);
                }
                case STR -> {
                    ChoiceBox<String> choiceBox = new ChoiceBox<>();
                    choiceBoxes.add(choiceBox);
                    if (Config.get("app.darkMode").value().bValue) {
                        choiceBox.setStyle(
                                "choice-box-background-color: #343536;" +
                                        "choice-box-border-color: #454545;" +
                                        "choice-box-text-fill: white;" +
                                        "choice-box-focused-color: #3b3b3c;" +
                                        "choice-box-icon-color: #4cc2ff;"
                        );
                    } else {
                        choiceBox.setStyle(
                                "choice-box-background-color: white;" +
                                        "choice-box-border-color: #dddddd;" +
                                        "choice-box-text-fill: black;" +
                                        "choice-box-focused-color: #f2f1ef;" +
                                        "choice-box-icon-color: #0067c0;"
                        );
                    }
                    if (item.equals("effectType")) {
                        if (!Config.get("app.supportMica").value().bValue) {
                            choiceBox.getItems().add("none");
                            choiceBox.setDisable(true);
                        } else {
                            choiceBox.getItems().addAll("mica", "trans", "tabbed");
                        }
                    } else if (item.equals("position"))
                        choiceBox.getItems().addAll("left", "center", "right");
                    choiceBox.setValue(preference.value().sValue);
                    choiceBox.getSelectionModel().selectedItemProperty().addListener(
                            (obs, oldV, newV) -> {
                                control.onStringConfigChange(configName, newV);
                            }
                    );
                    region.getChildren().addAll(text, space, choiceBox);
                }
                case DOUBLE -> {
                    ChoiceBox<Integer> choiceBox = new ChoiceBox<>();
                    choiceBoxes.add(choiceBox);
                    if (Config.get("app.darkMode").value().bValue) {
                        choiceBox.setStyle(
                                "choice-box-background-color: #343536;" +
                                        "choice-box-border-color: #454545;" +
                                        "choice-box-text-fill: white;" +
                                        "choice-box-focused-color: #3b3b3c;" +
                                        "choice-box-icon-color: #4cc2ff;"
                        );
                    } else {
                        choiceBox.setStyle(
                                "choice-box-background-color: white;" +
                                        "choice-box-border-color: #dddddd;" +
                                        "choice-box-text-fill: black;" +
                                        "choice-box-focused-color: #f2f1ef;" +
                                        "choice-box-icon-color: #0067c0;"
                        );
                    }
                    if (item.equals("frameNum"))
                        choiceBox.getItems().addAll(0, 128, 256, 512, 1024, 2048, 4096);
                    else if (item.equals("fontSize"))
                        choiceBox.getItems().addAll(20, 25, 30, 35, 40);
                    choiceBox.setValue((int) preference.value().dValue);
                    choiceBox.getSelectionModel().selectedItemProperty().addListener(
                            (obs, oldV, newV) -> {
                                control.onDoubleConfigChange(configName, newV);
                            }
                    );
                    region.getChildren().addAll(text, space, choiceBox);
                }
            }
            root.getChildren().add(region);
        }
        return root;
    }

    private HBox createAboutPane() {
        HBox root = new HBox(10);

        ImageView ico = new ImageView(Resources.ImageRes.fpa64);

        VBox text = new VBox(5);

        Label name = new Label("FPA Player2");
        texts.add(name);
        name.setFont(Font.font(18));
        Label ver = new Label("v1.0-SNAPSHOT");
        texts.add(ver);
        ver.setFont(Font.font(12));
        Label auther = new Label("Ba Linhui");
        texts.add(auther);
        auther.setFont(Font.font(14));

        text.getChildren().addAll(name, ver, auther);

        VBox info = new VBox(5);

        Label ffmpegInfo = new Label(Decoder.getDecoderInfo());
        texts.add(ffmpegInfo);
        Label portaudioInfo = new Label(Player.getPlayerInfo());
        texts.add(portaudioInfo);

        info.getChildren().addAll(ffmpegInfo, portaudioInfo);

        if (Config.get("app.darkMode").value().bValue) {
            name.setTextFill(Color.WHITE);
            ver.setTextFill(Color.WHITE);
            auther.setTextFill(Color.WHITE);
            ffmpegInfo.setTextFill(Color.WHITE);
            portaudioInfo.setTextFill(Color.WHITE);
        } else {
            name.setTextFill(Color.BLACK);
            ver.setTextFill(Color.BLACK);
            auther.setTextFill(Color.BLACK);
            ffmpegInfo.setTextFill(Color.BLACK);
            portaudioInfo.setTextFill(Color.BLACK);
        }

        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(ico, text, info);
        return root;
    }

    public static void setDarkMode(boolean darkMode) {
        boolean hr = NativeAPI.setDarkModeForOthers(screen.tmpHwnd, darkMode);
        screen.menuBar.setDarkMode(darkMode);
        screen.toggles.forEach(pSliderToggle -> pSliderToggle.setDarkMode(darkMode));
        if (darkMode) {
            if (!hr) screen.root.setBackground(Background.fill(Color.BLACK));
            screen.texts.forEach(text -> text.setTextFill(Color.WHITE));
            screen.choiceBoxes.forEach(choiceBox -> choiceBox.setStyle(
                    "choice-box-background-color: #343536;" +
                            "choice-box-border-color: #454545;" +
                            "choice-box-text-fill: white;" +
                            "choice-box-focused-color: #3b3b3c;" +
                            "choice-box-icon-color: #4cc2ff;"
            ));
        } else {
            if (!hr) screen.root.setBackground(Background.fill(Color.WHITE));
            screen.texts.forEach(text -> text.setTextFill(Color.BLACK));
            screen.choiceBoxes.forEach(choiceBox -> choiceBox.setStyle(
                    "choice-box-background-color: white;" +
                            "choice-box-border-color: #dddddd;" +
                            "choice-box-text-fill: black;" +
                            "choice-box-focused-color: #f2f1ef;" +
                            "choice-box-icon-color: #0067c0;"
            ));
        }
    }

    public static void setAlwaysOnTop(boolean alwaysOnTop) {
        screen.stage.setAlwaysOnTop(alwaysOnTop);
    }
}
