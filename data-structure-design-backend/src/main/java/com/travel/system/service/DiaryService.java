package com.travel.system.service;

import com.travel.system.mapper.DiaryMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.Diary;
import com.travel.system.model.DiaryComment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DiaryService {

    private final DiaryMapper diaryRepository;
    private final DiaryHeatService heatService;
    private final DiaryMediaService mediaService;
    private final DiaryAigcService aigcService;

    public DiaryService(DiaryMapper diaryRepository,
                        DiaryHeatService heatService,
                        DiaryMediaService mediaService,
                        DiaryAigcService aigcService) {
        this.diaryRepository = diaryRepository;
        this.heatService = heatService;
        this.mediaService = mediaService;
        this.aigcService = aigcService;
    }

    public List<Diary> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return diaryRepository.findAll();
        }
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword);
    }

    public Diary save(Diary diary) {
        if (diary.getViews() == null) diary.setViews(0L);
        if (diary.getLikeCount() == null) diary.setLikeCount(0L);
        if (diary.getFavoriteCount() == null) diary.setFavoriteCount(0L);
        if (diary.getCommentCount() == null) diary.setCommentCount(0L);
        if (diary.getShareCount() == null) diary.setShareCount(0L);
        if (diary.getIsPublic() == null) diary.setIsPublic(true);
        if (diary.getPublishedAt() == null) diary.setPublishedAt(LocalDateTime.now());
        if (diary.getDestination() == null) diary.setDestination(new Destination());
        if (diary.getShareToken() == null || diary.getShareToken().isBlank()) {
            diary.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        }
        mediaService.enrichCompression(diary);
        aigcService.enrichAnimation(diary);
        diary.setHeatScore(heatService.compute(diary));
        return diaryRepository.save(diary);
    }

    public List<Diary> fullTextSearch(String keyword) {
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword);
    }

    public List<Diary> hot(int limit) {
        return diaryRepository.findHotPublic(Math.max(1, Math.min(limit, 20)));
    }

    public Diary shared(String shareToken) {
        return diaryRepository.findByShareToken(shareToken);
    }

    public Diary interact(Long id, String type) {
        Diary diary = diaryRepository.findById(id);
        if (diary == null) {
            throw new IllegalArgumentException("Diary not found: " + id);
        }
        diary.setViews(value(diary.getViews()) + 1);
        if ("like".equals(type)) diary.setLikeCount(value(diary.getLikeCount()) + 1);
        if ("favorite".equals(type)) diary.setFavoriteCount(value(diary.getFavoriteCount()) + 1);
        if ("share".equals(type)) diary.setShareCount(value(diary.getShareCount()) + 1);
        diary.setHeatScore(heatService.compute(diary));
        diaryRepository.updateCounters(diary);
        return diaryRepository.findById(id);
    }

    public DiaryComment comment(Long id, DiaryComment comment) {
        Diary diary = diaryRepository.findById(id);
        if (diary == null) {
            throw new IllegalArgumentException("Diary not found: " + id);
        }
        comment.setDiaryId(id);
        if (comment.getAuthorName() == null || comment.getAuthorName().isBlank()) {
            comment.setAuthorName("游客");
        }
        comment.setCreatedAt(LocalDateTime.now());
        diaryRepository.insertComment(comment);
        diary.setCommentCount(value(diary.getCommentCount()) + 1);
        diary.setHeatScore(heatService.compute(diary));
        diaryRepository.updateCounters(diary);
        return comment;
    }

    public List<DiaryComment> comments(Long id) {
        return diaryRepository.findCommentsByDiaryId(id);
    }

    private long value(Long number) {
        return number == null ? 0L : number;
    }
}
