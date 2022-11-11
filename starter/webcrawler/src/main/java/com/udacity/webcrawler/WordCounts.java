package com.udacity.webcrawler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class that sorts the map of word counts.
 */
final class WordCounts {

    private WordCounts() {
        // This class cannot be instantiated
    }

    /**
     * Given an unsorted map of word counts, returns a new map whose word counts are sorted according
     * to the provided { WordCountComparator}, and includes only the top
     * {@param popluarWordCount} words and counts.
     * <p>
     * *
     *
     * @param wordCounts       the unsorted map of word counts.
     * @param popularWordCount the number of popular words to include in the result map.
     * @return a map containing the top {@param popularWordCount} words and counts in the right order.
     */
    static Map<String, Integer> sort(Map<String, Integer> wordCounts, int popularWordCount) {


        return wordCounts.entrySet()
                .stream()
                .sorted((Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) -> {
                            if (!a.getValue().equals(b.getValue())) {
                                return b.getValue() - a.getValue();
                            }
                            if (a.getKey().length() != b.getKey().length()) {
                                return b.getKey().length() - a.getKey().length();
                            }
                            return a.getKey().compareTo(b.getKey());
                        }
                )
                .limit(Math.min(popularWordCount, wordCounts.size()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> y, LinkedHashMap::new));

    }
}