package com.travel.system.service;

import com.travel.system.model.*;
import com.travel.system.mapper.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DestinationMapper destinationMapper;
    private final FoodMapper foodMapper;
    private final FacilityMapper facilityMapper;
    private final DiaryMapper diaryMapper;
    private final AuthService authService;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(DestinationMapper destinationMapper,
                           FoodMapper foodMapper,
                           FacilityMapper facilityMapper,
                           DiaryMapper diaryMapper,
                           AuthService authService,
                           JdbcTemplate jdbcTemplate) {
        this.destinationMapper = destinationMapper;
        this.foodMapper = foodMapper;
        this.facilityMapper = facilityMapper;
        this.diaryMapper = diaryMapper;
        this.authService = authService;
        this.jdbcTemplate = jdbcTemplate;
    }
    /**
     * 应用启动后执行数据初始化流程，确保演示所需的基础目的地、设施、美食和游记数据存在。
     */

    @Override
    public void run(String... args) {
        ensureDiaryEnhancementSchema();
        Destination bupt = ensureBuptDestination();
        authService.ensureSeedUsers();
        ensureFacilities(bupt);
        ensureFood(bupt);
        ensureDiary(bupt);
    }

    /**
     * 补齐老版本 SQLite 数据库中缺失的游记增强字段。
     */
    private void ensureDiaryEnhancementSchema() {
        Set<String> columns = jdbcTemplate.queryForList("PRAGMA table_info(diary)")
                .stream()
                .map(column -> String.valueOf(column.get("name")))
                .collect(Collectors.toSet());

        addDiaryColumnIfMissing(columns, "media_url", "TEXT");
        addDiaryColumnIfMissing(columns, "media_type", "TEXT");
        addDiaryColumnIfMissing(columns, "compressed_media_url", "TEXT");
        addDiaryColumnIfMissing(columns, "original_size_bytes", "INTEGER DEFAULT 0");
        addDiaryColumnIfMissing(columns, "compressed_size_bytes", "INTEGER DEFAULT 0");
        addDiaryColumnIfMissing(columns, "compression_status", "TEXT DEFAULT 'pending'");
        addDiaryColumnIfMissing(columns, "aigc_animation_url", "TEXT");
        addDiaryColumnIfMissing(columns, "aigc_status", "TEXT DEFAULT 'pending'");
        addDiaryColumnIfMissing(columns, "heat_score", "REAL DEFAULT 0");
        addDiaryColumnIfMissing(columns, "like_count", "INTEGER DEFAULT 0");
        addDiaryColumnIfMissing(columns, "favorite_count", "INTEGER DEFAULT 0");
        addDiaryColumnIfMissing(columns, "comment_count", "INTEGER DEFAULT 0");
        addDiaryColumnIfMissing(columns, "share_count", "INTEGER DEFAULT 0");
        addDiaryColumnIfMissing(columns, "is_public", "INTEGER DEFAULT 1");
        addDiaryColumnIfMissing(columns, "share_token", "TEXT");
        addDiaryColumnIfMissing(columns, "author_name", "TEXT");
        addDiaryColumnIfMissing(columns, "published_at", "TEXT");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS diary_comment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    diary_id INTEGER NOT NULL,
                    author_name TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
                """);
    }

    private void addDiaryColumnIfMissing(Set<String> columns, String columnName, String definition) {
        if (columns.add(columnName)) {
            jdbcTemplate.execute("ALTER TABLE diary ADD COLUMN " + columnName + " " + definition);
        }
    }

    /**

     * 检查并补齐北京邮电大学相关目的地数据，避免空库启动后首页和推荐接口没有基础数据。

     */
    private Destination ensureBuptDestination() {
        if (destinationMapper.findAll().size() > 0) {
            return destinationMapper.findAll().stream()
                    .filter(destination -> "北京邮电大学".equals(destination.getName()))
                    .findFirst()
                    .orElseGet(() -> {
                        Destination destination = new Destination();
                        destination.setName("北京邮电大学");
                        destination.setSceneType("校园");
                        destination.setCategory("理工类高校");
                        destination.setHeat(5.0);
                        destination.setRating(4.8);
                        destination.setDescription("信息通信特色高校");
                        destination.setLatitude(39.9652);
                        destination.setLongitude(116.3511);
                        destinationMapper.save(destination);
                        return destination;
                    });
        }

        Destination destination = new Destination();
        destination.setName("北京邮电大学");
        destination.setSceneType("校园");
        destination.setCategory("理工类高校");
        destination.setHeat(5.0);
        destination.setRating(4.8);
        destination.setDescription("信息通信特色高校");
        destination.setLatitude(39.9652);
        destination.setLongitude(116.3511);
        destinationMapper.save(destination);
        return destination;
    }

    /**

     * 检查并补齐博物馆示例目的地数据，用于推荐和搜索功能的基础展示。

     */
    private Destination ensureMuseumDestination() {
        return destinationMapper.findAll().stream()
                .filter(destination -> "国家博物馆".equals(destination.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Destination destination = new Destination();
                    destination.setName("国家博物馆");
                    destination.setSceneType("景区");
                    destination.setCategory("5A");
                    destination.setHeat(4.7);
                    destination.setRating(4.9);
                    destination.setDescription("综合性博物馆");
                    destination.setLatitude(39.9050);
                    destination.setLongitude(116.3976);
                    destinationMapper.save(destination);
                    return destination;
                });
    }

    /**

     * 为指定目的地补齐基础设施数据；已有数据时跳过，避免重复插入。

     */
    private void ensureFacilities(Destination bupt) {
        if (!facilityMapper.findAll().isEmpty()) {
            return;
        }

        Facility wc = new Facility();
        wc.setName("主楼卫生间");
        wc.setFacilityType("洗手间");
        wc.setDestination(bupt);
        wc.setLatitude(39.9650);
        wc.setLongitude(116.3510);
        facilityMapper.insert(wc);

        Facility cafe = new Facility();
        cafe.setName("校园咖啡角");
        cafe.setFacilityType("咖啡馆");
        cafe.setDestination(bupt);
        cafe.setLatitude(39.9653);
        cafe.setLongitude(116.3514);
        facilityMapper.insert(cafe);

        Facility canteen = new Facility();
        canteen.setName("学生食堂");
        canteen.setFacilityType("食堂");
        canteen.setDestination(bupt);
        canteen.setLatitude(39.9648);
        canteen.setLongitude(116.3508);
        facilityMapper.insert(canteen);
    }

    /**

     * 为指定目的地补齐美食数据；已有数据时跳过，保持初始化过程幂等。

     */
    private void ensureFood(Destination bupt) {
        if (!foodMapper.findAll().isEmpty()) {
            return;
        }

        Food food = new Food();
        food.setName("老北京炸酱面");
        food.setCuisine("京菜");
        food.setStoreName("校园食堂一层");
        food.setRating(4.6);
        food.setDestination(bupt);
        foodMapper.insert(food);
    }

    /**

     * 为指定目的地补齐游记数据；已有数据时跳过，保证重复启动不会产生重复记录。

     */
    private void ensureDiary(Destination bupt) {
        if (!diaryMapper.findAll().isEmpty()) {
            return;
        }

        Diary diary = new Diary();
        diary.setTitle("北邮春日打卡");
        diary.setContent("主楼、图书馆、银杏大道都很值得拍照。");
        diary.setMediaType("image");
        diary.setCompressionStatus("none");
        diary.setOriginalSizeBytes(0L);
        diary.setCompressedSizeBytes(0L);
        diary.setAigcAnimationUrl("/demo/aigc/diary-bupt-spring.mp4");
        diary.setAigcStatus("generated");
        diary.setHeatScore(134.0);
        diary.setLikeCount(8L);
        diary.setFavoriteCount(5L);
        diary.setCommentCount(0L);
        diary.setShareCount(2L);
        diary.setIsPublic(true);
        diary.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        diary.setAuthorName("演示用户");
        diary.setScore(4.7);
        diary.setViews(120L);
        diary.setPublishedAt(LocalDateTime.now());
        diary.setDestination(bupt);
        diaryMapper.insert(diary);
    }
}
