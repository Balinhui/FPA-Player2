package org.balinhui.fpaplayer.ui.components;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.List;
import java.util.Random;

public class PBackground extends StackPane {

    private final Pane auroraLayer = new Pane();
    private final Region darkOverlay = new Region();

    private final Random random = new Random();

    public PBackground(
            double width,
            double height,
            List<Color> palette
    ) {
        setPrefSize(width, height);

        auroraLayer.setPrefSize(width, height);

        generateAuroraBlobs(width, height, palette);

        darkOverlay.setPrefSize(width, height);

        darkOverlay.setBackground(
                new Background(
                        new BackgroundFill(
                                Color.rgb(0, 0, 0, 0.25),
                                CornerRadii.EMPTY,
                                Insets.EMPTY
                        )
                )
        );

        getChildren().addAll(
                auroraLayer,
                darkOverlay
        );
    }

    private void generateAuroraBlobs(
            double width,
            double height,
            List<Color> palette
    ) {
        int blobCount = Math.clamp(palette.size(), 3, 6);

        for (int i = 0; i < blobCount; i++) {
            Color baseColor = palette.get(i % palette.size());

            Circle blob = createBlob(
                    width,
                    height,
                    baseColor
            );

            auroraLayer.getChildren().add(blob);

            animateBlob(blob, width, height);
        }
    }

    private Circle createBlob(
            double width,
            double height,
            Color baseColor
    ) {
        double radius = 200 + random.nextDouble() * 250;

        Color glowColor = Color.hsb(
                baseColor.getHue(),
                Math.min(1.0, baseColor.getSaturation() * 0.85),
                Math.min(1.0, baseColor.getBrightness() * 1.1),
                0.45
        );

        Circle blob = new Circle(radius);

        blob.setFill(glowColor);

        blob.setEffect(
                new GaussianBlur(radius * 0.4)
        );

        // 真正随机分布到整个屏幕
        blob.setCenterX(random.nextDouble() * width);
        blob.setCenterY(random.nextDouble() * height);

        return blob;
    }

    private void animateBlob(
            Circle blob,
            double width,
            double height
    ) {
        Timeline movement = new Timeline(
                new KeyFrame(
                        Duration.ZERO,
                        new KeyValue(
                                blob.centerXProperty(),
                                blob.getCenterX()
                        ),
                        new KeyValue(
                                blob.centerYProperty(),
                                blob.getCenterY()
                        )
                ),
                new KeyFrame(
                        Duration.seconds(
                                12 + random.nextDouble() * 12
                        ),
                        new KeyValue(
                                blob.centerXProperty(),
                                random.nextDouble() * width
                        ),
                        new KeyValue(
                                blob.centerYProperty(),
                                random.nextDouble() * height
                        )
                )
        );

        movement.setAutoReverse(true);
        movement.setCycleCount(Animation.INDEFINITE);

        ScaleTransition pulse =
                new ScaleTransition(
                        Duration.seconds(
                                8 + random.nextDouble() * 8
                        ),
                        blob
                );

        pulse.setToX(
                1.2 + random.nextDouble() * 0.3
        );

        pulse.setToY(
                1.2 + random.nextDouble() * 0.3
        );

        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);

        FadeTransition fade =
                new FadeTransition(
                        Duration.seconds(
                                10 + random.nextDouble() * 10
                        ),
                        blob
                );

        fade.setToValue(
                0.25 + random.nextDouble() * 0.3
        );

        fade.setAutoReverse(true);
        fade.setCycleCount(Animation.INDEFINITE);

        new ParallelTransition(
                movement,
                pulse,
                fade
        ).play();
    }
}
