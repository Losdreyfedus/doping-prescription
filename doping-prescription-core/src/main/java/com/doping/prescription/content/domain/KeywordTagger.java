package com.doping.prescription.content.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Suggests topics for a video from its title and description by keyword rules (docs/algoritma.md §5). Pure and
 * deterministic: the same video and dictionary always give the same suggestions.
 *
 * <p>Turkish is agglutinative, so a keyword token matches a word that starts with it ({@code kume} matches
 * {@code kumeler}). A final p/t/k may also appear softened as b/d/g ({@code olasilik} matches {@code olasiligin}). Tokens
 * shorter than {@value #PREFIX_MATCH_MIN_LENGTH} characters must match a whole word ({@code us} does not match
 * {@code ustalik}). A multi-word keyword must match consecutive words.
 */
public final class KeywordTagger implements ContentTagger {

    /** docs/algoritma.md §5: shorter tokens match whole words only. */
    static final int PREFIX_MATCH_MIN_LENGTH = 4;

    /** docs/algoritma.md §5: a keyword in the title counts fully, one only in the description counts half. */
    static final double TITLE_FACTOR = 1.0;

    static final double DESCRIPTION_FACTOR = 0.5;

    private static final double MAX_SCORE = 1.0;
    private static final String HARD_CONSONANTS = "ptk";
    private static final String SOFTENED_CONSONANTS = "bdg";

    private final double minConfidence;

    /** @param minConfidence lowest score that still becomes a suggestion ({@code content.tagging.min-confidence}) */
    public KeywordTagger(double minConfidence) {
        if (minConfidence < 0 || minConfidence > MAX_SCORE) {
            throw new IllegalArgumentException("minConfidence must be between 0 and 1");
        }
        this.minConfidence = minConfidence;
    }

    /** Topics whose score reaches {@code minConfidence}, highest score first. */
    @Override
    public List<TagSuggestion> suggest(String title, String description, List<TaggingKeyword> dictionary) {
        List<String> titleTokens = TextNormalizer.tokens(title);
        List<String> descriptionTokens = TextNormalizer.tokens(description);
        Map<Integer, Double> scores = new HashMap<>();
        Set<Integer> excludedTopics = new HashSet<>();

        for (TaggingKeyword keyword : dictionary) {
            double factor = matchFactor(keyword.tokens(), titleTokens, descriptionTokens);
            if (factor == 0) {
                continue;
            }
            if (keyword.negative()) {
                excludedTopics.add(keyword.topicId());
            } else {
                scores.merge(keyword.topicId(), keyword.weight() * factor, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .filter(entry -> !excludedTopics.contains(entry.getKey()))
                .map(entry -> new TagSuggestion(entry.getKey(), Math.min(entry.getValue(), MAX_SCORE)))
                .filter(suggestion -> suggestion.score() >= minConfidence)
                .sorted(Comparator.comparingDouble(TagSuggestion::score)
                        .reversed()
                        .thenComparing(TagSuggestion::topicId))
                .toList();
    }

    private static double matchFactor(
            List<String> keywordTokens, List<String> titleTokens, List<String> descriptionTokens) {
        if (containsPhrase(titleTokens, keywordTokens)) {
            return TITLE_FACTOR;
        }
        if (containsPhrase(descriptionTokens, keywordTokens)) {
            return DESCRIPTION_FACTOR;
        }
        return 0;
    }

    private static boolean containsPhrase(List<String> words, List<String> keywordTokens) {
        for (int start = 0; start + keywordTokens.size() <= words.size(); start++) {
            if (matchesAt(words, start, keywordTokens)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAt(List<String> words, int start, List<String> keywordTokens) {
        for (int offset = 0; offset < keywordTokens.size(); offset++) {
            if (!tokenMatches(keywordTokens.get(offset), words.get(start + offset))) {
                return false;
            }
        }
        return true;
    }

    static boolean tokenMatches(String keywordToken, String word) {
        Objects.requireNonNull(word, "word");
        if (keywordToken.length() < PREFIX_MATCH_MIN_LENGTH) {
            return word.equals(keywordToken);
        }
        return word.startsWith(keywordToken) || word.startsWith(softenedForm(keywordToken));
    }

    /** {@code olasilik} -> {@code olasilig}; other endings are returned unchanged. */
    private static String softenedForm(String token) {
        int last = token.length() - 1;
        int hardIndex = HARD_CONSONANTS.indexOf(token.charAt(last));
        if (hardIndex < 0) {
            return token;
        }
        return token.substring(0, last) + SOFTENED_CONSONANTS.charAt(hardIndex);
    }
}
