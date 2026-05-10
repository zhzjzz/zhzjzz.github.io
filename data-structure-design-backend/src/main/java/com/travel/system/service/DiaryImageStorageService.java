package com.travel.system.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DiaryImageStorageService {
    private static final String BASE64_MARKER = ";base64,";
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final Path diaryUploadDir = Path.of("data", "uploads", "diaries").toAbsolutePath().normalize();

    public String saveDataUrlIfNeeded(String mediaUrl) {
        if (mediaUrl == null || !mediaUrl.startsWith("data:image/")) {
            return mediaUrl;
        }

        int markerIndex = mediaUrl.indexOf(BASE64_MARKER);
        if (markerIndex < 0) {
            return mediaUrl;
        }

        String mimeType = mediaUrl.substring("data:".length(), markerIndex).toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.getOrDefault(mimeType, "jpg");
        String payload = mediaUrl.substring(markerIndex + BASE64_MARKER.length());
        byte[] bytes = Base64.getDecoder().decode(payload);

        try {
            Files.createDirectories(diaryUploadDir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path target = diaryUploadDir.resolve(fileName).normalize();
            if (!target.startsWith(diaryUploadDir)) {
                throw new IllegalStateException("Invalid diary image path");
            }
            Files.write(target, bytes);
            return "/uploads/diaries/" + fileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store diary image", exception);
        }
    }
}
