package com.travel.system.repository;

import com.travel.system.search.DestinationDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Elasticsearch Repository for {@link DestinationDocument}.
 */
@Repository
public interface DestinationSearchRepository extends ElasticsearchRepository<DestinationDocument, String> {
    /**
     * 根据名称或类别进行模糊搜索
     */
    List<DestinationDocument> findByNameContainingOrCategoryContaining(String name, String category);
}
