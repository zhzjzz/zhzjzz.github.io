package com.travel.system.service;

import com.travel.system.model.*;
import com.travel.system.mapper.*;
import com.travel.system.repository.DestinationSearchRepository;
import com.travel.system.repository.DiarySearchRepository;
import com.travel.system.repository.FacilitySearchRepository;
import com.travel.system.repository.FoodSearchRepository;
import com.travel.system.search.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据初始化器
 * <p>
 * 负责在应用启动时初始化系统基础数据，包括：
 * <ul>
 *     <li>景点/目的地数据（如北京邮电大学）</li>
 *     <li>用户种子数据（演示账号）</li>
 *     <li>设施数据（卫生间、咖啡馆、食堂等）</li>
 *     <li>美食数据</li>
 *     <li>用户日记数据</li>
 *     <li>道路网络图数据（节点和边）</li>
 *     <li>Elasticsearch 索引同步</li>
 * </ul>
 * </p>
 * <p>
 * 注意：该类目前被注释掉（@Component 被注释），如需启用数据初始化功能请取消注释
 * </p>
 *
 * @author Travel System Team
 * @since 1.0.0
 * @see CommandLineRunner  Spring Boot 启动后自动执行
 */
//@Component
public class DataInitializer implements CommandLineRunner {

    /**
     * 目的地数据访问映射器
     */
    private final DestinationMapper destinationMapper;
    
    /**
     * 美食数据访问映射器
     */
    private final FoodMapper foodMapper;
    
    /**
     * 设施数据访问映射器
     */
    private final FacilityMapper facilityMapper;
    
    /**
     * 日记数据访问映射器
     */
    private final DiaryMapper diaryMapper;
    
    /**
     * 道路节点数据访问映射器
     */
    private final RoadNodeMapper roadNodeMapper;
    
    /**
     * 道路边数据访问映射器
     */
    private final RoadEdgeMapper roadEdgeMapper;
    
    /**
     * 认证服务，用于创建种子用户
     */
    private final AuthService authService;
    
    // Elasticsearch 搜索仓库（可选依赖）
    
    /**
     * 目的地搜索仓库（ES）
     */
    private final DestinationSearchRepository destinationSearchRepository;
    
    /**
     * 美食搜索仓库（ES）
     */
    private final FoodSearchRepository foodSearchRepository;
    
    /**
     * 设施搜索仓库（ES）
     */
    private final FacilitySearchRepository facilitySearchRepository;
    
    /**
     * 日记搜索仓库（ES）
     */
    private final DiarySearchRepository diarySearchRepository;

    /**
     * 构造函数，注入所有依赖
     * <p>
     * ES 仓库使用 ObjectProvider 包装以便在 ES 不可用时优雅降级
     * </p>
     *
     * @param destinationMapper 目的地映射器
     * @param foodMapper 美食映射器
     * @param facilityMapper 设施映射器
     * @param diaryMapper 日记映射器
     * @param roadNodeMapper 道路节点映射器
     * @param roadEdgeMapper 道路边映射器
     * @param authService 认证服务
     * @param destinationSearchRepositoryProvider 目的地搜索仓库提供者
     * @param foodSearchRepositoryProvider 美食搜索仓库提供者
     * @param facilitySearchRepositoryProvider 设施搜索仓库提供者
     * @param diarySearchRepositoryProvider 日记搜索仓库提供者
     */
    public DataInitializer(DestinationMapper destinationMapper,
                           FoodMapper foodMapper,
                           FacilityMapper facilityMapper,
                           DiaryMapper diaryMapper,
                           RoadNodeMapper roadNodeMapper,
                           RoadEdgeMapper roadEdgeMapper,
                           AuthService authService,
                           ObjectProvider<DestinationSearchRepository> destinationSearchRepositoryProvider,
                           ObjectProvider<FoodSearchRepository> foodSearchRepositoryProvider,
                           ObjectProvider<FacilitySearchRepository> facilitySearchRepositoryProvider,
                           ObjectProvider<DiarySearchRepository> diarySearchRepositoryProvider) {
        this.destinationMapper = destinationMapper;
        this.foodMapper = foodMapper;
        this.facilityMapper = facilityMapper;
        this.diaryMapper = diaryMapper;
        this.roadNodeMapper = roadNodeMapper;
        this.roadEdgeMapper = roadEdgeMapper;
        this.authService = authService;
        this.destinationSearchRepository = destinationSearchRepositoryProvider.getIfAvailable();
        this.foodSearchRepository = foodSearchRepositoryProvider.getIfAvailable();
        this.facilitySearchRepository = facilitySearchRepositoryProvider.getIfAvailable();
        this.diarySearchRepository = diarySearchRepositoryProvider.getIfAvailable();
    }

    /**
     * 数据初始化入口方法
     * <p>
     * 在 Spring Boot 应用启动完成后自动执行，按顺序初始化：
     * <ol>
     *     <li>学校目的地数据</li>
     *     <li>种子用户数据</li>
     *     <li>校园设施数据</li>
     *     <li>校园美食数据</li>
     *     <li>用户日记数据</li>
     *     <li>道路网络图数据</li>
     *     <li>Elasticsearch 数据同步</li>
     * </ol>
     * </p>
     *
     * @param args 命令行参数
     */
    @Override
    public void run(String... args) {
        Destination bupt = ensureBuptDestination();
        // Destination museum = ensureMuseumDestination();

        authService.ensureSeedUsers();
        ensureFacilities(bupt);
        ensureFood(bupt);
        ensureDiary(bupt);
        ensureRoadGraph();
        
        // 同步数据到 Elasticsearch
        syncDataToElasticsearch();
    }
    
    /**
     * 将现有数据全量同步到 Elasticsearch
     * <p>
     * 遍历所有数据库表，将数据转换为对应的 ES 文档并保存，
     * 便于后续全文搜索功能使用
     * </p>
     */
    private void syncDataToElasticsearch() {
        syncDestinations();
        syncFoods();
        syncFacilities();
        syncDiaries();
    }
    
    /**
     * 同步目的地数据到 Elasticsearch
     * <p>
     * 从数据库读取所有目的地，转换为 DestinationDocument 后保存到 ES
     * </p>
     */
    private void syncDestinations() {
        if (destinationSearchRepository == null) return;
        try {
            List<Destination> destinations = destinationMapper.findAll();
            for (Destination dest : destinations) {
                DestinationDocument doc = new DestinationDocument();
                doc.setId(String.valueOf(dest.getId()));
                doc.setName(dest.getName());
                doc.setSceneType(dest.getSceneType());
                doc.setCategory(dest.getCategory());
                doc.setHeat(dest.getHeat());
                doc.setRating(dest.getRating());
                doc.setDescription(dest.getDescription());
                doc.setLatitude(dest.getLatitude());
                doc.setLongitude(dest.getLongitude());
                destinationSearchRepository.save(doc);
            }
        } catch (Exception ignored) {
        }
    }
    
    /**
     * 同步美食数据到 Elasticsearch
     * <p>
     * 从数据库读取所有美食记录，转换为 FoodDocument 后保存到 ES
     * </p>
     */
    private void syncFoods() {
        if (foodSearchRepository == null) return;
        try {
            List<Food> foods = foodMapper.findAll();
            for (Food food : foods) {
                FoodDocument doc = new FoodDocument();
                doc.setId(String.valueOf(food.getId()));
                doc.setName(food.getName());
                doc.setCuisine(food.getCuisine());
                doc.setStoreName(food.getStoreName());
                doc.setHeat(food.getHeat());
                doc.setRating(food.getRating());
                if (food.getDestination() != null) {
                    doc.setDestinationName(food.getDestination().getName());
                }
                foodSearchRepository.save(doc);
            }
        } catch (Exception ignored) {
        }
    }
    
    /**
     * 同步设施数据到 Elasticsearch
     * <p>
     * 从数据库读取所有设施记录，转换为 FacilityDocument 后保存到 ES
     * </p>
     */
    private void syncFacilities() {
        if (facilitySearchRepository == null) return;
        try {
            List<Facility> facilities = facilityMapper.findAll();
            for (Facility facility : facilities) {
                FacilityDocument doc = new FacilityDocument();
                doc.setId(String.valueOf(facility.getId()));
                doc.setName(facility.getName());
                doc.setFacilityType(facility.getFacilityType());
                doc.setLatitude(facility.getLatitude());
                doc.setLongitude(facility.getLongitude());
                if (facility.getDestination() != null) {
                    doc.setDestinationName(facility.getDestination().getName());
                }
                facilitySearchRepository.save(doc);
            }
        } catch (Exception ignored) {
        }
    }
    
    /**
     * 同步日记数据到 Elasticsearch
     * <p>
     * 从数据库读取所有日记记录，转换为 DiaryDocument 后保存到 ES
     * </p>
     */
    private void syncDiaries() {
        if (diarySearchRepository == null) return;
        try {
            List<Diary> diaries = diaryMapper.findAll();
            for (Diary diary : diaries) {
                DiaryDocument doc = new DiaryDocument();
                doc.setId(String.valueOf(diary.getId()));
                doc.setTitle(diary.getTitle());
                doc.setContent(diary.getContent());
                if (diary.getDestination() != null) {
                    doc.setDestinationName(diary.getDestination().getName());
                }
                diarySearchRepository.save(doc);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 确保北京邮电大学目的地数据存在
     * <p>
     * 查询数据库，如果不存在则创建"北京邮电大学"作为默认目的地
     * </p>
     *
     * @return 北京邮电大学目的地对象（新建或已存在）
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
     * 确保国家博物馆目的地数据存在
     * <p>
     * 查询数据库，如果不存在则创建"国家博物馆"
     * </p>
     *
     * @return 国家博物馆目的地对象（新建或已存在）
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
     * 确保校园设施数据存在
     * <p>
     * 如果设施表为空，则创建默认设施：
     * <ul>
     *     <li>主楼卫生间</li>
     *     <li>校园咖啡角</li>
     *     <li>学生食堂</li>
     * </ul>
     * </p>
     *
     * @param bupt 关联的目的地（北京邮电大学）
     */
    private void ensureFacilities(Destination bupt) {
        if (!facilityMapper.findAll().isEmpty()) {
            return;
        }

        // 创建卫生间设施
        Facility wc = new Facility();
        wc.setName("主楼卫生间");
        wc.setFacilityType("洗手间");
        wc.setDestination(bupt);
        wc.setLatitude(39.9650);
        wc.setLongitude(116.3510);
        facilityMapper.insert(wc);

        // 创建咖啡馆设施
        Facility cafe = new Facility();
        cafe.setName("校园咖啡角");
        cafe.setFacilityType("咖啡馆");
        cafe.setDestination(bupt);
        cafe.setLatitude(39.9653);
        cafe.setLongitude(116.3514);
        facilityMapper.insert(cafe);

        // 创建食堂设施
        Facility canteen = new Facility();
        canteen.setName("学生食堂");
        canteen.setFacilityType("食堂");
        canteen.setDestination(bupt);
        canteen.setLatitude(39.9648);
        canteen.setLongitude(116.3508);
        facilityMapper.insert(canteen);
    }

    /**
     * 确保校园美食数据存在
     * <p>
     * 如果美食表为空，则创建默认美食数据（老北京炸酱面）
     * </p>
     *
     * @param bupt 关联的目的地（北京邮电大学）
     */
    private void ensureFood(Destination bupt) {
        if (!foodMapper.findAll().isEmpty()) {
            return;
        }

        Food food = new Food();
        food.setName("老北京炸酱面");
        food.setCuisine("京菜");
        food.setStoreName("校园食堂一层");
        food.setHeat(4.5);
        food.setRating(4.6);
        food.setDistanceMeters(350.0);
        food.setDestination(bupt);
        foodMapper.insert(food);
    }

    /**
     * 确保用户日记数据存在
     * <p>
     * 如果日记表为空，则创建默认日记数据（北邮春日打卡）
     * </p>
     *
     * @param bupt 关联的目的地（北京邮电大学）
     */
    private void ensureDiary(Destination bupt) {
        if (!diaryMapper.findAll().isEmpty()) {
            return;
        }

        Diary diary = new Diary();
        diary.setTitle("北邮春日打卡");
        diary.setContent("主楼、图书馆、银杏大道都很值得拍照。");
        diary.setMediaType("image");
        diary.setScore(4.7);
        diary.setViews(120L);
        diary.setPublishedAt(LocalDateTime.now());
        diary.setDestination(bupt);
        diaryMapper.insert(diary);
    }

    /**
     * 确保道路网络图数据存在
     * <p>
     * 创建校园道路网络的基础节点和边：
     * <ul>
     *     <li>北门、主楼、图书馆 三个节点</li>
     *     <li>北门-主楼、主楼-图书馆、北门-图书馆 三条双向边</li>
     * </ul>
     * 支持步行、自行车、班车等交通方式
     * </p>
     */
    private void ensureRoadGraph() {
        if (!roadNodeMapper.findAll().isEmpty()) {
            return;
        }

        // 创建北门节点
        RoadNode gate = new RoadNode();
        gate.setName("北门");
        gate.setNodeType("入口");
        gate.setLatitude(39.9656);
        gate.setLongitude(116.3505);
        roadNodeMapper.insert(gate);

        // 创建主楼节点
        RoadNode mainBuilding = new RoadNode();
        mainBuilding.setName("主楼");
        mainBuilding.setNodeType("建筑");
        mainBuilding.setLatitude(39.9651);
        mainBuilding.setLongitude(116.3510);
        roadNodeMapper.insert(mainBuilding);

        // 创建图书馆节点
        RoadNode library = new RoadNode();
        library.setName("图书馆");
        library.setNodeType("建筑");
        library.setLatitude(39.9649);
        library.setLongitude(116.3515);
        roadNodeMapper.insert(library);

        // 创建双向边连接各节点
        createBidirectionalEdge(gate, mainBuilding, 280.0, 75.0, 0.9, "walk,bike,shuttle");
        createBidirectionalEdge(mainBuilding, library, 220.0, 70.0, 0.8, "walk,bike,shuttle");
        createBidirectionalEdge(gate, library, 500.0, 68.0, 0.7, "walk,bike");
    }

    /**
     * 创建双向道路边
     * <p>
     * 在道路网络中，大多数道路都是双向通行的。
     * 此方法会创建两条 RoadEdge 记录，分别表示 from→to 和 to→from
     * </p>
     *
     * @param from            起点节点
     * @param to              终点节点
     * @param distance        距离（米）
     * @param idealSpeed      理想速度（米/分钟）
     * @param congestion      拥堵系数（0.0-1.0，值越大越畅通）
     * @param allowedTransport 允许的交通工具类型（逗号分隔），如"walk,bike,car"
     */
    private void createBidirectionalEdge(RoadNode from,
                                         RoadNode to,
                                         double distance,
                                         double idealSpeed,
                                         double congestion,
                                         String allowedTransport) {
        // 正向边 from → to
        RoadEdge forward = new RoadEdge();
        forward.setFromNode(from);
        forward.setToNode(to);
        forward.setDistanceMeters(distance);
        forward.setIdealSpeed(idealSpeed);
        forward.setCongestion(congestion);
        forward.setAllowedTransport(allowedTransport);
        roadEdgeMapper.insert(forward);

        // 反向边 to → from
        RoadEdge backward = new RoadEdge();
        backward.setFromNode(to);
        backward.setToNode(from);
        backward.setDistanceMeters(distance);
        backward.setIdealSpeed(idealSpeed);
        backward.setCongestion(congestion);
        backward.setAllowedTransport(allowedTransport);
        roadEdgeMapper.insert(backward);
    }
}
