package org.example.controller;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.example.service.Navigation;

import java.io.IOException;

public class init {
    private Button btnPlay;

    public void onClickPlay(javafx.event.ActionEvent event) throws IOException {
        Navigation.setStage(new Stage());
        Navigation.navigateTo("/screen/quiz.fxml");
        Stage oldStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        oldStage.close();
    }
}
