package org.example.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.model.ResultStore;
import org.example.model.QuizResult;
import org.example.service.Navigation;


public class result {

    @FXML
    private Label scoreLabel;
    @FXML
    private ListView<QuizResult.Item> listView;

    @FXML
    public void initialize() {
        QuizResult data = ResultStore.get();
        if (data == null) {
            scoreLabel.setText("No data.");
            return;
        }

        scoreLabel.setText("Score: " + data.getScore() + " / " + data.getTotal()
                + " (" + Math.round(data.getAccuracy() * 100) + "%)");

        listView.getItems().setAll(data.getItems());
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(QuizResult.Item it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String text =
                        "Q: " + it.getPrompt() +
                                "\n✓ Right answer: " + it.getCorrectText() +
                                "\nYou choose: " + it.getSelectedText();

                if (!it.getExplain().isBlank()) {
                    text += "\nExplanation: " + it.getExplain();
                }
                setText(text);
            }
        });
    }

    public void onPlayAgain(ActionEvent actionEvent) {
        Navigation.navigateTo("/screen/init.fxml");
    }
}

