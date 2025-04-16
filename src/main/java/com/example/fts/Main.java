package com.example.fts;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load the FXML file from the correct location
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/fts/game.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("FTS");
        primaryStage.setScene(new Scene(root, 1200, 600));
        primaryStage.setResizable(false);
        primaryStage.show();

        // Make sure the scene gets focus for keyboard events
        root.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}