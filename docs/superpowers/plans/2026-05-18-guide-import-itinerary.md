# Guide Import Itinerary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a guide import feature that converts pasted travel guide text or an existing travel diary into a new collaborative itinerary and opens the existing one-click planner.

**Architecture:** Add a backend import pipeline that loads text, extracts ordered places with the existing OpenAI-compatible LLM client, falls back to local destination-name matching, creates a new itinerary, and adds matched destination candidates. Add a Vue dialog on the itinerary page that previews matched and unmatched places before creating the itinerary.

**Tech Stack:** Spring Boot, MyBatis, JUnit 5, AssertJ, Jackson, Vue 3, Element Plus, Axios, Node test runner.

---

## Credential Handling

Use the DeepSeek key supplied by the project owner only as a local environment variable. Do not write the key into source files, docs, `.env.example`, tests, commits, frontend code, browser local storage, or build artifacts.

For local backend runs, set:

```powershell
$env:PROJECT_OWNER_DEEPSEEK_API_KEY = Read-Host 'DeepSeek API key for this shell only'
$env:LLM_BASE_URL = 'https://api.deepseek.com/v1'
$env:LLM_MODEL = 'deepseek-chat'
$env:LLM_API_KEY = $env:PROJECT_OWNER_DEEPSEEK_API_KEY
```

Keep `SILICONFLOW_*` variables working for compatibility with the existing setup.

## File Structure

Backend files:

- Modify: `data-structure-design-backend/src/main/resources/application.yml`
  - Add generic `LLM_*` environment variable support while keeping `SILICONFLOW_*` fallback.
- Modify: `data-structure-design-backend/.env.example`
  - Document `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`, with no secret values.
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryImportRequest.java`
  - Request DTO for preview/create.
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryImportResponse.java`
  - Shared import preview response DTO with matched/unmatched spot records.
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryImportCreateResponse.java`
  - Create response DTO containing itinerary, import result, and planner spots.
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ExtractedGuidePlan.java`
  - Internal LLM extraction DTO.
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/DestinationService.java`
  - Add `findAll()` for local matching.
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/TravelGuideExtractionService.java`
  - LLM extraction and local fallback extraction.
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/DestinationMatchService.java`
  - Maps extracted place names to destinations.
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/ItineraryImportService.java`
  - Orchestrates preview/create.
- Create: `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryImportController.java`
  - REST endpoints.
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/TravelGuideExtractionServiceTest.java`
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/DestinationMatchServiceTest.java`
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/ItineraryImportServiceTest.java`
- Create: `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryImportControllerTest.java`

Frontend files:

- Modify: `data-structure-design-frontend/src/api/travel.js`
  - Add import preview/create API methods.
- Create: `data-structure-design-frontend/src/utils/guideImport.js`
  - Request builders and result helpers.
- Create: `data-structure-design-frontend/src/utils/guideImport.test.js`
  - Unit tests for builders/helpers.
- Create: `data-structure-design-frontend/src/components/itinerary/GuideImportDialog.vue`
  - Import dialog UI.
- Modify: `data-structure-design-frontend/src/views/ItineraryView.vue`
  - Add entry point and success integration.

## Task 1: Generic LLM Environment Configuration

**Files:**
- Modify: `data-structure-design-backend/src/main/resources/application.yml`
- Modify: `data-structure-design-backend/.env.example`

- [ ] **Step 1: Inspect current LLM config**

Run:

```powershell
Get-Content -LiteralPath 'data-structure-design-backend\src\main\resources\application.yml'
Get-Content -LiteralPath 'data-structure-design-backend\.env.example'
```

Expected: `application.yml` currently uses `SILICONFLOW_BASE_URL`, `SILICONFLOW_API_KEY`, and `SILICONFLOW_MODEL`.

- [ ] **Step 2: Update `application.yml` to prefer generic LLM variables**

Change the `llm` block to:

```yaml
llm:
  base-url: ${LLM_BASE_URL:${SILICONFLOW_BASE_URL:https://api.siliconflow.cn/v1}}
  api-key: ${LLM_API_KEY:${SILICONFLOW_API_KEY:}}
  model: ${LLM_MODEL:${SILICONFLOW_MODEL:}}
  temperature: ${LLM_TEMPERATURE:0.4}
  timeout-seconds: ${LLM_TIMEOUT_SECONDS:90}
```

- [ ] **Step 3: Update `.env.example` without committing secrets**

Add these lines or replace the existing LLM example lines:

```dotenv
# OpenAI-compatible LLM provider. For DeepSeek local development:
# LLM_BASE_URL=https://api.deepseek.com/v1
# LLM_MODEL=deepseek-chat
# LLM_API_KEY must be set locally and must not be committed.
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_API_KEY=
LLM_MODEL=deepseek-chat
LLM_TEMPERATURE=0.4
LLM_TIMEOUT_SECONDS=90

# Backward compatibility with the previous SiliconFlow naming.
SILICONFLOW_BASE_URL=
SILICONFLOW_API_KEY=
SILICONFLOW_MODEL=
```

- [ ] **Step 4: Build backend config**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test -DskipTests
```

Expected: Maven build succeeds without test execution.

- [ ] **Step 5: Commit**

```bash
git add data-structure-design-backend/src/main/resources/application.yml data-structure-design-backend/.env.example
git commit -m "chore: support generic llm environment variables"
```

## Task 2: Backend Import DTOs

**Files:**
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryImportRequest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryImportResponse.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryImportCreateResponse.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/dto/ExtractedGuidePlan.java`

- [ ] **Step 1: Create request DTO**

Create `ItineraryImportRequest.java`:

```java
package com.travel.system.dto;

import lombok.Data;

@Data
public class ItineraryImportRequest {
    private String sourceType;
    private String text;
    private Long diaryId;
    private String owner;
}
```

- [ ] **Step 2: Create shared response DTO**

Create `ItineraryImportResponse.java`:

```java
package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryImportResponse {
    private String title;
    private String summary;
    private String sourceType;
    private List<MatchedSpot> spots = new ArrayList<>();
    private List<UnmatchedSpot> unmatchedSpots = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedSpot {
        private String rawName;
        private Long matchedDestinationId;
        private String matchedName;
        private Double latitude;
        private Double longitude;
        private Integer dayIndex;
        private Integer orderIndex;
        private Integer stayMinutes;
        private Double confidence;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnmatchedSpot {
        private String rawName;
        private Integer dayIndex;
        private Integer orderIndex;
        private String reason;
    }
}
```

- [ ] **Step 3: Create create-response DTO**

Create `ItineraryImportCreateResponse.java`:

```java
package com.travel.system.dto;

import com.travel.system.model.Itinerary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryImportCreateResponse {
    private Itinerary itinerary;
    private ItineraryImportResponse importResult;
    private List<ItineraryMapSpot> plannerSpots = new ArrayList<>();
}
```

- [ ] **Step 4: Create internal extraction DTO**

Create `ExtractedGuidePlan.java`:

```java
package com.travel.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedGuidePlan {
    private String title;
    private String summary;
    private List<Place> places = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Place {
        private String name;
        private Integer dayIndex;
        private Integer orderIndex;
        private Integer stayMinutes;
        private String reason;
        private Double confidence;
    }
}
```

- [ ] **Step 5: Compile DTOs**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test -DskipTests
```

Expected: compilation succeeds.

- [ ] **Step 6: Commit**

```bash
git add data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryImportRequest.java data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryImportResponse.java data-structure-design-backend/src/main/java/com/travel/system/dto/ItineraryImportCreateResponse.java data-structure-design-backend/src/main/java/com/travel/system/dto/ExtractedGuidePlan.java
git commit -m "feat: add itinerary import dto contracts"
```

## Task 3: Travel Guide Extraction Service

**Files:**
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/TravelGuideExtractionServiceTest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/TravelGuideExtractionService.java`

- [ ] **Step 1: Write failing extraction tests**

Create `TravelGuideExtractionServiceTest.java`:

```java
package com.travel.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.system.config.LlmProperties;
import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.dto.AgentChatMessage;
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
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test -Dtest=TravelGuideExtractionServiceTest
```

Expected: FAIL because `TravelGuideExtractionService` does not exist.

- [ ] **Step 3: Implement extraction service**

Create `TravelGuideExtractionService.java`:

```java
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
        return hasText(properties.getApiKey()) && hasText(properties.getModel());
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

    private boolean containsName(List<ExtractedGuidePlan.Place> places, String name) {
        return places.stream().anyMatch(place -> place.getName().equalsIgnoreCase(name));
    }

    private boolean isStopPhrase(String value) {
        String normalized = value.toLowerCase();
        return normalized.equals("first") || normalized.equals("then") || normalized.equals("visit") || normalized.equals("go");
    }

    private String cleanJson(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
```

- [ ] **Step 4: Run extraction tests**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test -Dtest=TravelGuideExtractionServiceTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add data-structure-design-backend/src/main/java/com/travel/system/service/TravelGuideExtractionService.java data-structure-design-backend/src/test/java/com/travel/system/service/TravelGuideExtractionServiceTest.java
git commit -m "feat: extract guide places from text"
```

## Task 4: Destination Matching

**Files:**
- Modify: `data-structure-design-backend/src/main/java/com/travel/system/service/DestinationService.java`
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/DestinationMatchServiceTest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/DestinationMatchService.java`

- [ ] **Step 1: Add `findAll` to `DestinationService`**

Add this public method after `findById`:

```java
public List<Destination> findAll() {
    return destinationMapper.findAll();
}
```

- [ ] **Step 2: Write failing destination match tests**

Create `DestinationMatchServiceTest.java`:

```java
package com.travel.system.service;

import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.model.Destination;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DestinationMatchServiceTest {
    private final FakeDestinationService destinationService = new FakeDestinationService();
    private final DestinationMatchService service = new DestinationMatchService(destinationService);

    @Test
    void matchesExactDestinationName() {
        destinationService.destinations.add(destination(1L, "The Bund"));

        DestinationMatchService.MatchResult result = service.match(place("The Bund", 0));

        assertThat(result.matched()).isTrue();
        assertThat(result.destination().getId()).isEqualTo(1L);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.9);
    }

    @Test
    void matchesNormalizedDestinationName() {
        destinationService.destinations.add(destination(2L, "Yu Garden"));

        DestinationMatchService.MatchResult result = service.match(place("yu-garden", 0));

        assertThat(result.matched()).isTrue();
        assertThat(result.destination().getName()).isEqualTo("Yu Garden");
    }

    @Test
    void rejectsAmbiguousKeywordMatch() {
        destinationService.destinations.add(destination(3L, "People Square"));
        destinationService.destinations.add(destination(4L, "People Park"));

        DestinationMatchService.MatchResult result = service.match(place("People", 0));

        assertThat(result.matched()).isFalse();
        assertThat(result.reason()).contains("Ambiguous");
    }

    @Test
    void rejectsDestinationWithoutCoordinates() {
        destinationService.destinations.add(new Destination(5L, "No Coordinates", "scenic", "park", 1.0, 1.0, "", null, null));

        DestinationMatchService.MatchResult result = service.match(place("No Coordinates", 0));

        assertThat(result.matched()).isFalse();
        assertThat(result.reason()).contains("coordinates");
    }

    private ExtractedGuidePlan.Place place(String name, int orderIndex) {
        return new ExtractedGuidePlan.Place(name, 1, orderIndex, 120, "", 0.8);
    }

    private Destination destination(Long id, String name) {
        return new Destination(id, name, "scenic", "spot", 1.0, 4.5, "", 31.0 + id, 121.0 + id);
    }

    private static class FakeDestinationService extends DestinationService {
        private final List<Destination> destinations = new ArrayList<>();

        FakeDestinationService() {
            super(null, null);
        }

        @Override
        public List<Destination> findAll() {
            return destinations;
        }

        @Override
        public List<Destination> searchForRoute(String keyword, int limit) {
            String normalized = keyword == null ? "" : keyword.toLowerCase();
            return destinations.stream()
                    .filter(destination -> destination.getName().toLowerCase().contains(normalized)
                            || normalized.contains(destination.getName().toLowerCase()))
                    .limit(limit)
                    .toList();
        }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test -Dtest=DestinationMatchServiceTest
```

Expected: FAIL because `DestinationMatchService` does not exist.

- [ ] **Step 4: Implement destination match service**

Create `DestinationMatchService.java`:

```java
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
        List<Destination> candidates = destinationService.searchForRoute(rawName, 5);
        List<DestinationScore> scored = candidates.stream()
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
        for (String token : raw.split(" ")) {
            if (!token.isBlank() && candidate.contains(token)) {
                common++;
            }
        }
        int denominator = Math.max(1, raw.split(" ").length);
        return common / (double) denominator;
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
```

- [ ] **Step 5: Run destination matching tests**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test -Dtest=DestinationMatchServiceTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add data-structure-design-backend/src/main/java/com/travel/system/service/DestinationService.java data-structure-design-backend/src/main/java/com/travel/system/service/DestinationMatchService.java data-structure-design-backend/src/test/java/com/travel/system/service/DestinationMatchServiceTest.java
git commit -m "feat: match extracted guide places to destinations"
```

## Task 5: Itinerary Import Service

**Files:**
- Create: `data-structure-design-backend/src/test/java/com/travel/system/service/ItineraryImportServiceTest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/service/ItineraryImportService.java`

- [ ] **Step 1: Write service tests**

Create `ItineraryImportServiceTest.java` with focused fake services. The test must cover text preview, diary preview, create success, and create rejection when no matched spots exist:

```java
package com.travel.system.service;

import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.dto.ItineraryImportCreateResponse;
import com.travel.system.dto.ItineraryImportRequest;
import com.travel.system.dto.ItineraryImportResponse;
import com.travel.system.dto.ItineraryMapSpot;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.model.Destination;
import com.travel.system.model.Diary;
import com.travel.system.model.Itinerary;
import com.travel.system.model.ItinerarySpotCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItineraryImportServiceTest {
    private final FakeExtractionService extractionService = new FakeExtractionService();
    private final FakeDestinationMatchService matchService = new FakeDestinationMatchService();
    private final FakeDiaryService diaryService = new FakeDiaryService();
    private final FakeItineraryService itineraryService = new FakeItineraryService();
    private final FakeCandidateService candidateService = new FakeCandidateService();
    private final ItineraryImportService service = new ItineraryImportService(
            extractionService,
            matchService,
            diaryService,
            itineraryService,
            candidateService);

    @Test
    void previewsTextSourceWithMatchedAndUnmatchedSpots() {
        extractionService.plan.getPlaces().add(place("The Bund", 0));
        extractionService.plan.getPlaces().add(place("Unknown Cafe", 1));
        matchService.destinations.add(destination(10L, "The Bund"));

        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");
        request.setText("The Bund then Unknown Cafe");

        ItineraryImportResponse response = service.preview(request);

        assertThat(response.getTitle()).isEqualTo("Imported Route");
        assertThat(response.getSourceType()).isEqualTo("TEXT");
        assertThat(response.getSpots()).hasSize(1);
        assertThat(response.getSpots().get(0).getMatchedDestinationId()).isEqualTo(10L);
        assertThat(response.getUnmatchedSpots()).hasSize(1);
    }

    @Test
    void previewsDiarySourceByLoadingDiaryContent() {
        Diary diary = new Diary();
        diary.setId(7L);
        diary.setTitle("Diary title");
        diary.setContent("The Bund");
        diaryService.diary = diary;
        extractionService.plan.getPlaces().add(place("The Bund", 0));
        matchService.destinations.add(destination(10L, "The Bund"));

        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("DIARY");
        request.setDiaryId(7L);

        ItineraryImportResponse response = service.preview(request);

        assertThat(extractionService.lastText).isEqualTo("Diary title\n\nThe Bund");
        assertThat(response.getSpots()).hasSize(1);
    }

    @Test
    void createsItineraryAndAddsMatchedCandidates() {
        extractionService.plan.getPlaces().add(place("The Bund", 0));
        matchService.destinations.add(destination(10L, "The Bund"));

        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");
        request.setText("The Bund");
        request.setOwner("Zhou");

        ItineraryImportCreateResponse response = service.create(request);

        assertThat(response.getItinerary().getName()).isEqualTo("Imported Route");
        assertThat(response.getItinerary().getOwner()).isEqualTo("Zhou");
        assertThat(candidateService.addedDestinationIds).containsExactly(10L);
        assertThat(response.getPlannerSpots()).hasSize(1);
    }

    @Test
    void rejectsCreateWhenNoDestinationsMatched() {
        extractionService.plan.getPlaces().add(place("Unknown Cafe", 0));

        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");
        request.setText("Unknown Cafe");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No matched destinations");
    }

    private ExtractedGuidePlan.Place place(String name, int orderIndex) {
        return new ExtractedGuidePlan.Place(name, 1, orderIndex, 120, "", 0.8);
    }

    private Destination destination(Long id, String name) {
        return new Destination(id, name, "scenic", "spot", 1.0, 4.5, "", 31.0 + id, 121.0 + id);
    }

    private static class FakeExtractionService extends TravelGuideExtractionService {
        private final ExtractedGuidePlan plan = new ExtractedGuidePlan("Imported Route", "Summary", new ArrayList<>(), new ArrayList<>());
        private String lastText;

        FakeExtractionService() {
            super(null, null, null);
        }

        @Override
        public ExtractedGuidePlan extract(String text) {
            lastText = text;
            return plan;
        }
    }

    private static class FakeDestinationMatchService extends DestinationMatchService {
        private final List<Destination> destinations = new ArrayList<>();

        FakeDestinationMatchService() {
            super(null);
        }

        @Override
        public MatchResult match(ExtractedGuidePlan.Place place) {
            return destinations.stream()
                    .filter(destination -> destination.getName().equals(place.getName()))
                    .findFirst()
                    .map(destination -> MatchResult.matched(place, destination, 0.95))
                    .orElseGet(() -> MatchResult.unmatched(place, "No matching destination found"));
        }
    }

    private static class FakeDiaryService extends DiaryService {
        private Diary diary;

        FakeDiaryService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public Diary detail(Long id) {
            return diary;
        }
    }

    private static class FakeItineraryService extends ItineraryService {
        private long nextId = 100L;

        FakeItineraryService() {
            super(null, null);
        }

        @Override
        public Itinerary create(Itinerary itinerary) {
            itinerary.setId(nextId++);
            return itinerary;
        }
    }

    private static class FakeCandidateService extends ItinerarySpotCandidateService {
        private final List<Long> addedDestinationIds = new ArrayList<>();

        FakeCandidateService() {
            super(null, null, null);
        }

        @Override
        public ItinerarySpotCandidate addCandidate(Long itineraryId, ItinerarySpotCandidateRequest request) {
            addedDestinationIds.add(request.getDestinationId());
            ItinerarySpotCandidate candidate = new ItinerarySpotCandidate();
            candidate.setItineraryId(itineraryId);
            candidate.setDestinationId(request.getDestinationId());
            return candidate;
        }

        @Override
        public List<ItineraryMapSpot> listMapSpots(Long itineraryId) {
            return addedDestinationIds.stream().map(id -> {
                ItineraryMapSpot spot = new ItineraryMapSpot();
                spot.setDestinationId(id);
                spot.setSpotId(id);
                spot.setSpotName("Imported " + id);
                spot.setLatitude(31.0 + id);
                spot.setLongitude(121.0 + id);
                return spot;
            }).toList();
        }
    }
}
```

- [ ] **Step 2: Run service tests to verify failure**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test -Dtest=ItineraryImportServiceTest
```

Expected: FAIL because `ItineraryImportService` does not exist.

- [ ] **Step 3: Implement import service**

Create `ItineraryImportService.java`:

```java
package com.travel.system.service;

import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.dto.ItineraryImportCreateResponse;
import com.travel.system.dto.ItineraryImportRequest;
import com.travel.system.dto.ItineraryImportResponse;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.model.Diary;
import com.travel.system.model.Itinerary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class ItineraryImportService {
    private final TravelGuideExtractionService extractionService;
    private final DestinationMatchService matchService;
    private final DiaryService diaryService;
    private final ItineraryService itineraryService;
    private final ItinerarySpotCandidateService candidateService;

    public ItineraryImportService(TravelGuideExtractionService extractionService,
                                  DestinationMatchService matchService,
                                  DiaryService diaryService,
                                  ItineraryService itineraryService,
                                  ItinerarySpotCandidateService candidateService) {
        this.extractionService = extractionService;
        this.matchService = matchService;
        this.diaryService = diaryService;
        this.itineraryService = itineraryService;
        this.candidateService = candidateService;
    }

    public ItineraryImportResponse preview(ItineraryImportRequest request) {
        SourceText source = sourceText(request);
        ExtractedGuidePlan plan = extractionService.extract(source.text());
        ItineraryImportResponse response = new ItineraryImportResponse();
        response.setTitle(hasText(plan.getTitle()) ? plan.getTitle() : fallbackTitle());
        response.setSummary(plan.getSummary() == null ? "" : plan.getSummary());
        response.setSourceType(source.sourceType());
        response.setWarnings(plan.getWarnings() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(plan.getWarnings()));
        List<ExtractedGuidePlan.Place> places = plan.getPlaces() == null ? List.of() : plan.getPlaces();
        for (ExtractedGuidePlan.Place place : places) {
            DestinationMatchService.MatchResult match = matchService.match(place);
            if (match.matched()) {
                response.getSpots().add(new ItineraryImportResponse.MatchedSpot(
                        place.getName(),
                        match.destination().getId(),
                        match.destination().getName(),
                        match.destination().getLatitude(),
                        match.destination().getLongitude(),
                        safeDay(place.getDayIndex()),
                        safeOrder(place.getOrderIndex(), response.getSpots().size() + response.getUnmatchedSpots().size()),
                        safeStay(place.getStayMinutes()),
                        Math.min(1.0, Math.max(0.0, (place.getConfidence() == null ? 0.75 : place.getConfidence()) * match.confidence())),
                        place.getReason() == null ? "" : place.getReason()
                ));
            } else {
                response.getUnmatchedSpots().add(new ItineraryImportResponse.UnmatchedSpot(
                        place.getName(),
                        safeDay(place.getDayIndex()),
                        safeOrder(place.getOrderIndex(), response.getSpots().size() + response.getUnmatchedSpots().size()),
                        match.reason()
                ));
            }
        }
        return response;
    }

    public ItineraryImportCreateResponse create(ItineraryImportRequest request) {
        ItineraryImportResponse importResult = preview(request);
        if (importResult.getSpots().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No matched destinations found");
        }
        Itinerary itinerary = new Itinerary();
        itinerary.setName(hasText(importResult.getTitle()) ? importResult.getTitle() : fallbackTitle());
        itinerary.setOwner(hasText(request == null ? null : request.getOwner()) ? request.getOwner().trim() : "Imported");
        itinerary.setCollaborators("");
        itinerary.setStrategy("SHORTEST_TIME");
        itinerary.setTransportMode("walk");
        itinerary.setNotes(importResult.getSummary());
        Itinerary created = itineraryService.create(itinerary);
        for (ItineraryImportResponse.MatchedSpot spot : importResult.getSpots()) {
            ItinerarySpotCandidateRequest candidateRequest = new ItinerarySpotCandidateRequest();
            candidateRequest.setDestinationId(spot.getMatchedDestinationId());
            candidateService.addCandidate(created.getId(), candidateRequest);
        }
        return new ItineraryImportCreateResponse(created, importResult, candidateService.listMapSpots(created.getId()));
    }

    private SourceText sourceText(ItineraryImportRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import request is required");
        }
        String sourceType = normalizeSourceType(request.getSourceType());
        if ("TEXT".equals(sourceType)) {
            if (!hasText(request.getText())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guide text is required");
            }
            return new SourceText(sourceType, request.getText().trim());
        }
        if ("DIARY".equals(sourceType)) {
            if (request.getDiaryId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "diaryId is required");
            }
            Diary diary = diaryService.detail(request.getDiaryId());
            if (diary == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary not found: " + request.getDiaryId());
            }
            return new SourceText(sourceType, joinDiaryText(diary));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceType must be TEXT or DIARY");
    }

    private String joinDiaryText(Diary diary) {
        String title = diary.getTitle() == null ? "" : diary.getTitle().trim();
        String content = diary.getContent() == null ? "" : diary.getContent().trim();
        String combined = (title + "\n\n" + content).trim();
        if (combined.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diary content is empty");
        }
        return combined;
    }

    private String normalizeSourceType(String sourceType) {
        return sourceType == null || sourceType.isBlank() ? "TEXT" : sourceType.trim().toUpperCase();
    }

    private Integer safeDay(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private Integer safeOrder(Integer value, int fallback) {
        return value == null || value < 0 ? fallback : value;
    }

    private Integer safeStay(Integer value) {
        return value == null || value < 0 ? 120 : value;
    }

    private String fallbackTitle() {
        return "Imported Itinerary " + LocalDate.now();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record SourceText(String sourceType, String text) {
    }
}
```

- [ ] **Step 4: Run import service tests**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test -Dtest=ItineraryImportServiceTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add data-structure-design-backend/src/main/java/com/travel/system/service/ItineraryImportService.java data-structure-design-backend/src/test/java/com/travel/system/service/ItineraryImportServiceTest.java
git commit -m "feat: create itineraries from imported guides"
```

## Task 6: Import REST Controller

**Files:**
- Create: `data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryImportControllerTest.java`
- Create: `data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryImportController.java`

- [ ] **Step 1: Write controller tests**

Create `ItineraryImportControllerTest.java`:

```java
package com.travel.system.controller;

import com.travel.system.dto.ItineraryImportCreateResponse;
import com.travel.system.dto.ItineraryImportRequest;
import com.travel.system.dto.ItineraryImportResponse;
import com.travel.system.model.Itinerary;
import com.travel.system.service.ItineraryImportService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItineraryImportControllerTest {
    private final FakeImportService service = new FakeImportService();
    private final ItineraryImportController controller = new ItineraryImportController(service);

    @Test
    void delegatesPreview() {
        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");

        ItineraryImportResponse response = controller.preview(request);

        assertThat(service.previewRequest).isSameAs(request);
        assertThat(response.getTitle()).isEqualTo("Preview");
    }

    @Test
    void delegatesCreate() {
        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");

        ItineraryImportCreateResponse response = controller.create(request);

        assertThat(service.createRequest).isSameAs(request);
        assertThat(response.getItinerary().getId()).isEqualTo(77L);
    }

    private static class FakeImportService extends ItineraryImportService {
        private ItineraryImportRequest previewRequest;
        private ItineraryImportRequest createRequest;

        FakeImportService() {
            super(null, null, null, null, null);
        }

        @Override
        public ItineraryImportResponse preview(ItineraryImportRequest request) {
            previewRequest = request;
            ItineraryImportResponse response = new ItineraryImportResponse();
            response.setTitle("Preview");
            return response;
        }

        @Override
        public ItineraryImportCreateResponse create(ItineraryImportRequest request) {
            createRequest = request;
            Itinerary itinerary = new Itinerary();
            itinerary.setId(77L);
            return new ItineraryImportCreateResponse(itinerary, new ItineraryImportResponse(), java.util.List.of());
        }
    }
}
```

- [ ] **Step 2: Run controller tests to verify failure**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test -Dtest=ItineraryImportControllerTest
```

Expected: FAIL because `ItineraryImportController` does not exist.

- [ ] **Step 3: Implement controller**

Create `ItineraryImportController.java`:

```java
package com.travel.system.controller;

import com.travel.system.dto.ItineraryImportCreateResponse;
import com.travel.system.dto.ItineraryImportRequest;
import com.travel.system.dto.ItineraryImportResponse;
import com.travel.system.service.ItineraryImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/itinerary-import")
public class ItineraryImportController {
    private final ItineraryImportService importService;

    public ItineraryImportController(ItineraryImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/preview")
    public ItineraryImportResponse preview(@RequestBody ItineraryImportRequest request) {
        return importService.preview(request);
    }

    @PostMapping("/create")
    public ItineraryImportCreateResponse create(@RequestBody ItineraryImportRequest request) {
        return importService.create(request);
    }
}
```

- [ ] **Step 4: Run backend import tests**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test '-Dtest=TravelGuideExtractionServiceTest,DestinationMatchServiceTest,ItineraryImportServiceTest,ItineraryImportControllerTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add data-structure-design-backend/src/main/java/com/travel/system/controller/ItineraryImportController.java data-structure-design-backend/src/test/java/com/travel/system/controller/ItineraryImportControllerTest.java
git commit -m "feat: expose itinerary import endpoints"
```

## Task 7: Frontend API and Utility Helpers

**Files:**
- Modify: `data-structure-design-frontend/src/api/travel.js`
- Create: `data-structure-design-frontend/src/utils/guideImport.js`
- Create: `data-structure-design-frontend/src/utils/guideImport.test.js`

- [ ] **Step 1: Add API methods**

Add to `travel.js` near the itinerary APIs:

```js
export const previewItineraryImport = (payload) => http.post('/itinerary-import/preview', payload, { timeout: 90000 })
export const createItineraryFromImport = (payload) => http.post('/itinerary-import/create', payload, { timeout: 90000 })
```

- [ ] **Step 2: Write helper tests**

Create `guideImport.test.js`:

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildDiaryImportPayload,
  buildTextImportPayload,
  importCanCreate,
  importSpotLabel,
} from './guideImport.js'

test('buildTextImportPayload trims text and owner', () => {
  assert.deepEqual(buildTextImportPayload('  go to The Bund  ', '  Zhou  '), {
    sourceType: 'TEXT',
    text: 'go to The Bund',
    diaryId: null,
    owner: 'Zhou',
  })
})

test('buildDiaryImportPayload uses diary id', () => {
  assert.deepEqual(buildDiaryImportPayload(9, 'Lin'), {
    sourceType: 'DIARY',
    text: null,
    diaryId: 9,
    owner: 'Lin',
  })
})

test('importCanCreate requires at least one matched spot', () => {
  assert.equal(importCanCreate({ spots: [] }), false)
  assert.equal(importCanCreate({ spots: [{ matchedDestinationId: 1 }] }), true)
})

test('importSpotLabel includes order and stay time', () => {
  assert.equal(importSpotLabel({ orderIndex: 1, matchedName: 'The Bund', stayMinutes: 90 }), '2. The Bund · 90 min')
})
```

- [ ] **Step 3: Run helper tests to verify failure**

Run:

```powershell
npm.cmd --prefix data-structure-design-frontend test -- src/utils/guideImport.test.js
```

Expected: FAIL because `guideImport.js` does not exist.

- [ ] **Step 4: Implement helpers**

Create `guideImport.js`:

```js
export const buildTextImportPayload = (text, owner = '') => ({
  sourceType: 'TEXT',
  text: String(text || '').trim(),
  diaryId: null,
  owner: String(owner || '').trim(),
})

export const buildDiaryImportPayload = (diaryId, owner = '') => ({
  sourceType: 'DIARY',
  text: null,
  diaryId,
  owner: String(owner || '').trim(),
})

export const importCanCreate = (preview) =>
  Array.isArray(preview?.spots) && preview.spots.some((spot) => spot?.matchedDestinationId)

export const importSpotLabel = (spot) => {
  const order = Number.isFinite(Number(spot?.orderIndex)) ? Number(spot.orderIndex) + 1 : 1
  const name = spot?.matchedName || spot?.rawName || 'Imported spot'
  const stay = Number.isFinite(Number(spot?.stayMinutes)) ? Number(spot.stayMinutes) : 120
  return `${order}. ${name} · ${stay} min`
}
```

- [ ] **Step 5: Run frontend helper tests**

Run:

```powershell
npm.cmd --prefix data-structure-design-frontend test -- src/utils/guideImport.test.js
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add data-structure-design-frontend/src/api/travel.js data-structure-design-frontend/src/utils/guideImport.js data-structure-design-frontend/src/utils/guideImport.test.js
git commit -m "feat: add guide import frontend helpers"
```

## Task 8: Guide Import Dialog UI

**Files:**
- Create: `data-structure-design-frontend/src/components/itinerary/GuideImportDialog.vue`

- [ ] **Step 1: Create dialog component**

Create `GuideImportDialog.vue`:

```vue
<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentAdd, Notebook, Search } from '@icon-park/vue-next'
import {
  createItineraryFromImport,
  listDiaries,
  previewItineraryImport,
  searchDiaryFullText,
} from '../../api/travel'
import {
  buildDiaryImportPayload,
  buildTextImportPayload,
  importCanCreate,
  importSpotLabel,
} from '../../utils/guideImport'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  owner: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue', 'created'])

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

const sourceType = ref('TEXT')
const text = ref('')
const diaryKeyword = ref('')
const diaries = ref([])
const selectedDiaryId = ref(null)
const loadingDiaries = ref(false)
const previewing = ref(false)
const creating = ref(false)
const preview = ref(null)
const error = ref('')

const selectedDiary = computed(() => diaries.value.find((diary) => diary.id === selectedDiaryId.value) || null)
const canPreview = computed(() => {
  if (sourceType.value === 'TEXT') return text.value.trim().length > 0
  return Boolean(selectedDiaryId.value)
})
const canCreate = computed(() => importCanCreate(preview.value) && !creating.value)

watch(() => props.modelValue, (open) => {
  if (open && sourceType.value === 'DIARY' && !diaries.value.length) {
    void loadDiaries()
  }
})

watch(sourceType, (next) => {
  preview.value = null
  error.value = ''
  if (next === 'DIARY' && !diaries.value.length) {
    void loadDiaries()
  }
})

const close = () => {
  visible.value = false
}

const loadDiaries = async () => {
  loadingDiaries.value = true
  try {
    const request = diaryKeyword.value.trim()
      ? searchDiaryFullText(diaryKeyword.value.trim())
      : listDiaries(20)
    const { data } = await request
    diaries.value = Array.isArray(data) ? data : []
  } catch (caught) {
    ElMessage.error(caught?.message || 'Failed to load diaries')
  } finally {
    loadingDiaries.value = false
  }
}

const payload = () => sourceType.value === 'TEXT'
  ? buildTextImportPayload(text.value, props.owner)
  : buildDiaryImportPayload(selectedDiaryId.value, props.owner)

const previewImport = async () => {
  if (!canPreview.value) {
    ElMessage.warning(sourceType.value === 'TEXT' ? 'Please paste guide text first' : 'Please choose a diary first')
    return
  }
  previewing.value = true
  error.value = ''
  try {
    const { data } = await previewItineraryImport(payload())
    preview.value = data
  } catch (caught) {
    error.value = caught?.response?.data?.message || caught?.message || 'Guide recognition failed'
    ElMessage.error(error.value)
  } finally {
    previewing.value = false
  }
}

const createImport = async () => {
  if (!canCreate.value) {
    ElMessage.warning('No matched spots are ready to create')
    return
  }
  creating.value = true
  try {
    const { data } = await createItineraryFromImport(payload())
    emit('created', data)
    close()
  } catch (caught) {
    error.value = caught?.response?.data?.message || caught?.message || 'Failed to create itinerary'
    ElMessage.error(error.value)
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="Import Guide" width="720px" class="guide-import-dialog">
    <el-tabs v-model="sourceType">
      <el-tab-pane label="Paste text" name="TEXT">
        <el-input
          v-model="text"
          type="textarea"
          :rows="8"
          maxlength="8000"
          show-word-limit
          placeholder="Paste travel guide text here..."
        />
      </el-tab-pane>
      <el-tab-pane label="Choose diary" name="DIARY">
        <div class="diary-picker">
          <el-input v-model="diaryKeyword" clearable placeholder="Search diaries" @keyup.enter="loadDiaries">
            <template #prefix>
              <Search theme="outline" size="16" fill="currentColor" />
            </template>
          </el-input>
          <el-button :loading="loadingDiaries" @click="loadDiaries">Search</el-button>
        </div>
        <div class="diary-list" v-loading="loadingDiaries">
          <button
            v-for="diary in diaries"
            :key="diary.id"
            type="button"
            :class="['diary-choice', { active: diary.id === selectedDiaryId }]"
            @click="selectedDiaryId = diary.id"
          >
            <Notebook theme="outline" size="17" fill="currentColor" />
            <span>
              <strong>{{ diary.title || 'Untitled diary' }}</strong>
              <small>{{ diary.content || 'No content' }}</small>
            </span>
          </button>
          <el-empty v-if="!diaries.length" description="No diaries found" />
        </div>
        <el-alert v-if="selectedDiary" type="info" :title="selectedDiary.title" :closable="false" show-icon />
      </el-tab-pane>
    </el-tabs>

    <el-alert v-if="error" type="warning" :title="error" :closable="false" show-icon />

    <section v-if="preview" class="import-preview">
      <header>
        <div>
          <strong>{{ preview.title || 'Imported itinerary' }}</strong>
          <span>{{ preview.summary || 'No summary' }}</span>
        </div>
        <el-tag type="success">{{ preview.spots?.length || 0 }} matched</el-tag>
      </header>

      <div class="matched-list">
        <article v-for="spot in preview.spots || []" :key="`${spot.matchedDestinationId}-${spot.orderIndex}`">
          <strong>{{ importSpotLabel(spot) }}</strong>
          <span>Confidence {{ Math.round((spot.confidence || 0) * 100) }}%</span>
          <small>{{ spot.notes || spot.rawName }}</small>
        </article>
      </div>

      <div v-if="preview.unmatchedSpots?.length" class="unmatched-list">
        <el-tag v-for="spot in preview.unmatchedSpots" :key="`${spot.rawName}-${spot.orderIndex}`" type="warning" effect="plain">
          {{ spot.rawName }} · {{ spot.reason }}
        </el-tag>
      </div>

      <div v-if="preview.warnings?.length" class="unmatched-list">
        <el-tag v-for="warning in preview.warnings" :key="warning" type="warning" effect="plain">{{ warning }}</el-tag>
      </div>
    </section>

    <template #footer>
      <el-button @click="close">Cancel</el-button>
      <el-button :loading="previewing" :disabled="!canPreview" @click="previewImport">
        <DocumentAdd theme="outline" size="16" fill="currentColor" />
        Recognize
      </el-button>
      <el-button type="primary" :loading="creating" :disabled="!canCreate" @click="createImport">
        Generate itinerary
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.guide-import-dialog :deep(.el-dialog__body) {
  display: grid;
  gap: 14px;
}

.diary-picker {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  margin-bottom: 12px;
}

.diary-list,
.import-preview,
.matched-list,
.unmatched-list {
  display: grid;
  gap: 10px;
}

.diary-choice {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  color: #111827;
  text-align: left;
  cursor: pointer;
}

.diary-choice.active {
  border-color: #ff385c;
  box-shadow: 0 0 0 2px rgba(255, 56, 92, 0.12);
}

.diary-choice strong,
.diary-choice small,
.import-preview header strong,
.import-preview header span {
  display: block;
}

.diary-choice small {
  margin-top: 4px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.import-preview {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.import-preview header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.matched-list article {
  padding: 10px;
  border-radius: 8px;
  background: #ffffff;
}

.matched-list strong,
.matched-list span,
.matched-list small {
  display: block;
}

.matched-list span,
.matched-list small,
.import-preview header span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.unmatched-list {
  display: flex;
  flex-wrap: wrap;
}
</style>
```

- [ ] **Step 2: Build frontend**

Run:

```powershell
npm.cmd --prefix data-structure-design-frontend run build
```

Expected: build succeeds.

- [ ] **Step 3: Commit**

```bash
git add data-structure-design-frontend/src/components/itinerary/GuideImportDialog.vue
git commit -m "feat: add guide import dialog"
```

## Task 9: Itinerary Page Integration

**Files:**
- Modify: `data-structure-design-frontend/src/views/ItineraryView.vue`

- [ ] **Step 1: Update imports**

Add `DocumentAdd` to the icon imports and import the new dialog:

```js
import { Connection, Copy, DocumentAdd, Refresh, Search } from '@icon-park/vue-next'
import GuideImportDialog from '../components/itinerary/GuideImportDialog.vue'
```

- [ ] **Step 2: Add import dialog state**

Add near existing refs:

```js
const importOpen = ref(false)
```

- [ ] **Step 3: Add create-success handler**

Add near `applyPlannerPreview`:

```js
const handleImportCreated = async (response) => {
  if (!response?.itinerary?.id) return
  updateRowInList(response.itinerary)
  await openCollaboration(response.itinerary)
  plannerOpen.value = true
  plannerPreview.value = null
  ElMessage.success('Imported itinerary created')
}
```

- [ ] **Step 4: Add import button in the toolbar**

In the main toolbar actions, add:

```vue
<el-button type="primary" @click="importOpen = true">
  <DocumentAdd theme="outline" size="16" fill="currentColor" />
  导入攻略
</el-button>
```

Keep the existing refresh/search behavior unchanged.

- [ ] **Step 5: Mount dialog at the end of the page template**

Before the closing `</section>` of the root page, add:

```vue
<GuideImportDialog
  v-model="importOpen"
  :owner="currentEditorName"
  @created="handleImportCreated"
/>
```

- [ ] **Step 6: Build frontend**

Run:

```powershell
npm.cmd --prefix data-structure-design-frontend run build
```

Expected: build succeeds.

- [ ] **Step 7: Commit**

```bash
git add data-structure-design-frontend/src/views/ItineraryView.vue
git commit -m "feat: open imported itineraries in planner"
```

## Task 10: Full Verification

**Files:**
- No new files.

- [ ] **Step 1: Run backend focused tests**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd test '-Dtest=TravelGuideExtractionServiceTest,DestinationMatchServiceTest,ItineraryImportServiceTest,ItineraryImportControllerTest'
```

Expected: all focused backend tests pass.

- [ ] **Step 2: Run full backend test suite**

Run:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading'; mvn.cmd test
```

Expected: all backend tests pass.

- [ ] **Step 3: Run frontend tests**

Run:

```powershell
npm.cmd --prefix data-structure-design-frontend test
```

Expected: all frontend tests pass.

- [ ] **Step 4: Build frontend**

Run:

```powershell
npm.cmd --prefix data-structure-design-frontend run build
```

Expected: Vite build succeeds and produces `data-structure-design-frontend/dist`.

- [ ] **Step 5: Manual DeepSeek-backed smoke test**

Set local environment variables in the backend shell. Do not write the key to disk:

```powershell
$env:PROJECT_OWNER_DEEPSEEK_API_KEY = Read-Host 'DeepSeek API key for this shell only'
$env:LLM_BASE_URL = 'https://api.deepseek.com/v1'
$env:LLM_MODEL = 'deepseek-chat'
$env:LLM_API_KEY = $env:PROJECT_OWNER_DEEPSEEK_API_KEY
```

Start backend:

```powershell
$env:JAVA_HOME='D:\software\jdk-26'; $env:PATH='D:\software\jdk-26\bin;D:\codex-deps\tools\apache-maven-3.9.15\bin;' + $env:PATH; $env:MAVEN_OPTS='-Djdk.attach.allowAttachSelf=true'; mvn.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

Start frontend:

```powershell
npm.cmd --prefix data-structure-design-frontend run dev -- --host 127.0.0.1 --port 5173
```

In the browser:

1. Open `http://localhost:5173/#/itineraries`.
2. Click `导入攻略`.
3. Paste guide text containing known destinations from the local database.
4. Click `Recognize`.
5. Confirm matched spots appear.
6. Click `Generate itinerary`.
7. Confirm the collaboration drawer opens and the one-click planner panel is expanded.

Expected: imported itinerary is created, matched spots appear on the tactical map, and the planner is ready to generate a route preview.

- [ ] **Step 6: Final status check**

Run:

```powershell
git status --short
```

Expected: only intentional source, test, and build artifact changes remain. Do not include local secret files or shell history in commits.

## Self-Review

- Spec coverage: pasted text import, diary import, LLM extraction, local fallback, destination matching, create endpoint, frontend dialog, itinerary page integration, planner handoff, and testing are each covered by tasks.
- Secret handling: the DeepSeek key is intentionally not written into this plan or any planned repository file; only environment variable names are documented.
- Scope control: webpage scraping, OCR, file import, and full day-by-day schedule persistence remain out of scope.
- Type consistency: DTO names used by controller, service, and frontend API are consistent across tasks.
