package com.travel.system.repository;

import com.travel.system.search.RoadEdgeDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RoadEdgeSearchRepository extends ElasticsearchRepository<RoadEdgeDocument, String> {
}
