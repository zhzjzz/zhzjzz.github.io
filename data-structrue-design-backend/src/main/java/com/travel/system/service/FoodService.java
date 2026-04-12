package com.travel.system.service;

import com.github.pagehelper.PageHelper;
import com.travel.system.model.Food;
import com.travel.system.mapper.FoodMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * {@code FoodService} 负责封装美食（Food）相关的业务逻辑。
 *
 * <p>主要功能包括：
 *
 * <ul>
 *   <li>分页搜索美食（名称、菜系、店名模糊匹配）；</li>
 *   <li>Top‑K 推荐计算（委托 {@link RecommendationService}）；</li>
 *   <li>新增、更新美食记录。</li>
 * </ul>
 *
 * @author 自动生成
 */
@Service
public class FoodService {

private final FoodMapper foodRepository;
    private final RecommendationService recommendationService;

public FoodService(FoodMapper foodRepository,
                       RecommendationService recommendationService) {
this.foodRepository = foodRepository;
        this.recommendationService = recommendationService;
    }

    /**
     * 分页搜索美食。
     *
     * @param keyword 可选的搜索关键字；若为 {@code null} 或空字符串，则返回全部美食
     * @param page    页码（从 1 开始）
     * @param size    每页记录数
     * @return 当前页的 {@link Food} 列表
     */
    public List<Food> search(String keyword, int page, int size) {
        PageHelper.startPage(page <= 0 ? 1 : page, size <= 0 ? 10 : size);
        if (keyword == null || keyword.isBlank()) {
            return foodRepository.findAll();
        }
        // 使用统一的关键字搜索方法
        return foodRepository.findByKeyword(keyword);
    }

    /**
     * Top‑K 推荐美食。
     *
     * @param k 返回数量
     * @return 按热度/评分综合排序的美食列表
     */
    public List<Food> topK(int k) {
        List<Food> all = foodRepository.findAll();
        return recommendationService.topKFood(all, k);
    }
}
