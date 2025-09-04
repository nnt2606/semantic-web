package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.service.Navigation;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Navigation.setStage(primaryStage);
        Navigation.navigateTo("/screen/init.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
