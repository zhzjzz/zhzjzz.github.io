package com.travel.system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.system.model.Diary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DiaryAigcService {

    private static final String FALLBACK_IMAGE_URL = "/demo/aigc/diary-default-generated.png";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aigc.image.api-url:}")
    private String apiUrl;

    @Value("${aigc.image.api-key:}")
    private String apiKey;

    @Value("${aigc.image.model:}")
    private String model;

    public void enrichAnimation(Diary diary) {
        if (diary == null) {
            return;
        }
        if (apiUrl == null || apiUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            diary.setAigcAnimationUrl(FALLBACK_IMAGE_URL);
            diary.setAigcStatus("generated");
            return;
        }
        try {
            String prompt = "Generate a travel diary illustration for: "
                    + safe(diary.getTitle()) + " " + safe(diary.getContent());
            diary.setAigcAnimationUrl(callImageApi(prompt));
            diary.setAigcStatus("generated");
        } catch (RuntimeException exception) {
            diary.setAigcAnimationUrl(FALLBACK_IMAGE_URL);
            diary.setAigcStatus("failed");
        }
    }

    private String callImageApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        Map<String, Object> payload = model == null || model.isBlank()
                ? Map.of("prompt", prompt, "image_size", "1024x1024", "batch_size", 1)
                : Map.of("model", model, "prompt", prompt, "image_size", "1024x1024", "batch_size", 1);
        String response = restTemplate.postForObject(imageApiEndpoint(apiUrl), new HttpEntity<>(payload, headers), String.class);
        String imageUrl = extractImageUrl(response);
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalStateException("Image API returned no URL");
        }
        return imageUrl;
    }

    static String imageApiEndpoint(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isBlank()) {
            return "";
        }
        String normalized = configuredUrl.trim().replaceAll("/+$", "");
        if (normalized.endsWith("/images/generations")) {
            return normalized;
        }
        return normalized + "/images/generations";
    }

    private String extractImageUrl(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            for (String field : List.of("url", "image_url")) {
                JsonNode value = root.get(field);
                if (value != null && value.isTextual()) {
                    return value.asText();
                }
            }
            JsonNode data = root.get("data");
            if (data != null && data.isArray() && !data.isEmpty()) {
                JsonNode first = data.get(0);
                JsonNode url = first.get("url");
                if (url != null && url.isTextual()) {
                    return url.asText();
                }
                JsonNode imageUrl = first.get("image_url");
                if (imageUrl != null && imageUrl.isTextual()) {
                    return imageUrl.asText();
                }
            }
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
