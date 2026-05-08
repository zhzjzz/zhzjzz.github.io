package com.travel.system.service;

import com.travel.system.model.Diary;
import org.springframework.stereotype.Service;

@Service
public class DiaryAigcService {
    public void enrichAnimation(Diary diary) {
        String title = diary.getTitle() == null ? "travel-memory" : diary.getTitle().trim();
        String slug = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]+", "-");
        if (slug.isBlank()) {
            slug = "travel-memory";
        }
        diary.setAigcAnimationUrl("/demo/aigc/diary-" + slug + ".mp4");
        diary.setAigcStatus("generated");
    }
}
