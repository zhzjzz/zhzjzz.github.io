package com.travel.system.service;

import com.travel.system.model.Diary;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    void optimizesBase64ImageDataUrlWhenJpegIsSmaller() throws IOException {
        Diary diary = new Diary();
        BufferedImage image = new BufferedImage(1200, 900, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            for (int y = 0; y < image.getHeight(); y += 30) {
                graphics.setColor(new Color((y * 3) % 255, (y * 7) % 255, (y * 11) % 255));
                graphics.fillRect(0, y, image.getWidth(), 30);
            }
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "bmp", output);
        byte[] originalBytes = output.toByteArray();
        String payload = Base64.getEncoder().encodeToString(originalBytes);
        diary.setMediaUrl("data:image/bmp;base64," + payload);

        service.enrichCompression(diary);

        assertThat(diary.getOriginalSizeBytes()).isEqualTo(originalBytes.length);
        assertThat(diary.getCompressedSizeBytes()).isLessThan(diary.getOriginalSizeBytes());
        assertThat(diary.getCompressedMediaUrl()).startsWith("data:image/jpeg;base64,");
        assertThat(diary.getCompressionStatus()).isEqualTo("image_jpeg_optimized");
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
