package com.anders.poewishlist.util;

import com.opencsv.CSVReader;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UniqueItemMatcher {
    private final List<String> uniques = new ArrayList<>();
    private final LevenshteinDistance ld = LevenshteinDistance.getDefaultInstance();

    // if you are off by more than 3 characters in the unique name, you don't deserve it anyway
    private static final int MAX_DIST = 3;

    public UniqueItemMatcher() {
        loadFromCsv();
    }

    private void loadFromCsv() {
        try (
                Reader reader = new InputStreamReader(
                        getClass().getResourceAsStream("/uniques.csv"), StandardCharsets.UTF_8
                );
                CSVReader csv = new CSVReader(reader)
        ) {
            String[] row;
            while ((row = csv.readNext()) != null) {
                for (String name : row) {
                    if (name != null && !name.isBlank()) {
                        uniques.add(name.trim());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load uniques.csv", e);
        }
    }

    /**
     * Returns the closest matching unique name, or null if none within MAX_DIST.
     */
    public String match(String input) {
        if (input == null || input.isBlank()) return null;
        String lower = input.toLowerCase();
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : uniques) {
            int dist = ld.apply(lower, candidate.toLowerCase());
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return (bestDist <= MAX_DIST) ? best : null;
    }
}