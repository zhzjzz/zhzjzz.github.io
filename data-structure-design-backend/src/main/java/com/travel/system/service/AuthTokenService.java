package com.travel.system.service;

import org.springframework.stereotype.Service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthTokenService {
    private final Map<String, String> tokenDisplayNames = new ConcurrentHashMap<>();

    public void remember(String token, String displayName) {
        if (hasText(token) && hasText(displayName)) {
            tokenDisplayNames.put(token, displayName);
        }
    }

    public Optional<String> displayName(String authorizationHeader, String fallbackDisplayName) {
        String token = normalize(authorizationHeader);
        if (hasText(token)) {
            String displayName = tokenDisplayNames.get(token);
            if (hasText(displayName)) {
                return Optional.of(displayName);
            }
        }
        String decodedFallback = decode(fallbackDisplayName);
        return hasText(decodedFallback) ? Optional.of(decodedFallback.trim()) : Optional.empty();
    }

    private String normalize(String authorizationHeader) {
        if (!hasText(authorizationHeader)) {
            return "";
        }
        String trimmed = authorizationHeader.trim();
        return trimmed.startsWith("Bearer ") ? trimmed.substring(7).trim() : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String decode(String value) {
        if (!hasText(value)) {
            return "";
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
