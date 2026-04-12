package com.travel.system.service;

import com.github.pagehelper.PageHelper;
import com.travel.system.mapper.DestinationMapper;
import com.travel.system.model.Destination;
import com.travel.system.mapper.DestinationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@code DestinationService} 负责封装目的地相关的业务逻辑。
 *
 * <p>核心职责：
 *
 * <ul>
 *   <li>分页查询与关键字搜索；</li>
 *   <li>Top‑K 推荐结果的获取与缓存（后续可扩展）；</li>
 *   <li>目的地新增/更新前的业务校验。</li>
 * </ul>
 *
 * <p>控制器层不再直接调用 {@link DestinationRepository}，而是统一通过 Service 层进行交互，
 * 便于后续增加日志、权限、缓存等横切关注点。
 *
 * @author 自动生成
 */
@Service
public class DestinationService {

    /** 目的地持久层（MyBatis Mapper）。 */
    private final DestinationMapper destinationMapper;

    /** 推荐服务，用于计算热度/评分综合排序的 Top‑K 结果。 */
    private final RecommendationService recommendationService;

    public DestinationService(DestinationMapper destinationMapper,
                              RecommendationService recommendationService) {
        this.destinationMapper = destinationMapper;
        this.recommendationService = recommendationService;
    }

    /**
     * 分页查询目的地列表并支持关键字模糊搜索。
     *
     * @param keyword 可选的搜索关键字；若为 {@code null} 或空字符串，则返回全部目的地
     * @param page    页码（从 1 开始），若小于 1 则自动纠正为 1
     * @param size    每页记录数，若小于等于 0 则使用默认值 10
     * @return 当前页的 {@link Destination} 列表
     */
    public List<Destination> list(String keyword, int page, int size) {
        // 使用 PageHelper 设置分页参数，仅对紧随其后的查询生效
        PageHelper.startPage(page <= 0 ? 1 : page, size <= 0 ? 10 : size);
        if (keyword == null || keyword.isBlank()) {
            return destinationMapper.findAll();
        }
        return destinationMapper.findByKeyword(keyword);
    }

    /**
     * 返回热度+评分综合排序的前 {@code k} 名目的地。
     *
     * <p>先从数据库获取全部记录，再交由 {@link RecommendationService} 计算 Top‑K。
     * 若后续数据量增大，可改为在 SQL 层面进行预排序与限制。
     *
     * @param k 想要返回的目的地数量
     * @return 已排序的 {@link Destination} 列表
     */
    public List<Destination> topK(int k) {
        List<Destination> all = destinationMapper.findAll();
        return recommendationService.topKDestinations(all, k);
    }

    /**
     * 保存目的地实体（新增或更新）。
     *
     * @param destination 前端提交的目的地实体
     * @return 保存后的实体（含数据库生成的主键）
     */
    public Destination save(Destination destination) {
        destinationMapper.save(destination);
        return destination;
    }
}
