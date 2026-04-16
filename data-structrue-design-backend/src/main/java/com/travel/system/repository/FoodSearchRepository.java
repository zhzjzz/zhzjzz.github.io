package com.travel.system.repository;

import com.travel.system.search.FoodDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch Repository for {@link FoodDocument}.
 */
@Repository
public interface FoodSearchRepository extends ElasticsearchRepository<FoodDocument, String> {
    /**
     * 根据名称、菜系或店名进行模糊搜索
     */
    List<FoodDocument> findByNameContainingOrCuisineContainingOrStoreNameContaining(String name, String cuisine, String storeName);
}
