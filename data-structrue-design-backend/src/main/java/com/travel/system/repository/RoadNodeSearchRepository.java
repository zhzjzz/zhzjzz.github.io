package com.travel.system.repository;

import com.travel.system.search.RoadNodeDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RoadNodeSearchRepository extends ElasticsearchRepository<RoadNodeDocument, String> {
}
