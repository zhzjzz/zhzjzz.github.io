package com.travel.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.system.config.LlmProperties;
import com.travel.system.dto.AgentChatMessage;
import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.llm.LlmChatClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TravelGuideExtractionServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesStrictJsonFromLlm() {
        LlmProperties properties = configuredProperties();
        TravelGuideExtractionService service = new TravelGuideExtractionService(
                (systemPrompt, messages) -> """
                        {
                          "title": "Shanghai Classic",
                          "summary": "A compact city route",
                          "places": [
                            {"name":"The Bund","dayIndex":1,"orderIndex":0,"stayMinutes":90,"reason":"evening view","confidence":0.93}
                          ],
                          "warnings": []
                        }
                        """,
                properties,
                objectMapper);

        ExtractedGuidePlan plan = service.extract("Visit The Bund in the evening.");

        assertThat(plan.getTitle()).isEqualTo("Shanghai Classic");
        assertThat(plan.getPlaces()).hasSize(1);
        assertThat(plan.getPlaces().get(0).getName()).isEqualTo("The Bund");
        assertThat(plan.getPlaces().get(0).getStayMinutes()).isEqualTo(90);
    }

    @Test
    void stripsMarkdownFenceBeforeParsing() {
        LlmProperties properties = configuredProperties();
        TravelGuideExtractionService service = new TravelGuideExtractionService(
                (systemPrompt, messages) -> """
                        ```json
                        {"title":"Fence","summary":"","places":[{"name":"Yu Garden","dayIndex":1,"orderIndex":0,"stayMinutes":120,"reason":"","confidence":0.8}],"warnings":[]}
                        ```
                        """,
                properties,
                objectMapper);

        ExtractedGuidePlan plan = service.extract("Yu Garden");

        assertThat(plan.getPlaces()).extracting(ExtractedGuidePlan.Place::getName).containsExactly("Yu Garden");
    }

    @Test
    void fallsBackWhenLlmIsNotConfigured() {
        LlmProperties properties = new LlmProperties();
        TravelGuideExtractionService service = new TravelGuideExtractionService(
                new FailingLlmClient(),
                properties,
                objectMapper);

        ExtractedGuidePlan plan = service.extract("First visit West Lake, then Lingyin Temple.");

        assertThat(plan.getTitle()).isEqualTo("Imported Travel Guide");
        assertThat(plan.getPlaces()).extracting(ExtractedGuidePlan.Place::getName)
                .containsExactly("West Lake", "Lingyin Temple");
        assertThat(plan.getWarnings()).contains("LLM is not configured; used local text extraction");
    }

    @Test
    void fallsBackWhenLlmReturnsInvalidJson() {
        LlmProperties properties = configuredProperties();
        TravelGuideExtractionService service = new TravelGuideExtractionService(
                (systemPrompt, messages) -> "not-json",
                properties,
                objectMapper);

        ExtractedGuidePlan plan = service.extract("Go to Museum and Riverside Park.");

        assertThat(plan.getPlaces()).extracting(ExtractedGuidePlan.Place::getName)
                .containsExactly("Museum", "Riverside Park");
        assertThat(plan.getWarnings()).anySatisfy(warning ->
                assertThat(warning).contains("LLM extraction failed"));
    }

    @Test
    void localFallbackExtractsChineseDemoLines() {
        LlmProperties properties = new LlmProperties();
        TravelGuideExtractionService service = new TravelGuideExtractionService(
                new FailingLlmClient(),
                properties,
                objectMapper);

        ExtractedGuidePlan plan = service.extract("""
                观复博物馆
                北京理工大学
                八达岭野生动物园
                """);

        assertThat(plan.getPlaces()).extracting(ExtractedGuidePlan.Place::getName)
                .containsExactly("观复博物馆", "北京理工大学", "八达岭野生动物园");
    }

    private LlmProperties configuredProperties() {
        LlmProperties properties = new LlmProperties();
        properties.setApiKey("test-key");
        properties.setModel("deepseek-chat");
        properties.setBaseUrl("https://api.deepseek.com/v1");
        return properties;
    }

    private static class FailingLlmClient implements LlmChatClient {
        @Override
        public String chat(String systemPrompt, List<AgentChatMessage> messages) {
            throw new AssertionError("LLM should not be called when not configured");
        }
    }
}
