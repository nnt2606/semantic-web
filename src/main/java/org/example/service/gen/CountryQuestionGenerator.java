package org.example.service.gen;
import org.example.model.*;

import org.example.model.asianCountry.CountryFact;

import java.util.*;
import java.util.stream.Collectors;

public class CountryQuestionGenerator {
    private final Random rnd;

    public CountryQuestionGenerator() {
        this(new Random());
    }
    public CountryQuestionGenerator(Random rnd) {
        this.rnd = rnd;
    }

    public Optional<Question> generate(CountryFact fact, List<CountryFact> allFacts, List<String> captalPool) {
        List<Question.QuestionType> choices = new ArrayList<>(List.of(
                Question.QuestionType.COUNTRY,
                Question.QuestionType.CAPITAL
        ));
        Collections.shuffle(choices, rnd);

        for(Question.QuestionType t : choices) {
            Optional<Question> q = switch (t) {
                case COUNTRY -> capitalOfCountry(fact, captalPool);
                case CAPITAL -> countryByCapital(fact, allFacts);
            };
            if(q.isPresent()) return q;
        }
        return Optional.empty();
    }

    public Optional<Question> capitalOfCountry(CountryFact fact, List<String> capitalPool) {
        if(isBlank(fact.getCountry()) || isBlank(fact.getCapital())) {
            return Optional.empty();
        }
        String prompt = "What is the capital city of " + fact.getCountry() +"?";
        String correct = fact.getCapital();

        List<String> wrongs = capitalPool.stream()
                .filter(s -> !equalsIgnoreCaseTrim(s, correct))
                .filter(this::notBlank)
                .distinct()
                .limit(12)
                .collect(Collectors.toCollection(ArrayList::new));

        if (wrongs.size() < 3) return Optional.empty();
        Collections.shuffle(wrongs, rnd);
        wrongs = wrongs.subList(0, 3);

        List<String> options = new ArrayList<>();
        options.add(correct);
        options.addAll(wrongs);


        Question q = new Question.Builder()
                .type(Question.QuestionType.COUNTRY)
                .prompt(prompt)
                .options(options)
                .correctIndex(0)
                .explanation(null)
                .meta("country", fact.getCountry())
                .meta("capital", fact.getCapital())
                .meta("thumbnail", fact.getThumbnail())
                .buildShuffled(rnd);
        return Optional.of(q);
    }

    public Optional<Question> countryByCapital(CountryFact fact, List<CountryFact> allFacts) {
        if(isBlank(fact.getCountry()) || isBlank(fact.getCapital())) {
            return Optional.empty();
        }
        String prompt = "Which country has capital " + fact.getCapital() +"?";
        String correct = fact.getCountry();

        List<String> wrongs = allFacts.stream()
                .map(CountryFact::getCountry)
                .filter(s -> !equalsIgnoreCaseTrim(s, correct))
                .limit(12)
                .collect(Collectors.toCollection(ArrayList::new));

        if (wrongs.size() < 3) return Optional.empty();
        Collections.shuffle(wrongs, rnd);
        wrongs = wrongs.subList(0, 3);

        List<String> options = new ArrayList<>();
        options.add(correct);
        options.addAll(wrongs);


        Question q = new Question.Builder()
                .type(Question.QuestionType.COUNTRY)
                .prompt(prompt)
                .options(options)
                .correctIndex(0)
                .explanation(null)
                .meta("country", fact.getCountry())
                .meta("capital", fact.getCapital())
                .meta("thumbnail", fact.getThumbnail())
                .buildShuffled(rnd);
        return Optional.of(q);
    }


    private static boolean equalsIgnoreCaseTrim(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }
    private boolean notBlank(String s) { return !isBlank(s); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

}