package com.resolvr.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides text normalization and Jaccard + token overlap similarity scoring for Duplicate Incident Detection.
 */
public final class TextSimilarityUtil {

    private static final Set<String> STOP_WORDS = Set.of(
        "the", "and", "for", "with", "that", "this", "from", "are", "was", "were",
        "have", "has", "had", "not", "but", "what", "all", "when", "can", "into",
        "our", "out", "about", "who", "get", "which", "their", "unable", "issue", "problem"
    );

    private TextSimilarityUtil() {
    }

    public static Set<String> tokenizeAndNormalize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        return Arrays.stream(normalized.split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 3 && !STOP_WORDS.contains(token))
                .collect(Collectors.toSet());
    }

    public static double calculateJaccardSimilarity(String textA, String textB) {
        Set<String> tokensA = tokenizeAndNormalize(textA);
        Set<String> tokensB = tokenizeAndNormalize(textB);

        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        if (union.isEmpty()) {
            return 0.0;
        }
        return (double) intersection.size() / union.size();
    }
}
