package com.doping.prescription.quiz.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Shuffles a question's options and relabels them A, B, C, ... (docs/api.md: the mapping travels inside the quiz
 * token, so a shared screenshot does not give away the answer letter). Pure.
 */
public final class OptionShuffle {

    private OptionShuffle() {}

    /**
     * @param options stored key → text, in stored order
     * @return the shown options and, for each shown key, the stored key
     */
    public static Shuffled shuffle(Map<String, String> options, RandomGenerator random) {
        List<String> storedKeys = new ArrayList<>(options.keySet());
        Collections.shuffle(storedKeys, random);
        Map<String, String> shown = new LinkedHashMap<>();
        Map<String, String> shownToStored = new LinkedHashMap<>();
        for (int index = 0; index < storedKeys.size(); index++) {
            String shownKey = String.valueOf((char) ('A' + index));
            shown.put(shownKey, options.get(storedKeys.get(index)));
            shownToStored.put(shownKey, storedKeys.get(index));
        }
        return new Shuffled(shown, shownToStored);
    }

    /**
     * @param shown         shown key → text, in display order
     * @param shownToStored shown key → stored key
     */
    public record Shuffled(Map<String, String> shown, Map<String, String> shownToStored) {

        public Shuffled {
            shown = Collections.unmodifiableMap(new LinkedHashMap<>(shown));
            shownToStored = Collections.unmodifiableMap(new LinkedHashMap<>(shownToStored));
        }

        /** The shown letter of a stored key. */
        public static String shownKeyOf(Map<String, String> shownToStored, String storedKey) {
            return shownToStored.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(storedKey))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("stored key not in the mapping"));
        }
    }
}
