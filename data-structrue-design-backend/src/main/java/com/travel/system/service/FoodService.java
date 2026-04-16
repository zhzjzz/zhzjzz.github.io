package com.travel.system.service;

import com.github.pagehelper.PageHelper;
import com.travel.system.model.Food;
import com.travel.system.mapper.FoodMapper;
import com.travel.system.repository.FoodSearchRepository;
import com.travel.system.search.FoodDocument;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
    private final FoodSearchRepository foodSearchRepository;
    private final RecommendationService recommendationService;

    public FoodService(FoodMapper foodRepository,
                       ObjectProvider<FoodSearchRepository> foodSearchRepositoryProvider,
                       RecommendationService recommendationService) {
        this.foodRepository = foodRepository;
        this.foodSearchRepository = foodSearchRepositoryProvider.getIfAvailable();
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
        
        // 优先使用 Elasticsearch 进行模糊搜索
        if (foodSearchRepository != null) {
            try {
                List<FoodDocument> docs = foodSearchRepository
                    .findByNameContainingOrCuisineContainingOrStoreNameContaining(keyword, keyword, keyword);
                return docs.stream().map(this::toFood).collect(Collectors.toList());
            } catch (Exception e) {
                // ES 搜索失败时回退到 MySQL
            }
        }
        
        // 使用 MySQL 进行模糊搜索
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
    
    /**
     * 保存美食记录并同步到 Elasticsearch
     */
    public Food save(Food food) {
        Food saved = foodRepository.save(food);
        if (foodSearchRepository != null) {
            try {
                FoodDocument doc = toDocument(saved);
                foodSearchRepository.save(doc);
            } catch (Exception ignored) {
            }
        }
        return saved;
    }
    
    /**
     * FoodDocument 转换为 Food
     */
    private Food toFood(FoodDocument doc) {
        Food food = new Food();
        food.setId(Long.valueOf(doc.getId()));
        food.setName(doc.getName());
        food.setCuisine(doc.getCuisine());
        food.setStoreName(doc.getStoreName());
        food.setHeat(doc.getHeat());
        food.setRating(doc.getRating());
        return food;
    }
    
    /**
     * Food 转换为 FoodDocument
     */
    private FoodDocument toDocument(Food food) {
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
        return doc;
    }
}
