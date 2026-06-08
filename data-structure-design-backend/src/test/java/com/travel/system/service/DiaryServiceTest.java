package com.travel.system.service;

import com.travel.system.mapper.DiaryMapper;
import com.travel.system.model.Diary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

    @Mock
    DiaryMapper diaryMapper;
    @Mock
    DiaryMediaService mediaService;
    @Mock
    DiaryImageStorageService imageStorageService;
    @Mock
    AuthTokenService authTokenService;

    DiaryHeatService heatService = new DiaryHeatService();
    DiaryAigcService aigcService = new DiaryAigcService();
    DiaryService service;

    @BeforeEach
    void setUp() {
        service = new DiaryService(diaryMapper, heatService, mediaService, aigcService, authTokenService, imageStorageService);
    }

    @Test
    void rateUpdatesScoreAndHeat() {
        Diary diary = new Diary();
        diary.setId(7L);
        diary.setScore(3.0);
        diary.setViews(20L);
        when(diaryMapper.findById(7L)).thenReturn(diary);

        Diary result = service.rate(7L, 4.8);

        verify(diaryMapper).updateScore(eq(7L), eq(4.8), anyDouble());
        assertThat(result.getScore()).isEqualTo(4.8);
    }

    @Test
    void exactTitleDelegatesToMapper() {
        Diary diary = new Diary();
        diary.setTitle("Palace Day");
        when(diaryMapper.findByExactTitle("Palace Day")).thenReturn(diary);

        assertThat(service.findExactTitle("Palace Day")).isSameAs(diary);
    }
}
