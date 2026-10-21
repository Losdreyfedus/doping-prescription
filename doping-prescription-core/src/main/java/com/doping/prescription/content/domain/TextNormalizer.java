package com.doping.prescription.content.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes Turkish text for keyword matching (docs/algoritma.md §5): Turkish lower case, Turkish letters folded to
 * ASCII, punctuation turned into spaces and whitespace collapsed: {@code Olasılığın temeli!} becomes
 * {@code olasiligin temeli}.
 */
public final class TextNormalizer {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    // Circumflex vowels (kâr, hâlâ) are folded too; otherwise the punctuation step would split the word.
    private static final String TURKISH_LETTERS = "çğıöşüâîû";
    private static final String ASCII_LETTERS = "cgiosuaiu";

    private TextNormalizer() {}

    public static String normalize(String text) {
        String lowerCase = text.toLowerCase(TURKISH);
        StringBuilder folded = new StringBuilder(lowerCase.length());
        for (char character : lowerCase.toCharArray()) {
            int turkishIndex = TURKISH_LETTERS.indexOf(character);
            folded.append(turkishIndex >= 0 ? ASCII_LETTERS.charAt(turkishIndex) : character);
        }
        return NON_ALPHANUMERIC.matcher(folded).replaceAll(" ").strip();
    }

    public static List<String> tokens(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(normalized.split(" "));
    }
}
