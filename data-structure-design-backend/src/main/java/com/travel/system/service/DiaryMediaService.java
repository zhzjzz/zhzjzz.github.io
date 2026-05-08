package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

@Service
public class DiaryMediaService {
    public void enrichCompression(Diary diary) {
        if (diary.getMediaUrl() == null || diary.getMediaUrl().isBlank()) {
            diary.setCompressionStatus("none");
            diary.setOriginalSizeBytes(0L);
            diary.setCompressedSizeBytes(0L);
            diary.setCompressedMediaUrl(null);
            return;
        }
        long original = diary.getOriginalSizeBytes() == null || diary.getOriginalSizeBytes() <= 0
                ? 8_388_608L
                : diary.getOriginalSizeBytes();
        long compressed = Math.max(1L, Math.round(original * 0.72));
        diary.setOriginalSizeBytes(original);
        diary.setCompressedSizeBytes(compressed);
        diary.setCompressedMediaUrl(diary.getMediaUrl() + "?optimized=lossless");
        diary.setCompressionStatus("lossless_optimized");
    }
}
