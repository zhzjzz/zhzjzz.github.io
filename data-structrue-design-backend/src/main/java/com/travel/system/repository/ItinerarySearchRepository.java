package com.travel.system.repository;

import com.travel.system.search.ItineraryDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ItinerarySearchRepository extends ElasticsearchRepository<ItineraryDocument, String> {
}
