package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.Deflater;

@Service
public class DiaryMediaService {
    private static final String DATA_URL_MARKER = ";base64,";
    private static final String DEFLATE_DATA_URL_PREFIX = "data:application/octet-stream;compression=deflate;base64,";
    private static final String JPEG_DATA_URL_PREFIX = "data:image/jpeg;base64,";
    private static final int IMAGE_MAX_EDGE = 1280;
    private static final float JPEG_QUALITY = 0.78f;

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
        CompressionResult imageResult = optimizeImage(diary.getMediaUrl(), source);
        byte[] compressed = imageResult.bytes.length > 0 ? imageResult.bytes : deflate(source);

        diary.setOriginalSizeBytes(original);
        if (compressed.length > 0 && compressed.length < source.length) {
            diary.setCompressedSizeBytes((long) compressed.length);
            if (imageResult.bytes.length > 0) {
                diary.setCompressedMediaUrl(JPEG_DATA_URL_PREFIX + Base64.getEncoder().encodeToString(compressed));
                diary.setCompressionStatus("image_jpeg_optimized");
            } else {
                diary.setCompressedMediaUrl(DEFLATE_DATA_URL_PREFIX + Base64.getEncoder().encodeToString(compressed));
                diary.setCompressionStatus("lossless_deflate");
            }
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

    private CompressionResult optimizeImage(String mediaUrl, byte[] source) {
        if (!mediaUrl.toLowerCase(Locale.ROOT).startsWith("data:image/")) {
            return CompressionResult.empty();
        }
        try {
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(source));
            if (image == null) {
                return CompressionResult.empty();
            }

            double scale = Math.min(1.0, (double) IMAGE_MAX_EDGE / Math.max(image.getWidth(), image.getHeight()));
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
            BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = output.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(java.awt.Color.WHITE);
                graphics.fillRect(0, 0, width, height);
                graphics.drawImage(image, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }
            return new CompressionResult(writeJpeg(output, JPEG_QUALITY));
        } catch (IOException | RuntimeException ignored) {
            return CompressionResult.empty();
        }
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            return new byte[0];
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), params);
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
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

    private record CompressionResult(byte[] bytes) {
        static CompressionResult empty() {
            return new CompressionResult(new byte[0]);
        }
    }

}
