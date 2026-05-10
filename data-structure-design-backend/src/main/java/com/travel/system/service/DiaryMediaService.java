package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class DiaryMediaService {
    private static final String DATA_URL_MARKER = ";base64,";

    public void enrichCompression(Diary diary) {
        if (diary.getMediaUrl() == null || diary.getMediaUrl().isBlank()) {
            diary.setCompressionStatus("none");
            diary.setOriginalSizeBytes(0L);
            diary.setCompressedSizeBytes(0L);
            diary.setCompressedMediaUrl(null);
            return;
        }

        long original = extractStoredBytes(diary.getMediaUrl()).length;
        diary.setOriginalSizeBytes(original);
        diary.setCompressedSizeBytes(original);
        diary.setCompressedMediaUrl(null);
        diary.setCompressionStatus("stored_as_file");
    }

    private byte[] extractStoredBytes(String mediaUrl) {
        int base64Start = mediaUrl.indexOf(DATA_URL_MARKER);
        if (mediaUrl.startsWith("data:") && base64Start >= 0) {
            String payload = mediaUrl.substring(base64Start + DATA_URL_MARKER.length());
            try {
                return Base64.getDecoder().decode(payload);
            } catch (IllegalArgumentException ignored) {
                return mediaUrl.getBytes(StandardCharsets.UTF_8);
            }
        }
        return mediaUrl.getBytes(StandardCharsets.UTF_8);
    }

}
