package com.travel.system.service;

import com.travel.system.model.Diary;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiaryHeatServiceTest {
    private final DiaryHeatService service = new DiaryHeatService();

    @Test
    void computesHeatFromRatingTrafficAndInteractions() {
        Diary diary = new Diary();
        diary.setScore(4.7);
        diary.setViews(120L);
        diary.setLikeCount(8L);
        diary.setFavoriteCount(3L);
        diary.setCommentCount(2L);
        diary.setShareCount(1L);

        double heat = service.compute(diary);

        assertThat(heat).isEqualTo(157.0);
    }

    @Test
    void treatsMissingNumbersAsZero() {
        Diary diary = new Diary();

        double heat = service.compute(diary);

        assertThat(heat).isEqualTo(0.0);
    }
}
