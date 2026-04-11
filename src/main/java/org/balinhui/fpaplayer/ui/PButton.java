package org.balinhui.fpaplayer.ui;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;

public class PButton extends Button {
    private List<String> lightColors;
    private List<String> darkColors;
    private Image lightImage;
    private Image darkImage;
    private ImageView icon;
    private final boolean image;
    private boolean darkMode = false;

    public PButton(String context) {
        this(context, ButtonColor.DEFAULT);
    }

    public PButton(String context, ButtonColor color) {
        super(context);
        image = false;
        if (color == ButtonColor.DEFAULT) {
            createColorList(
                    "#ffffff",
                    "#000000",
                    "#cccccc",
                    "#f6f6f6",
                    "#f6f6f6",
                    "#595b5d",
                    "#343536",
                    "#ffffff",
                    "#454545",
                    "#3c3c3c",
                    "#2f3030",
                    "#b4b5b5"
            );
        } else if (color == ButtonColor.BLUE) {
            createColorList(
                    "#0067c0",
                    "#ffffff",
                    "#003e73",
                    "#1975c5",
                    "#3183ca",
                    "#c2daef",
                    "#4cc2ff",
                    "#000000",
                    "#42a7dc",
                    "#99ebff",
                    "#0091f8",
                    "#00487c"
            );
        }
        setAppearance();
    }

    public PButton(Image lightImage, Image darkImage) {
        super();
        image = true;
        this.lightImage = lightImage;
        this.darkImage = darkImage;
        icon = new ImageView();
        createColorList(
                "#ffffff",
                "#000000",
                "#cccccc",
                "#f6f6f6",
                "#f6f6f6",
                "#595b5d",
                "#343536",
                "#ffffff",
                "#454545",
                "#3c3c3c",
                "#2f3030",
                "#b4b5b5"
        );
        setAppearance();
    }

    public void setDarkMode(boolean darkMode) {
        if (darkMode == this.darkMode) return;
        this.darkMode = darkMode;
        setAppearance();
    }

    public void setImages(Image lightImage, Image darkImage) {
        if (lightImage == this.lightImage && darkImage == this.darkImage) return;
        this.lightImage = lightImage;
        this.darkImage = darkImage;
        if (darkMode) icon.setImage(darkImage);
        else icon.setImage(lightImage);
    }

    public void setIconWidth(double width) {
        if (icon != null) icon.setFitWidth(width);
    }

    public void setIconHeight(double height) {
        if (icon != null) icon.setFitHeight(height);
    }

    private void createColorList(
            String lightBackground,
            String lightText,
            String lightBorder,
            String lightEnteredBackground,
            String lightPressedBackground,
            String lightPressedText,
            String darkBackground,
            String darkText,
            String darkBorder,
            String darkEnteredBackground,
            String darkPressedBackground,
            String darkPressedText
    ) {
        lightColors = List.of(
                lightBackground, lightText, lightBorder, lightEnteredBackground, lightPressedBackground, lightPressedText
        );
        darkColors = List.of(
                darkBackground, darkText, darkBorder, darkEnteredBackground, darkPressedBackground, darkPressedText
        );
    }

    private void setAppearance() {
        List<String> colors = darkMode ? darkColors : lightColors;
        setStyle("-fx-background-color: " + colors.getFirst() + ";" +
                "-fx-text-base-color: " + colors.get(1) + ";" +
                "-fx-background-radius: 5.0;" +
                "-fx-border-color: " + colors.get(2) + ";" +
                "-fx-border-radius: 5.0;" +
                "-fx-border-width: 1 1 2 1;" +
                "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        setOnMouseEntered(mouseEvent -> setStyle(
                "-fx-background-color: " + colors.get(3) + ";" +
                        "-fx-text-base-color: " + colors.get(1) + ";" +
                        "-fx-background-radius: 5.0;" +
                        "-fx-border-color: " + colors.get(2) + ";" +
                        "-fx-border-radius: 5.0;" +
                        "-fx-border-width: 1 1 2 1;" +
                        "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;"));
        setOnMouseExited(mouseEvent -> setStyle(
                "-fx-background-color: " + colors.getFirst() + ";" +
                        "-fx-text-base-color: " + colors.get(1) + ";" +
                        "-fx-background-radius: 5.0;" +
                        "-fx-border-color: " + colors.get(2) + ";" +
                        "-fx-border-radius: 5.0; " +
                        "-fx-border-width: 1 1 2 1;" +
                        "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;"));
        setOnMousePressed(mouseEvent -> setStyle(
                "-fx-background-color: " + colors.get(4) + ";" +
                        "-fx-text-base-color: " + colors.get(5) + ";" +
                        "-fx-background-radius: 5.0; " +
                        "-fx-border-color: " + colors.get(2) + ";" +
                        "-fx-border-radius: 5.0;" +
                        "-fx-border-width: 1;" +
                        "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;"));
        setOnMouseReleased(mouseEvent -> setStyle(
                "-fx-background-color: " + colors.getFirst() + ";" +
                        "-fx-text-base-color: " + colors.get(1) + ";" +
                        "-fx-background-radius: 5.0;" +
                        "-fx-border-color: " + colors.get(2) + ";" +
                        "-fx-border-radius: 5.0;" +
                        "-fx-border-width: 1 1 2 1;" +
                        "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;"));
        if (image) {
            if (darkMode) icon.setImage(darkImage);
            else icon.setImage(lightImage);
            setGraphic(icon);
        }
    }

    public enum ButtonColor {
        DEFAULT, BLUE
    }
}
