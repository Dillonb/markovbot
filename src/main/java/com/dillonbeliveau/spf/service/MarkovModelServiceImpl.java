package com.dillonbeliveau.spf.service;

import com.dillonbeliveau.spf.MarkovModelService;
import com.dillonbeliveau.spf.util.EventToMessage;
import com.dillonbeliveau.spf.util.Windowed;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import me.ramswaroop.jbot.core.slack.models.Event;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

interface MarkovToToken {
    int getWeight();
    String getDisplayText();
    boolean isEnd();
    int addWeight();
    int addWeight(int weight);
}

abstract class MarkovWeightedToken implements MarkovToToken{
    private int weight;

    public MarkovWeightedToken() {
        this(0);
    }

    public MarkovWeightedToken(int weight) {
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public int addWeight() {
        return addWeight(1);
    }

    @Override
    public int addWeight(int weight) {
        this.weight += weight;
        return weight;
    }
}

class MarkovToEnd extends MarkovWeightedToken {
    @Override
    public String getDisplayText() {
        return "";
    }

    @Override
    public boolean isEnd() {
        return true;
    }
}

class MarkovToWord extends MarkovWeightedToken {
    private final String word;

    public MarkovToWord(String word) {
        this.word = word;
    }

    @Override
    public String getDisplayText() {
        return word;
    }

    @Override
    public boolean isEnd() {
        return false;
    }
}


class MarkovTransitions {
    private static final Random random = new Random();

    private List<MarkovToToken> transitions = new ArrayList<>();

    private int totalWeight = 0;

    private Map<String, MarkovToToken> cache = new HashMap<>();

    private MarkovToEnd endTokenCache = null;

    public void addTransitionToEnd() {
        if (endTokenCache == null) {
            endTokenCache = new MarkovToEnd();
            transitions.add(endTokenCache);
        }
        totalWeight += endTokenCache.addWeight();
    }

    public void addTransition(String toWord) {
        MarkovToToken transition;
        if (cache.containsKey(toWord)) {
            transition = cache.get(toWord);
        }
        else {
            transition = new MarkovToWord(toWord);
            transitions.add(transition);
            cache.put(toWord, transition);
        }

        totalWeight += transition.addWeight();
    }

    public MarkovToToken pickRandom() {
        int randomWeight = random.nextInt(totalWeight);
        for (MarkovToToken transition : transitions) {
            randomWeight -= transition.getWeight();
            if (randomWeight <= 0) {
                return transition;
            }
        }
        // Panic?
        return transitions.get(transitions.size() - 1);
    }
}

@Component
public class MarkovModelServiceImpl implements MarkovModelService {
    private static final int MIN_LENGTH_FOR_TRAINING = 20;
    private static final int MIN_LENGTH_FOR_MESSAGE = 20;
    private static final int MARKOV_DEGREE = 2;
    private static final int MIN_WORDS_FOR_TRAINING = MARKOV_DEGREE + 1;

    private Map<ImmutableList<String>, MarkovTransitions> model = new HashMap<>();

    @Override
    public String generateMessage() {
        List<String> words = new ArrayList<>();

        MarkovToToken lastToken;

        do {
            int skip = words.size() - MARKOV_DEGREE;
            skip = Math.max(skip, 0);
            MarkovTransitions transitions = model.get(words.stream().skip(skip).limit(MARKOV_DEGREE).collect(ImmutableList.toImmutableList()));
            lastToken = transitions.pickRandom();

            words.add(lastToken.getDisplayText());
        } while (!lastToken.isEnd());

        return Joiner.on(" ").join(words).trim();
    }

    @Override
    public void trainOnEvent(Event event) {
        EventToMessage.singleton.apply(event)
                .filter(text -> text.length() >= MIN_LENGTH_FOR_TRAINING)
                .ifPresent(this::train);
    }

    private void train(ImmutableList<String> from, String to) {
        model.computeIfAbsent(from, key -> new MarkovTransitions()).addTransition(to);
    }

    private void trainToEnd(ImmutableList<String> from) {
        model.computeIfAbsent(from, key -> new MarkovTransitions()).addTransitionToEnd();
    }

    private void train(String line) {
        List<String> words = Arrays.stream(line.split("\\s"))
                .filter(Strings::isNotBlank)
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (words.size() < MIN_WORDS_FOR_TRAINING) {
            // Too few words to train on
            return;
        }

        // Populate start transition, these will always use less than MARKOV_DEGREE words, so we have a starting point.
        for (int numWords = 0; numWords < MARKOV_DEGREE; numWords++) {
            ImmutableList<String> from = words.stream().limit(numWords).collect(ImmutableList.toImmutableList());
            String to = words.get(numWords);
            train(from, to);
        }

        // Window by the markov degree + 1 more for the word to transition to
        Windowed<String> windowed = new Windowed<>(words, MARKOV_DEGREE + 1);

        windowed.forEachRemaining(window -> {
            ImmutableList<String> from = window.stream().limit(MARKOV_DEGREE).collect(ImmutableList.toImmutableList());
            String to = window.get(window.size() - 1);
            train(from, to);
        });

        // Populate end transition, so it'll be impossible to end up in an infinite loop.
        ImmutableList<String> from = words.stream().skip(words.size() - MARKOV_DEGREE).limit(MARKOV_DEGREE).collect(ImmutableList.toImmutableList());
        trainToEnd(from);
    }
}
