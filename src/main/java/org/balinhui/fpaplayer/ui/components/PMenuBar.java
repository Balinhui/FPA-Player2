package org.balinhui.fpaplayer.ui.components;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import java.util.HashMap;
import java.util.Map;

public final class PMenuBar extends HBox {
    private final Map<String, PButton> buttonMap = new HashMap<>();
    public PMenuBar() {
        setAlignment(Pos.CENTER);
        //setStyle("-fx-background-color:white");
    }

    public void addItem(String name, EventHandler<ActionEvent> event) {
        PButton button = new PButton(name);
        button.setOnAction(event);
        getChildren().add(button);
        buttonMap.put(name, button);
    }

    public PButton getItemButton(String name) {
        return buttonMap.get(name);
    }

    public PButton[] getItems() {
        PButton[] buttons = new PButton[buttonMap.size()];
        int i = 0;
        for (Map.Entry<String, PButton> buttonEntry : buttonMap.entrySet()) {
            buttons[i] = buttonEntry.getValue();
            i++;
        }
        return buttons;
    }

    public void setDarkMode(boolean darkMode) {
        buttonMap.forEach((name, button) -> {
            button.setDarkMode(darkMode);
        });
    }
}
