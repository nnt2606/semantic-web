package org.example.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class QuizResult {

    private final int score;
    private final int total;
    private final List<Item> items;

    public QuizResult(int score, int total, List<Item> items) {
        this.score = score;
        this.total = total;
        this.items = items == null ? List.of() : List.copyOf(items);
    }

    public int getScore() { return score; }
    public int getTotal() { return total; }
    public List<Item> getItems() { return Collections.unmodifiableList(items); }

    /** Tỉ lệ đúng (0..1) */
    public double getAccuracy() {
        return total == 0 ? 0.0 : (double) score / (double) total;
    }

    /** Mục kết quả cho 1 câu hỏi */
    public static class Item {
        private final String prompt;
        private final List<String> options;   // size = 4 theo thiết kế hiện tại
        private final int correctIndex;       // 0..3
        private final int selectedIndex;      // 0..3 hoặc -1 nếu bỏ qua
        private final String explain;         // có thể rỗng
        private final String thumbnail;       // URL có thể rỗng

        public Item(String prompt,
                    List<String> options,
                    int correctIndex,
                    int selectedIndex,
                    String explain,
                    String thumbnail) {
            this.prompt = Objects.requireNonNullElse(prompt, "");
            this.options = options == null ? List.of() : List.copyOf(options);
            this.correctIndex = correctIndex;
            this.selectedIndex = selectedIndex;
            this.explain = Objects.requireNonNullElse(explain, "");
            this.thumbnail = Objects.requireNonNullElse(thumbnail, "");
        }

        public String getPrompt() { return prompt; }
        public String getExplain() { return explain; }
        public String getThumbnail() { return thumbnail; }

        public boolean isAnswered() { return selectedIndex >= 0; }
        public String getCorrectText() {
            return (options.size() > correctIndex && correctIndex >= 0) ? options.get(correctIndex) : "";
        }
        public String getSelectedText() {
            return (isAnswered() && options.size() > selectedIndex) ? options.get(selectedIndex) : "(bỏ qua)";
        }
    }

}
