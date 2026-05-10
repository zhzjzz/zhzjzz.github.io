package com.travel.system.service;

import com.travel.system.model.Diary;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class DiaryMediaServiceTest {
    private final DiaryMediaService service = new DiaryMediaService();

    @Test
    void compressesBase64DataUrlWithDeflateWhenSmaller() {
        Diary diary = new Diary();
        byte[] repeatedBytes = "travel-memory-".repeat(512).getBytes(StandardCharsets.UTF_8);
        String payload = Base64.getEncoder().encodeToString(repeatedBytes);
        diary.setMediaUrl("data:image/plain;base64," + payload);

        service.enrichCompression(diary);

        assertThat(diary.getOriginalSizeBytes()).isEqualTo(repeatedBytes.length);
        assertThat(diary.getCompressedSizeBytes()).isLessThan(diary.getOriginalSizeBytes());
        assertThat(diary.getCompressedMediaUrl()).startsWith("data:application/octet-stream;compression=deflate;base64,");
        assertThat(diary.getCompressionStatus()).isEqualTo("lossless_deflate");
    }

    @Test
    void marksEmptyMediaAsNone() {
        Diary diary = new Diary();

        service.enrichCompression(diary);

        assertThat(diary.getOriginalSizeBytes()).isZero();
        assertThat(diary.getCompressedSizeBytes()).isZero();
        assertThat(diary.getCompressedMediaUrl()).isNull();
        assertThat(diary.getCompressionStatus()).isEqualTo("none");
    }
}
