package org.example.model;


import java.util.*;

public class Question {
    public enum QuestionType {
        CAPITAL,
        COUNTRY
    }

    private final String id;
    private final QuestionType type;
    private final String prompt;
    private final List<String> options;
    private final int correctIndex;
    private final String explanation;
    private final Map<String, String> meta;

    public Question(String id, QuestionType type, String prompt, List<String> options, int correctIndex, String explanation, Map<String, String> meta) {
        this.id = id;
        this.type = type;
        this.prompt = prompt;
        this.options = options;
        this.correctIndex = correctIndex;
        this.explanation = explanation;
        this.meta = meta;
    }

    public String getId() {
        return id;
    }

    public QuestionType getType() {
        return type;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private QuestionType type;
        private String prompt;
        private List<String> options = new ArrayList<>();
        private int correctIndex;
        private String explanation;
        private Map<String, String> meta = new HashMap<>();


        public Builder id(String id) { this.id = id; return this; }
        public Builder type(QuestionType type) { this.type = type; return this; }
        public Builder prompt(String prompt) { this.prompt = prompt; return this; }
        public Builder options(List<String> options) { this.options = new ArrayList<>(options); return this; }
        public Builder correctIndex(int idx) { this.correctIndex = idx; return this; }
        public Builder explanation(String explanation) { this.explanation = explanation; return this; }
        public Builder meta(String key, String value) { if (value != null) this.meta.put(key, value); return this; }


        public Question buildShuffled(Random rnd) {
            if (options == null || options.size() < 2) throw new IllegalStateException("Need at least 2 options");
            String correct = options.get(correctIndex);
            List<String> shuffled = new ArrayList<>(options);
            Collections.shuffle(shuffled, rnd);
            int newIdx = shuffled.indexOf(correct);
            return new Question(id, type, prompt, shuffled, newIdx, explanation, meta);
        }

    }
}