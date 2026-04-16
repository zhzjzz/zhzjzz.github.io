package com.travel.system.repository;

import com.travel.system.search.FacilityDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
/*
  Elasticsearch Repository for {@link FacilityDocument}.
 */
public interface FacilitySearchRepository extends ElasticsearchRepository<FacilityDocument, String> {
    /**
     * 根据名称、设施类型或目的地名称进行模糊搜索
     */
    List<FacilityDocument> findByNameContainingOrFacilityTypeContainingOrDestinationNameContaining(String name, String facilityType, String destinationName);
}
