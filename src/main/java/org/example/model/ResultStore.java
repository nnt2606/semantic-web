package org.example.model;

public class ResultStore {
    public static QuizResult lastResult;

    private ResultStore() {}

    public static void set(QuizResult result) {
        lastResult = result;
    }

    public static QuizResult get(){
        return lastResult;
    }

    public static void clear() {
        lastResult = null;
    }
}
