package com.travel.system.service;

import com.travel.system.mapper.DiaryMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.Diary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
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

    private final DiaryHeatService heatService = new DiaryHeatService();
    private final DiaryAigcService aigcService = new DiaryAigcService();
    private DiaryService service;

    @BeforeEach
    void setUp() {
        service = new DiaryService(diaryMapper, heatService, mediaService, aigcService, authTokenService, imageStorageService);
    }

    @Test
    void rateStoresNewRatingAndUpdatesCompositeScore() {
        Diary diary = new Diary();
        diary.setId(7L);
        diary.setScore(3.0);
        diary.setLikeCount(12L);
        when(diaryMapper.findById(7L)).thenReturn(diary);
        when(diaryMapper.findAverageRatingByDiaryId(7L)).thenReturn(4.4);

        Diary result = service.rate(7L, 4.8);

        verify(diaryMapper).insertRating(eq(7L), eq(4.8), any(LocalDateTime.class));
        verify(diaryMapper).updateScore(eq(7L), eq(4.4), anyDouble());
        assertThat(result.getScore()).isEqualTo(4.4);
    }

    @Test
    void exactTitleDelegatesToMapper() {
        Diary diary = new Diary();
        diary.setTitle("Palace Day");
        when(diaryMapper.findByExactTitle("Palace Day")).thenReturn(diary);

        assertThat(service.findExactTitle("Palace Day")).isSameAs(diary);
    }

    @Test
    void demoUserCanDeleteAnyDiary() {
        Diary diary = new Diary();
        diary.setId(12L);
        diary.setAuthorName("other-user");
        when(diaryMapper.findById(12L)).thenReturn(diary);
        when(authTokenService.displayName(null, "demo")).thenReturn(Optional.of("demo"));

        assertThatCode(() -> service.delete(12L, null, "demo")).doesNotThrowAnyException();

        verify(diaryMapper).deleteCommentsByDiaryId(12L);
        verify(diaryMapper).deleteById(12L);
    }

    @Test
    void destinationSearchMatchesDestinationAndDiaryText() {
        Destination bupt = new Destination();
        bupt.setName("北京邮电大学");
        bupt.setCategory("校园");
        bupt.setDescription("北邮主楼和图书馆很适合散步");

        Destination beihai = new Destination();
        beihai.setName("北海公园");
        beihai.setCategory("自然");
        beihai.setDescription("白塔和湖景很出片");

        Diary buptDiary = new Diary();
        buptDiary.setId(21L);
        buptDiary.setTitle("北邮春日打卡");
        buptDiary.setContent("主楼旁的银杏大道很好看");
        buptDiary.setDestination(bupt);
        buptDiary.setLikeCount(8L);
        buptDiary.setScore(4.7);
        buptDiary.setIsPublic(true);

        Diary beihaiDiary = new Diary();
        beihaiDiary.setId(22L);
        beihaiDiary.setTitle("白塔日落散步");
        beihaiDiary.setContent("北海的傍晚很舒服");
        beihaiDiary.setDestination(beihai);
        beihaiDiary.setLikeCount(12L);
        beihaiDiary.setScore(4.6);
        beihaiDiary.setIsPublic(true);

        when(diaryMapper.findAll()).thenReturn(java.util.List.of(buptDiary, beihaiDiary));

        assertThat(service.byDestination("北邮", "recommend", 20)).extracting(Diary::getId).containsExactly(21L);
        assertThat(service.byDestination("北", "recommend", 20)).extracting(Diary::getId).containsExactly(22L, 21L);
    }
}
