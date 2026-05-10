package com.travel.system.service;

import com.travel.system.mapper.DiaryMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.Diary;
import com.travel.system.model.DiaryComment;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DiaryService {

    private final DiaryMapper diaryRepository;
    private final DiaryHeatService heatService;
    private final DiaryMediaService mediaService;
    private final DiaryAigcService aigcService;
    private final AuthTokenService authTokenService;
    private final DiaryImageStorageService imageStorageService;

    public DiaryService(DiaryMapper diaryRepository,
                        DiaryHeatService heatService,
                        DiaryMediaService mediaService,
                        DiaryAigcService aigcService,
                        AuthTokenService authTokenService,
                        DiaryImageStorageService imageStorageService) {
        this.diaryRepository = diaryRepository;
        this.heatService = heatService;
        this.mediaService = mediaService;
        this.aigcService = aigcService;
        this.authTokenService = authTokenService;
        this.imageStorageService = imageStorageService;
    }

    /**

     * 按查询条件读取列表数据；分页、过滤或排序规则由 service 层统一处理。

     */
    public List<Diary> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return diaryRepository.findRecent(20);
        }
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword, 20);
    }

    /**

     * 保存或更新实体数据，并返回数据库持久化后的结果。

     */
    public Diary save(Diary diary, String authorizationHeader, String userNameHeader) {
        diary.setAuthorName(currentDisplayName(authorizationHeader, userNameHeader));
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
        if (diary.getMediaType() == null || diary.getMediaType().isBlank()) {
            diary.setMediaType("text");
        }
        mediaService.enrichCompression(diary);
        if ("image".equals(diary.getMediaType())) {
            try {
                diary.setMediaUrl(imageStorageService.saveDataUrlIfNeeded(diary.getMediaUrl()));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid diary image", exception);
            }
            diary.setCompressedMediaUrl(null);
        }
        aigcService.enrichAnimation(diary);
        diary.setHeatScore(heatService.compute(diary));
        return diaryRepository.save(diary);
    }

    public void delete(Long id, String authorizationHeader, String userNameHeader) {
        Diary diary = diaryRepository.findById(id);
        if (diary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary not found: " + id);
        }
        String currentUser = currentDisplayName(authorizationHeader, userNameHeader);
        if (!currentUser.equals(normalize(diary.getAuthorName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can delete this diary");
        }
        imageStorageService.deleteStoredImageIfOwned(diary.getMediaUrl());
        diaryRepository.deleteCommentsByDiaryId(id);
        diaryRepository.deleteById(id);
    }

    /**

     * 执行面向游记内容的关键词检索，关键词为空时返回常规列表结果。

     */
    public List<Diary> fullTextSearch(String keyword) {
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword, 20);
    }

    public List<Diary> hot(int limit) {
        return diaryRepository.findHotPublic(Math.max(1, Math.min(limit, 20)));
    }

    public Diary shared(String shareToken) {
        return diaryRepository.findByShareToken(shareToken);
    }

    public Diary detail(Long id) {
        Diary diary = diaryRepository.findById(id);
        if (diary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary not found: " + id);
        }
        return diary;
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

    private String currentDisplayName(String authorizationHeader, String userNameHeader) {
        return authTokenService.displayName(authorizationHeader, userNameHeader)
                .map(this::normalize)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login first"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
