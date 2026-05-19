package com.travel.system.service;

import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.model.Destination;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class DestinationMatchService {
    private final DestinationService destinationService;

    public DestinationMatchService(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    public MatchResult match(ExtractedGuidePlan.Place place) {
        if (place == null || !hasText(place.getName())) {
            return MatchResult.unmatched(place, "Place name is empty");
        }
        String rawName = place.getName().trim();
        List<Destination> destinations = destinationService.findAll();
        for (Destination destination : destinations) {
            if (rawName.equals(destination.getName())) {
                return withCoordinates(place, destination, 0.98);
            }
        }
        String normalizedRaw = normalize(rawName);
        for (Destination destination : destinations) {
            if (normalizedRaw.equals(normalize(destination.getName()))) {
                return withCoordinates(place, destination, 0.92);
            }
        }
        List<DestinationScore> scored = destinationService.searchForRoute(rawName, 5)
                .stream()
                .map(destination -> new DestinationScore(destination, score(normalizedRaw, normalize(destination.getName()))))
                .filter(candidate -> candidate.score() >= 0.45)
                .sorted(Comparator.comparing(DestinationScore::score).reversed())
                .toList();
        if (scored.isEmpty()) {
            return MatchResult.unmatched(place, "No matching destination found");
        }
        if (scored.size() > 1 && scored.get(0).score() - scored.get(1).score() < 0.20) {
            return MatchResult.unmatched(place, "Ambiguous destination match");
        }
        return withCoordinates(place, scored.get(0).destination(), Math.max(0.70, scored.get(0).score()));
    }

    private MatchResult withCoordinates(ExtractedGuidePlan.Place place, Destination destination, double confidence) {
        if (destination.getLatitude() == null || destination.getLongitude() == null) {
            return MatchResult.unmatched(place, "Matched destination is missing coordinates");
        }
        return MatchResult.matched(place, destination, confidence);
    }

    private double score(String raw, String candidate) {
        if (candidate.equals(raw)) {
            return 1.0;
        }
        if (candidate.contains(raw) || raw.contains(candidate)) {
            return 0.72;
        }
        int common = 0;
        String[] tokens = raw.split(" ");
        for (String token : tokens) {
            if (!token.isBlank() && candidate.contains(token)) {
                common++;
            }
        }
        return common / (double) Math.max(1, tokens.length);
    }

    private String normalize(String value) {
        String text = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        text = Normalizer.normalize(text, Normalizer.Form.NFKC);
        return text.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ").trim().replaceAll("\\s+", " ");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record DestinationScore(Destination destination, double score) {
    }

    public record MatchResult(ExtractedGuidePlan.Place place, Destination destination, double confidence, String reason) {
        public boolean matched() {
            return destination != null;
        }

        public static MatchResult matched(ExtractedGuidePlan.Place place, Destination destination, double confidence) {
            return new MatchResult(place, destination, confidence, "");
        }

        public static MatchResult unmatched(ExtractedGuidePlan.Place place, String reason) {
            return new MatchResult(place, null, 0.0, reason);
        }
    }
}
