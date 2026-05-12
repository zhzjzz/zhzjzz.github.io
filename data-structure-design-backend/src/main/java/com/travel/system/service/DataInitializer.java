package com.travel.system.service;

import com.travel.system.mapper.DestinationMapper;
import com.travel.system.mapper.DiaryMapper;
import com.travel.system.mapper.FacilityMapper;
import com.travel.system.mapper.FoodMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.Diary;
import com.travel.system.model.Facility;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    public void run(String... args) {
        ensureDiaryEnhancementSchema();
        ensureFoodSchema();
        Destination bupt = ensureBuptDestination();
        authService.ensureSeedUsers();
        ensureFacilities(bupt);
        ensureFood();
        ensureDiary(bupt);
    }

    private void ensureDiaryEnhancementSchema() {
        Set<String> columns = tableColumns("diary");

        addColumnIfMissing("diary", columns, "media_url", "TEXT");
        addColumnIfMissing("diary", columns, "media_type", "TEXT");
        addColumnIfMissing("diary", columns, "compressed_media_url", "TEXT");
        addColumnIfMissing("diary", columns, "original_size_bytes", "INTEGER DEFAULT 0");
        addColumnIfMissing("diary", columns, "compressed_size_bytes", "INTEGER DEFAULT 0");
        addColumnIfMissing("diary", columns, "compression_status", "TEXT DEFAULT 'pending'");
        addColumnIfMissing("diary", columns, "aigc_animation_url", "TEXT");
        addColumnIfMissing("diary", columns, "aigc_status", "TEXT DEFAULT 'pending'");
        addColumnIfMissing("diary", columns, "heat_score", "REAL DEFAULT 0");
        addColumnIfMissing("diary", columns, "like_count", "INTEGER DEFAULT 0");
        addColumnIfMissing("diary", columns, "favorite_count", "INTEGER DEFAULT 0");
        addColumnIfMissing("diary", columns, "comment_count", "INTEGER DEFAULT 0");
        addColumnIfMissing("diary", columns, "share_count", "INTEGER DEFAULT 0");
        addColumnIfMissing("diary", columns, "is_public", "INTEGER DEFAULT 1");
        addColumnIfMissing("diary", columns, "share_token", "TEXT");
        addColumnIfMissing("diary", columns, "author_name", "TEXT");
        addColumnIfMissing("diary", columns, "published_at", "TEXT");

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

    private void ensureFoodSchema() {
        Set<String> columns = tableColumns("food");
        addColumnIfMissing("food", columns, "heat", "REAL");
    }

    private Set<String> tableColumns(String tableName) {
        return jdbcTemplate.queryForList("PRAGMA table_info(" + tableName + ")")
                .stream()
                .map(column -> String.valueOf(column.get("name")))
                .collect(Collectors.toSet());
    }

    private void addColumnIfMissing(String tableName, Set<String> columns, String columnName, String definition) {
        if (columns.add(columnName)) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private Destination ensureBuptDestination() {
        return destinationMapper.findAll().stream()
                .filter(destination -> "北京邮电大学".equals(destination.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Destination destination = new Destination();
                    destination.setName("北京邮电大学");
                    destination.setSceneType("校园");
                    destination.setCategory("校园");
                    destination.setHeat(8622.0);
                    destination.setRating(3.9);
                    destination.setDescription("信息通信特色高校");
                    destination.setLatitude(39.9652);
                    destination.setLongitude(116.3511);
                    destinationMapper.save(destination);
                    return destination;
                });
    }

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

    private void ensureFood() {
        jdbcTemplate.update("UPDATE food SET heat = 91 WHERE name = ? AND heat IS NULL", "老北京炸酱面");

        if (foodMapper.findAll().size() >= 30) {
            return;
        }

        for (FoodSeed seed : foodSeeds()) {
            Long spotId = spotIdByName(seed.destinationName());
            if (spotId == null || foodExists(seed.name(), seed.storeName())) {
                continue;
            }
            jdbcTemplate.update("""
                            INSERT INTO food (name, cuisine, store_name, rating, heat, destination_id)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    seed.name(),
                    seed.cuisine(),
                    seed.storeName(),
                    seed.rating(),
                    seed.heat(),
                    spotId);
        }
    }

    private boolean foodExists(String name, String storeName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM food WHERE name = ? AND store_name = ?",
                Integer.class,
                name,
                storeName);
        return count != null && count > 0;
    }

    private Long spotIdByName(String destinationName) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT spot_id FROM spots WHERE name = ? ORDER BY spot_id LIMIT 1",
                (rs, rowNum) -> rs.getLong("spot_id"),
                destinationName);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private List<FoodSeed> foodSeeds() {
        return List.of(
                new FoodSeed("老北京炸酱面", "京菜", "校园食堂一层", 4.6, 91d, "北京邮电大学"),
                new FoodSeed("招牌鸡腿饭", "快餐", "学苑餐厅", 4.4, 80d, "北京邮电大学"),
                new FoodSeed("湖畔拿铁", "咖啡", "湖畔咖啡", 4.8, 74d, "北京邮电大学"),
                new FoodSeed("牛肉芝士堡", "西式简餐", "学生活动中心轻食", 4.2, 95d, "北京邮电大学"),
                new FoodSeed("麻辣香锅", "川湘风味", "学生餐厅二层", 4.5, 89d, "北京邮电大学"),
                new FoodSeed("番茄牛腩面", "面食", "校园面馆", 4.3, 77d, "北京邮电大学"),
                new FoodSeed("清华园烤冷面", "小吃", "荷塘小吃档", 4.4, 86d, "清华大学"),
                new FoodSeed("紫荆咖啡", "咖啡", "紫荆咖啡角", 4.7, 81d, "清华大学"),
                new FoodSeed("燕园豆花", "甜品", "未名湖甜品铺", 4.5, 83d, "北京大学"),
                new FoodSeed("未名湖素面", "素食", "燕园素食馆", 4.4, 72d, "北京大学"),
                new FoodSeed("豌豆黄", "京味小吃", "南锣鼓巷小吃铺", 4.5, 85d, "南锣鼓巷"),
                new FoodSeed("糖火烧", "京味小吃", "鼓巷点心铺", 4.3, 79d, "南锣鼓巷"),
                new FoodSeed("胡同酸梅汤", "饮品", "南锣茶饮", 4.6, 88d, "南锣鼓巷"),
                new FoodSeed("桂花酒酿圆子", "甜品", "紫竹院茶点铺", 4.7, 78d, "紫竹院公园"),
                new FoodSeed("荷叶饭", "江南风味", "公园餐厅", 4.4, 82d, "紫竹院公园"),
                new FoodSeed("奥园能量碗", "轻食", "奥园轻食站", 4.4, 84d, "顺义奥林匹克水上公园"),
                new FoodSeed("冠军牛肉饭", "快餐", "水上公园餐吧", 4.3, 87d, "顺义奥林匹克水上公园"),
                new FoodSeed("天坛素斋", "素食", "祈年殿素斋馆", 4.6, 76d, "天坛公园"),
                new FoodSeed("红糖冰粉", "甜品", "天坛小食铺", 4.4, 71d, "天坛公园"),
                new FoodSeed("北海莲子羹", "甜品", "北海茶社", 4.7, 80d, "北海公园"),
                new FoodSeed("宫廷奶酪", "京味小吃", "白塔甜品铺", 4.5, 83d, "北海公园"),
                new FoodSeed("香山栗子糕", "京味小吃", "香山茶点铺", 4.4, 78d, "香山公园"),
                new FoodSeed("山脚热豆浆", "早餐", "香山早餐铺", 4.2, 69d, "香山公园"),
                new FoodSeed("什刹海爆肚", "京菜", "海子边小馆", 4.6, 90d, "什刹海风景区"),
                new FoodSeed("胡同烤串", "烧烤", "银锭桥串吧", 4.5, 92d, "什刹海风景区"),
                new FoodSeed("恭王府杏仁豆腐", "甜品", "王府茶歇", 4.6, 82d, "恭王府"),
                new FoodSeed("雍和宫素面", "素食", "雍和素面馆", 4.5, 80d, "雍和宫"),
                new FoodSeed("五道营手冲", "咖啡", "胡同手冲店", 4.8, 86d, "五道营胡同"),
                new FoodSeed("大栅栏卤煮", "京菜", "前门老味道", 4.4, 90d, "大栅栏"),
                new FoodSeed("观复文创拿铁", "咖啡", "观复咖啡", 4.6, 77d, "观复博物馆"),
                new FoodSeed("航空主题餐", "快餐", "航空博物馆餐吧", 4.2, 84d, "中国航空博物馆"),
                new FoodSeed("牛街清真小吃", "清真菜", "牛街小吃铺", 4.7, 94d, "牛街清真寺"),
                new FoodSeed("石林峡山野面", "面食", "峡谷餐厅", 4.3, 83d, "石林峡"),
                new FoodSeed("云蒙山栗子鸡", "农家菜", "云蒙山农家院", 4.5, 87d, "云蒙山国家森林公园"),
                new FoodSeed("慕田峪驴打滚", "京味小吃", "长城脚下茶铺", 4.4, 82d, "慕田峪长城"),
                new FoodSeed("城市绿心轻食", "轻食", "绿心森林餐吧", 4.3, 79d, "城市绿心森林公园")
        );
    }

    private void ensureDiary(Destination bupt) {
        if (!diaryMapper.findAll().isEmpty()) {
            return;
        }

        Diary diary = new Diary();
        diary.setTitle("北邮春日打卡");
        diary.setContent("主楼、图书馆和银杏大道都很适合拍照，也可以顺路去食堂尝尝校园风味。");
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

    private record FoodSeed(String name,
                            String cuisine,
                            String storeName,
                            Double rating,
                            Double heat,
                            String destinationName) {
    }
}
