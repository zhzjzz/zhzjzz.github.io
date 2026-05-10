package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;

@Service
public class DiaryMediaService {
    private static final String DATA_URL_MARKER = ";base64,";
    private static final String DEFLATE_DATA_URL_PREFIX = "data:application/octet-stream;compression=deflate;base64,";

    public void enrichCompression(Diary diary) {
        if (diary.getMediaUrl() == null || diary.getMediaUrl().isBlank()) {
            diary.setCompressionStatus("none");
            diary.setOriginalSizeBytes(0L);
            diary.setCompressedSizeBytes(0L);
            diary.setCompressedMediaUrl(null);
            return;
        }

        byte[] source = extractStoredBytes(diary.getMediaUrl());
        long original = source.length;
        byte[] compressed = deflate(source);

        diary.setOriginalSizeBytes(original);
        if (compressed.length > 0 && compressed.length < source.length) {
            diary.setCompressedSizeBytes((long) compressed.length);
            diary.setCompressedMediaUrl(DEFLATE_DATA_URL_PREFIX + Base64.getEncoder().encodeToString(compressed));
            diary.setCompressionStatus("lossless_deflate");
        } else {
            diary.setCompressedSizeBytes(original);
            diary.setCompressedMediaUrl(diary.getMediaUrl());
            diary.setCompressionStatus("already_optimal");
        }
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

    private byte[] deflate(byte[] source) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(source);
        deflater.finish();
        byte[] buffer = new byte[4096];
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(source.length)) {
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                if (count <= 0) {
                    break;
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } catch (Exception ignored) {
            return new byte[0];
        } finally {
            deflater.end();
        }
    }
}
