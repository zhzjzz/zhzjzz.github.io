package com.travel.system.service;

import com.travel.system.mapper.DiaryMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.Diary;
import com.travel.system.model.DiaryComment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DiaryService {

    private static final String DEMO_DISPLAY_NAME = "演示用户";
    private static final String DEMO_USERNAME = "demo";

    public enum DiaryRankingMode {
        RECOMMEND,
        HEAT,
        SCORE,
        INTEREST
    }

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

    public List<Diary> list(String keyword) {
        return list(keyword, null, null, 20);
    }

    public List<Diary> list(String keyword, String sort, String interest, int limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        int safeLimit = clampLimit(limit);
        if (!normalizedKeyword.isEmpty()) {
            return trimToLimit(
                    sortDiaries(searchByKeyword(visibleDiaries(), normalizedKeyword), parseRankingMode(sort), normalizeInterest(interest)),
                    safeLimit
            );
        }
        return trimToLimit(sortDiaries(visibleDiaries(), parseRankingMode(sort), normalizeInterest(interest)), safeLimit);
    }

    public Diary save(Diary diary, String authorizationHeader, String userNameHeader) {
        boolean creating = diary.getId() == null;
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
                if ("image_jpeg_optimized".equals(diary.getCompressionStatus()) && diary.getCompressedMediaUrl() != null) {
                    diary.setMediaUrl(diary.getCompressedMediaUrl());
                }
                diary.setMediaUrl(imageStorageService.saveDataUrlIfNeeded(diary.getMediaUrl()));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid diary image", exception);
            }
            diary.setCompressedMediaUrl(null);
        } else {
            // 只在没有上传图片时才生成AI图片
            aigcService.enrichAnimation(diary);
        }
        diary.setHeatScore(heatService.compute(diary));
        Diary saved = diaryRepository.save(diary);
        if (creating && saved.getId() != null && saved.getScore() != null) {
            diaryRepository.insertRating(saved.getId(), saved.getScore(), saved.getPublishedAt() == null ? LocalDateTime.now() : saved.getPublishedAt());
        }
        return saved;
    }

    public void delete(Long id, String authorizationHeader, String userNameHeader) {
        Diary diary = diaryRepository.findById(id);
        if (diary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary not found: " + id);
        }
        String currentUser = currentDisplayName(authorizationHeader, userNameHeader);
        if (!isDeleteAllowed(currentUser, diary)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can delete this diary");
        }
        imageStorageService.deleteStoredImageIfOwned(diary.getMediaUrl());
        diaryRepository.deleteCommentsByDiaryId(id);
        diaryRepository.deleteById(id);
    }

    public List<Diary> fullTextSearch(String keyword) {
        return fullTextSearch(keyword, 20);
    }

    public List<Diary> fullTextSearch(String keyword, int limit) {
        return fullTextSearch(keyword, null, null, limit);
    }

    public List<Diary> fullTextSearch(String keyword, String sort, String interest, int limit) {
        String normalized = keyword == null ? "" : keyword.trim();
        int safeLimit = clampLimit(limit);
        if (normalized.isEmpty()) {
            return trimToLimit(sortDiaries(visibleDiaries(), DiaryRankingMode.RECOMMEND, normalizeInterest(null)), safeLimit);
        }
        return trimToLimit(
                sortDiaries(searchByKeyword(visibleDiaries(), normalized), parseRankingMode(sort), normalizeInterest(interest)),
                safeLimit
        );
    }

    public List<Diary> byDestination(String keyword, int limit) {
        return byDestination(keyword, null, limit);
    }

    public List<Diary> byDestination(String keyword, String sort, int limit) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<Diary> matched = searchByDestinationKeyword(visibleDiaries(), normalized);
        return trimToLimit(sortDiaries(matched, parseRankingMode(sort), normalizeInterest(null)), clampLimit(limit));
    }

    public Diary findExactTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        return normalized.isEmpty() ? null : diaryRepository.findByExactTitle(normalized);
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

    public Diary rate(Long id, Double score) {
        Diary diary = detail(id);
        double normalizedScore = Math.max(1.0, Math.min(score == null ? 5.0 : score, 5.0));
        diaryRepository.insertRating(id, normalizedScore, LocalDateTime.now());
        Double averageScore = diaryRepository.findAverageRatingByDiaryId(id);
        diary.setScore(averageScore == null ? normalizedScore : roundScore(averageScore));
        diary.setHeatScore(heatService.compute(diary));
        diaryRepository.updateScore(id, diary.getScore(), diary.getHeatScore());
        return diaryRepository.findById(id);
    }

    public Diary generateAigcImage(Long id) {
        Diary diary = detail(id);
        aigcService.enrichAnimation(diary);
        diaryRepository.save(diary);
        return diaryRepository.findById(id);
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

    private boolean isDeleteAllowed(String currentUser, Diary diary) {
        String normalizedUser = normalize(currentUser);
        if (normalizedUser.equals(normalize(diary.getAuthorName()))) {
            return true;
        }
        return DEMO_DISPLAY_NAME.equals(normalizedUser) || DEMO_USERNAME.equalsIgnoreCase(normalizedUser);
    }

    private int clampLimit(int limit) {
        return Math.max(1, Math.min(limit, 50));
    }

    private List<Diary> visibleDiaries() {
        return diaryRepository.findAll().stream()
                .filter(diary -> !Boolean.FALSE.equals(diary.getIsPublic()))
                .toList();
    }

    private List<Diary> searchByDestinationKeyword(List<Diary> diaries, String keyword) {
        List<Diary> matched = new ArrayList<>();
        for (Diary diary : diaries) {
            String destinationText = destinationSearchText(diary);
            if (containsIgnoreCase(destinationText, keyword)) {
                matched.add(diary);
            }
        }
        return matched;
    }

    private List<Diary> searchByKeyword(List<Diary> diaries, String keyword) {
        List<Diary> matched = new ArrayList<>();
        for (Diary diary : diaries) {
            String destinationName = diary.getDestination() == null ? "" : normalize(diary.getDestination().getName());
            String text = String.join(" ",
                    normalize(diary.getTitle()),
                    normalize(diary.getContent()),
                    destinationName);
            if (containsIgnoreCase(text, keyword)) {
                matched.add(diary);
            }
        }
        return matched;
    }

    private String destinationSearchText(Diary diary) {
        Destination destination = diary.getDestination();
        return String.join(" ",
                destination == null ? "" : normalize(destination.getName()),
                destination == null ? "" : normalize(destination.getCategory()),
                destination == null ? "" : normalize(destination.getDescription()),
                normalize(diary.getTitle()),
                normalize(diary.getContent()));
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private List<Diary> sortDiaries(List<Diary> source, DiaryRankingMode mode, String interest) {
        if (source.size() < 2) {
            return source;
        }
        ArrayList<Diary> sorted = new ArrayList<>(source);
        ArrayList<Diary> buffer = new ArrayList<>(sorted);
        DiarySortContext context = DiarySortContext.from(sorted);
        Comparator<Diary> comparator = diaryComparator(mode, interest, context);
        mergeSort(sorted, buffer, 0, sorted.size(), comparator);
        return sorted;
    }

    private void mergeSort(List<Diary> data, List<Diary> buffer, int start, int end, Comparator<Diary> comparator) {
        if (end - start <= 1) {
            return;
        }
        int middle = (start + end) / 2;
        mergeSort(data, buffer, start, middle, comparator);
        mergeSort(data, buffer, middle, end, comparator);

        int left = start;
        int right = middle;
        int index = start;
        while (left < middle && right < end) {
            if (comparator.compare(data.get(left), data.get(right)) <= 0) {
                buffer.set(index++, data.get(left++));
            } else {
                buffer.set(index++, data.get(right++));
            }
        }
        while (left < middle) {
            buffer.set(index++, data.get(left++));
        }
        while (right < end) {
            buffer.set(index++, data.get(right++));
        }
        for (int i = start; i < end; i++) {
            data.set(i, buffer.get(i));
        }
    }

    private Comparator<Diary> diaryComparator(DiaryRankingMode mode, String interest, DiarySortContext context) {
        DiaryRankingMode rankingMode = mode == null ? DiaryRankingMode.RECOMMEND : mode;
        return (left, right) -> {
            int primary = switch (rankingMode) {
                case HEAT -> compareLongDesc(value(left.getLikeCount()), value(right.getLikeCount()));
                case SCORE -> compareDesc(safe(left.getScore()), safe(right.getScore()));
                case INTEREST -> compareDesc(interestScore(left, interest, context), interestScore(right, interest, context));
                case RECOMMEND -> compareDesc(recommendationScore(left, interest, context), recommendationScore(right, interest, context));
            };
            if (primary != 0) {
                return primary;
            }
            int byHeat = compareLongDesc(value(left.getLikeCount()), value(right.getLikeCount()));
            if (byHeat != 0) {
                return byHeat;
            }
            int byScore = compareDesc(safe(left.getScore()), safe(right.getScore()));
            if (byScore != 0) {
                return byScore;
            }
            int byPublishedAt = comparePublishedAtDesc(left, right);
            if (byPublishedAt != 0) {
                return byPublishedAt;
            }
            return Long.compare(right.getId() == null ? 0L : right.getId(), left.getId() == null ? 0L : left.getId());
        };
    }

    private DiaryRankingMode parseRankingMode(String sort) {
        if (sort == null || sort.isBlank()) {
            return DiaryRankingMode.RECOMMEND;
        }
        return switch (sort.trim().toLowerCase(Locale.ROOT)) {
            case "heat", "hot" -> DiaryRankingMode.HEAT;
            case "score", "rating" -> DiaryRankingMode.SCORE;
            case "interest", "personalized" -> DiaryRankingMode.INTEREST;
            default -> DiaryRankingMode.RECOMMEND;
        };
    }

    private List<Diary> trimToLimit(List<Diary> diaries, int limit) {
        if (diaries.size() <= limit) {
            return diaries;
        }
        return new ArrayList<>(diaries.subList(0, limit));
    }

    private int compareDesc(double left, double right) {
        return Double.compare(right, left);
    }

    private int compareLongDesc(long left, long right) {
        return Long.compare(right, left);
    }

    private int comparePublishedAtDesc(Diary left, Diary right) {
        LocalDateTime leftTime = left.getPublishedAt();
        LocalDateTime rightTime = right.getPublishedAt();
        if (leftTime == null && rightTime == null) {
            return 0;
        }
        if (leftTime == null) {
            return 1;
        }
        if (rightTime == null) {
            return -1;
        }
        return rightTime.compareTo(leftTime);
    }

    private double safe(Double value) {
        return value == null ? 0 : value;
    }

    private double normalized(double value, double min, double max) {
        if (Double.compare(max, min) == 0) {
            return 0;
        }
        return (value - min) / (max - min);
    }

    private double recommendationScore(Diary diary, String interest, DiarySortContext context) {
        double heat = normalized(value(diary.getLikeCount()), context.minLikes(), context.maxLikes());
        double score = normalized(safe(diary.getScore()), context.minScore(), context.maxScore());
        double interestBoost = interestMatches(diary, interest) ? 1.0 : 0.0;
        return heat * 0.6 + score * 0.35 + interestBoost * 0.05;
    }

    private double interestScore(Diary diary, String interest, DiarySortContext context) {
        double heat = normalized(value(diary.getLikeCount()), context.minLikes(), context.maxLikes());
        double score = normalized(safe(diary.getScore()), context.minScore(), context.maxScore());
        double interestBoost = interestMatches(diary, interest) ? 1.0 : 0.0;
        return interestBoost * 0.7 + heat * 0.2 + score * 0.1;
    }

    private boolean interestMatches(Diary diary, String interest) {
        if (interest == null || interest.isBlank()) {
            return false;
        }
        String destinationName = diary.getDestination() == null ? "" : normalize(diary.getDestination().getName());
        String destinationCategory = diary.getDestination() == null ? "" : normalize(diary.getDestination().getCategory());
        String destinationDescription = diary.getDestination() == null ? "" : normalize(diary.getDestination().getDescription());
        String text = String.join(" ",
                        normalize(diary.getTitle()),
                        normalize(diary.getContent()),
                        destinationName,
                        destinationCategory,
                        destinationDescription)
                .toLowerCase(Locale.ROOT);
        for (String token : interestKeywords(interest)) {
            if (!token.isBlank() && text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeInterest(String interest) {
        if (interest == null || interest.isBlank()) {
            return "美食";
        }
        return interest.trim();
    }

    private List<String> interestKeywords(String interest) {
        String normalized = interest == null ? "" : interest.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "自然" -> List.of("自然", "森林", "公园", "山", "湖", "海", "湿地", "风景", "徒步");
            case "历史" -> List.of("历史", "古迹", "文物", "博物馆", "文化", "遗址", "故宫", "寺", "祠");
            case "美食" -> List.of("美食", "小吃", "餐厅", "咖啡", "奶茶", "早餐", "夜市", "味道", "面", "饭");
            case "校园" -> List.of("校园", "大学", "学院", "校区", "学校");
            case "亲子" -> List.of("亲子", "儿童", "乐园", "动物园", "水族馆", "游乐");
            default -> List.of(normalized);
        };
    }

    private double roundScore(double score) {
        return Math.round(score * 10.0) / 10.0;
    }

    private record DiarySortContext(double minLikes, double maxLikes, double minScore, double maxScore) {
        static DiarySortContext from(List<Diary> diaries) {
            double minLikes = diaries.stream().mapToDouble(diary -> diary.getLikeCount() == null ? 0 : diary.getLikeCount()).min().orElse(0);
            double maxLikes = diaries.stream().mapToDouble(diary -> diary.getLikeCount() == null ? 0 : diary.getLikeCount()).max().orElse(0);
            double minScore = diaries.stream().mapToDouble(diary -> diary.getScore() == null ? 0 : diary.getScore()).min().orElse(0);
            double maxScore = diaries.stream().mapToDouble(diary -> diary.getScore() == null ? 0 : diary.getScore()).max().orElse(0);
            return new DiarySortContext(minLikes, maxLikes, minScore, maxScore);
        }
    }
}
