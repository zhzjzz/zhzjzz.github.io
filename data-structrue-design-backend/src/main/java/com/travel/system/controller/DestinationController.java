package com.travel.system.controller;

import com.travel.system.model.Destination;
import com.travel.system.mapper.DestinationRepository;
import com.travel.system.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code DestinationController} 负责处理与景区/校园目的地相关的 HTTP 请求。
 *
 * <p>提供以下功能：
 *
 * <ul>
 *   <li>查询目的地列表并支持关键字模糊搜索；</li>
 *   <li>基于热度和评分的 Top‑K 推荐接口；</li>
 *   <li>新增目的地数据的持久化。</li>
 * </ul>
 *
 * 该控制器使用 Spring MVC 注解实现 RESTful 风格的 API，所有路径统一以 {@code /api/destinations}
 * 为前缀。
 *
 * @author 自动生成
 */
@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    /** 持久化目的地的 JPA 仓库。 */
    private final DestinationRepository destinationRepository;

    /** 用于业务层推荐逻辑的服务。 */
    private final RecommendationService recommendationService;

    /**
     * 构造函数注入所需的依赖。
     *
     * @param destinationRepository 目的地数据访问层
     * @param recommendationService  业务层推荐服务
     */
    public DestinationController(DestinationRepository destinationRepository,
                                 RecommendationService recommendationService) {
        this.destinationRepository = destinationRepository;
        this.recommendationService = recommendationService;
    }

    /**
     * 查询目的地列表。
     *
     * @param keyword 可选的搜索关键字；若为 {@code null} 或空字符串，则返回全部目的地
     * @return 符合条件的 {@link Destination} 列表
     */
    @GetMapping
    public List<Destination> list(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.isBlank()) {
            // 没有提供关键字，返回全部记录
            return destinationRepository.findAll();
        }
        // 使用关键字在名称或类别字段执行模糊匹配（不区分大小写）
        return destinationRepository.findByKeyword(keyword);
    }

    /**
     * 返回热度+评分综合排序的前 {@code k} 名目的地。
     *
     * @param k 想要返回的目的地数量，默认值为 10
     * @return 已排序的 {@link Destination} 列表
     */
    @GetMapping("/top")
    public List<Destination> top(@RequestParam(defaultValue = "10") int k) {
        // 先获取全部目的地，再交由推荐服务进行排序与截取
        return recommendationService.topKDestinations(destinationRepository.findAll(), k);
    }

    /**
     * 新增目的地记录。
     *
     * @param destination 前端提交的目的地实体（JSON → {@link Destination}）
     * @return 保存后的实体，包含数据库生成的主键等信息
     */
    @PostMapping
    public Destination create(@RequestBody Destination destination) {
        // 直接使用 JPA 仓库的 save 方法完成持久化
        return destinationRepository.save(destination);
    }
}
