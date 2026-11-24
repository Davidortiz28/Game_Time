package com.gametime.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GameTimeClient extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("GameTime · Cliente Windows en preparación");
        label.setStyle("-fx-font-size: 18px; -fx-text-fill: #f9fafb;");

        StackPane root = new StackPane(label);
        root.setStyle("-fx-background-color: #111827;");

        stage.setTitle("GameTime");
        stage.setScene(new Scene(root, 720, 420));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
