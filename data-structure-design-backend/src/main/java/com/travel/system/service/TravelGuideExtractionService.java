package com.travel.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.system.config.LlmProperties;
import com.travel.system.dto.AgentChatMessage;
import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.llm.LlmChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TravelGuideExtractionService {
    private static final Pattern FALLBACK_PLACE_PATTERN = Pattern.compile(
            "\\b([A-Z][A-Za-z]+(?:\\s+[A-Z][A-Za-z]+){0,3})\\b");
    private static final int DEFAULT_STAY_MINUTES = 120;

    private final LlmChatClient llmChatClient;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public TravelGuideExtractionService(LlmChatClient llmChatClient,
                                        LlmProperties properties,
                                        ObjectMapper objectMapper) {
        this.llmChatClient = llmChatClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public ExtractedGuidePlan extract(String text) {
        String normalizedText = text == null ? "" : text.trim();
        if (normalizedText.isBlank()) {
            throw new IllegalArgumentException("Guide text is required");
        }
        if (!isConfigured()) {
            ExtractedGuidePlan fallback = fallbackExtract(normalizedText);
            fallback.getWarnings().add("LLM is not configured; used local text extraction");
            return fallback;
        }
        try {
            String content = llmChatClient.chat(systemPrompt(), List.of(new AgentChatMessage("user", normalizedText)));
            return normalizePlan(objectMapper.readValue(cleanJson(content), ExtractedGuidePlan.class), normalizedText);
        } catch (Exception exception) {
            ExtractedGuidePlan fallback = fallbackExtract(normalizedText);
            fallback.getWarnings().add("LLM extraction failed; used local text extraction: " + exception.getClass().getSimpleName());
            return fallback;
        }
    }

    private boolean isConfigured() {
        return properties != null
            && properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank()
            && properties.getApiKey() != null && !properties.getApiKey().isBlank()
            && properties.getModel() != null && !properties.getModel().isBlank();
    }

    private String systemPrompt() {
        return """
                You extract travel itinerary structure from user-provided travel guide text.
                Return strict JSON only with this schema:
                {"title":"string","summary":"string","places":[{"name":"string","dayIndex":1,"orderIndex":0,"stayMinutes":120,"reason":"string","confidence":0.8}],"warnings":[]}
                Extract only places present or clearly implied in the input. Preserve visit order.
                Use conservative stayMinutes: landmarks 45-90, museums/parks/scenic areas 90-180, meals/cafes 45-75.
                If day grouping is unclear, use dayIndex 1. Do not invent coordinates or facts.
                """;
    }

    private ExtractedGuidePlan normalizePlan(ExtractedGuidePlan plan, String sourceText) {
        ExtractedGuidePlan safe = plan == null ? new ExtractedGuidePlan() : plan;
        if (!hasText(safe.getTitle())) {
            safe.setTitle("Imported Travel Guide");
        }
        if (safe.getSummary() == null) {
            safe.setSummary("");
        }
        if (safe.getPlaces() == null) {
            safe.setPlaces(new ArrayList<>());
        }
        if (safe.getWarnings() == null) {
            safe.setWarnings(new ArrayList<>());
        }
        List<ExtractedGuidePlan.Place> normalizedPlaces = new ArrayList<>();
        int index = 0;
        for (ExtractedGuidePlan.Place place : safe.getPlaces()) {
            if (place == null || !hasText(place.getName())) {
                continue;
            }
            place.setName(place.getName().trim());
            place.setDayIndex(place.getDayIndex() == null || place.getDayIndex() < 1 ? 1 : place.getDayIndex());
            place.setOrderIndex(place.getOrderIndex() == null || place.getOrderIndex() < 0 ? index : place.getOrderIndex());
            place.setStayMinutes(place.getStayMinutes() == null || place.getStayMinutes() < 0 ? DEFAULT_STAY_MINUTES : place.getStayMinutes());
            place.setReason(place.getReason() == null ? "" : place.getReason().trim());
            place.setConfidence(place.getConfidence() == null ? 0.75 : Math.max(0.0, Math.min(1.0, place.getConfidence())));
            normalizedPlaces.add(place);
            index++;
        }
        normalizedPlaces.sort(Comparator.comparing(ExtractedGuidePlan.Place::getDayIndex)
                .thenComparing(ExtractedGuidePlan.Place::getOrderIndex));
        safe.setPlaces(normalizedPlaces);
        if (safe.getPlaces().isEmpty()) {
            return fallbackExtract(sourceText);
        }
        return safe;
    }

    private ExtractedGuidePlan fallbackExtract(String text) {
        ExtractedGuidePlan plan = new ExtractedGuidePlan();
        plan.setTitle("Imported Travel Guide");
        plan.setSummary("Extracted from guide text using local rules.");
        List<ExtractedGuidePlan.Place> places = new ArrayList<>();
        for (String segment : text.split("[\\r\\n,，。；;]+")) {
            String name = segment.trim();
            if (hasCjk(name) && name.length() >= 2 && name.length() <= 30 && !containsName(places, name)) {
                places.add(new ExtractedGuidePlan.Place(name, 1, places.size(), DEFAULT_STAY_MINUTES, "Matched by text line", 0.65));
            }
        }
        Matcher matcher = FALLBACK_PLACE_PATTERN.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            if (name.length() < 3 || isStopPhrase(name) || containsName(places, name)) {
                continue;
            }
            places.add(new ExtractedGuidePlan.Place(name, 1, places.size(), DEFAULT_STAY_MINUTES, "Matched by text appearance", 0.65));
        }
        plan.setPlaces(places);
        return plan;
    }

    private boolean hasCjk(String value) {
        return value != null && value.codePoints().anyMatch(codePoint ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private boolean containsName(List<ExtractedGuidePlan.Place> places, String name) {
        return places.stream().anyMatch(place -> place.getName().equalsIgnoreCase(name));
    }

    private boolean isStopPhrase(String value) {
        String normalized = value.toLowerCase();
        return normalized.equals("first") || normalized.equals("then") || normalized.equals("visit") || normalized.equals("go");
    }

    private String cleanJson(String content) {
        String value = content == null ? "" : content.trim();
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return value.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
