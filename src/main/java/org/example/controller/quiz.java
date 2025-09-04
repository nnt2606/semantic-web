package org.example.controller;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.example.model.ResultStore;
import org.example.model.Question;
import org.example.model.QuizResult;
import org.example.service.QuizService;
import org.example.service.Navigation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller khớp với FXML:
 * - fx:controller="org.example.controller.QuizController"
 * - fx:id: lblCategory, lblTimer, lblScore, imgPrompt, lblQuestion, btnA, btnB, btnC, btnD
 *
 * Flow:
 * - Timer callback từ QuizService -> Platform.runLater cập nhật UI.
 * - Người dùng Answer/Skip -> lock nút, hiển thị đúng/sai -> auto next sau 1.2s.
 * - Hết câu -> goToResult() (bạn nối scene sang Result).
 */
public class quiz {

    @FXML private Label questionLabel;
    @FXML private ToggleGroup optionsGroup;   // gắn group trong FXML
    @FXML private RadioButton optionA;
    @FXML private RadioButton optionB;
    @FXML private RadioButton optionC;
    @FXML private RadioButton optionD;
    @FXML private Button nextButton;
    @FXML private ImageView thumbnail;

    private final QuizService quizService = new QuizService();
    private Question current;

    // ====== cấu hình quiz ======
    private static final int MAX_QUESTIONS = 10;

    // ====== trạng thái runtime ======
    private int index = 0;        // đếm câu hiện tại (1..MAX_QUESTIONS)
    private int score = 0;        // số câu đúng
    private final List<AnswerReview> history = new ArrayList<>();

    // Lưu thông tin để hiển thị lời giải ở màn Result
    private static final class AnswerReview {
        final Question q;
        final int selectedIndex;   // -1 nếu bỏ qua
        final boolean correct;
        AnswerReview(Question q, int selectedIndex, boolean correct) {
            this.q = q; this.selectedIndex = selectedIndex; this.correct = correct;
        }
    }

    @FXML
    public void initialize() {
        setUiEnabled(false);
        nextButton.setOnAction(evt -> onNext());

        // preload + lấy câu đầu
        CompletableFuture.runAsync(() -> {
            try {
                quizService.preload();
                current = quizService.getNextQuestion().orElse(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).thenRunAsync(() -> {
            setUiEnabled(true);
            if (current != null) {
                index = 1;
                render(current);
                updateNextButtonText();
            } else {
                questionLabel.setText("Không tải được câu hỏi.");
            }
        }, Platform::runLater);
    }

    private void setUiEnabled(boolean enabled) {
        questionLabel.setDisable(!enabled);
        optionA.setDisable(!enabled);
        optionB.setDisable(!enabled);
        optionC.setDisable(!enabled);
        optionD.setDisable(!enabled);
        nextButton.setDisable(!enabled);
    }

    private void render(Question q) {
        questionLabel.setText(q.getPrompt());
        var opts = q.getOptions();
        optionA.setText(opts.get(0));
        optionB.setText(opts.get(1));
        optionC.setText(opts.get(2));
        optionD.setText(opts.get(3));
        optionsGroup.selectToggle(null);

        // thumbnail (nếu có)
        String url = q.getMeta().get("thumbnail");
        if (url != null && !url.isBlank()) {
            try {
                thumbnail.setImage(new Image(url, true)); // async
                thumbnail.setVisible(true);
            } catch (Exception ignored) {
                thumbnail.setVisible(false);
            }
        } else {
            thumbnail.setVisible(false);
        }
    }

    private void onNext() {
        // chấm điểm cho câu hiện tại
        int selected = selectedIndex();
        if (current != null) {
            boolean correct = (selected != -1) && (selected == current.getCorrectIndex());
            if (correct) score++;
            history.add(new AnswerReview(current, selected, correct));
        }

        // Nếu đã đủ 10 câu -> sang Result
        if (index >= MAX_QUESTIONS) {
            goToResult();
            return;
        }

        // Lấy câu kế
        setUiEnabled(false);
        CompletableFuture.supplyAsync(() -> {
            try {
                return quizService.getNextQuestion().orElse(null);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).thenAcceptAsync(q -> {
            setUiEnabled(true);
            if (q != null) {
                current = q;
                index++;
                render(q);
                updateNextButtonText();
            } else {
                // Nếu service hết câu sớm hơn 10 → vẫn kết thúc và show kết quả
                goToResult();
            }
        }, Platform::runLater);
    }

    private void updateNextButtonText() {
        nextButton.setText(index >= MAX_QUESTIONS ? "Finish" : "Next");
    }

    private int selectedIndex() {
        var sel = optionsGroup.getSelectedToggle();
        if (sel == null) return -1;
        if (sel == optionA) return 0;
        if (sel == optionB) return 1;
        if (sel == optionC) return 2;
        if (sel == optionD) return 3;
        return -1;
    }

    private void goToResult() {

        List<QuizResult.Item> items = history.stream().map(ar -> {
            var q = ar.q;
            String prompt = q.getPrompt();
            List<String> opts = q.getOptions();
            int correctIdx = q.getCorrectIndex();
            int selectedIdx = ar.selectedIndex;
            String explain = q.getMeta().getOrDefault("explain", "");
            String thumb = q.getMeta().getOrDefault("thumbnail", "");
            return new QuizResult.Item(prompt, opts, correctIdx, selectedIdx, explain, thumb);
        }).collect(java.util.stream.Collectors.toList());

        QuizResult result = new QuizResult(score, history.size(), items);

        // Đặt vào store rồi chuyển scene
        ResultStore.set(result);
        Navigation.navigateTo("/screen/result.fxml");

    }


}

