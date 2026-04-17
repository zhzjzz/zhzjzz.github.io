package com.travel.system.repository;

import com.travel.system.search.UserAccountDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserAccountSearchRepository extends ElasticsearchRepository<UserAccountDocument, String> {
}
