package org.example.service;

import org.example.model.*;
import org.example.model.asianCountry.CountryFact;
import org.example.service.gen.CountryQuestionGenerator;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class QuizService {
    private DbpediaClient db = new DbpediaClient();
    private final CountryQuestionGenerator generator;
    private final Random rnd;

    private final int preloadSize;
    private final int refillThreshold;

    private final List<CountryFact> factPool = new ArrayList<>();
    private final List<String> capitalPool = new ArrayList<>();
    private final Deque<CountryFact> queue = new ArrayDeque<>();
    private final Set<String> usedCountryList = new HashSet<>();

    public QuizService() {
        this(new DbpediaClient(DbpediaClient.DEFAULT_ENDPOINT, Duration.ofSeconds(12), 2),
                new CountryQuestionGenerator(new Random()),
                16,    // preload 16 facts
                6      // when <=6 left, refill
        );
    }

    public QuizService(DbpediaClient db, CountryQuestionGenerator generator,
                       int preloadSize, int refillThreshold) {
        this.db = Objects.requireNonNull(db);
        this.generator = Objects.requireNonNull(generator);
        this.preloadSize = Math.max(8, preloadSize);
        this.refillThreshold = Math.max(4, refillThreshold);
        this.rnd = new Random();
    }

    //call once when the quiz starts
    public synchronized void preload() throws IOException, InterruptedException {
        List<CountryFact> fatcts = db.getRandomAsianCountryFacts(preloadSize);

        fatcts.removeIf(f -> isBlank(f.getCountry()));
        for(CountryFact f:fatcts) {
            if(f.getCountryUri() != null && usedCountryList.contains(f.getCountryUri())) continue;
            if(f.getCountryUri() != null && factPool.stream().anyMatch(x -> f.getCountryUri().equals(x.getCountryUri()))) continue;
            factPool.add(f);
            queue.addLast(f);
        }

        List<String> capitals = db.getAsianCapitalName(preloadSize*2);
        capitals.removeIf(QuizService::isBlank);

        Set<String> seen = new HashSet<>(capitalPool.size() + capitals.size());
        capitalPool.forEach(c -> seen.add(c.toLowerCase(Locale.ROOT).trim()));
        for(String c: capitals) {
            String key = c.toLowerCase(Locale.ROOT).trim();
            if(!seen.contains(key)) {
                capitalPool.add(c);
                seen.add(key);
            }
        }

        //Shuffle
        List<CountryFact> tmp = new ArrayList<>(queue);
        queue.clear();
        Collections.shuffle(tmp, rnd);
        tmp.forEach(queue::addLast);
    }

    public synchronized Optional<Question> getNextQuestion() throws IOException, InterruptedException {
        ensureRefillIfNeeded();

        int attempts = Math.max(8, queue.size());
        while(attempts--> 0 & !queue.isEmpty()) {
            CountryFact fact = queue.pollFirst();
            if(fact == null) break;

            if(fact.getCountryUri() != null && usedCountryList.contains(fact.getCountryUri())) continue;

            Optional<Question> q = generator.generate(fact, factPool, capitalPool);
            if(q.isPresent()) {
                if(fact.getCountryUri() != null) usedCountryList.add(fact.getCountryUri());
                return q;
            }
        }

        if(queue.isEmpty()) {
            preload();
            if(!queue.isEmpty()) {
                return getNextQuestion();
            }
        }

        return tryBuildFrom(new ArrayList<>(queue));
    }


    private Optional<Question> tryBuildFrom(List<CountryFact> candidates) {
        Collections.shuffle(candidates, rnd);
        for (CountryFact fact : candidates) {
            if (fact.getCountryUri() != null && usedCountryList.contains(fact.getCountryUri())) continue;
            var q = generator.generate(fact, factPool, capitalPool);
            if (q.isPresent()) {
                if (fact.getCountryUri() != null) usedCountryList.add(fact.getCountryUri());
                queue.remove(fact);
                return q;
            } else {
                // Drop unusable fact from queue
                queue.remove(fact);
            }
        }
        return Optional.empty();
    }


    private void ensureRefillIfNeeded() throws IOException, InterruptedException {
        if (queue.size() <= refillThreshold) {
            preload();
        }
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }


}