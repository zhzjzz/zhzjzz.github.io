package com.travel.system.repository;

import com.travel.system.search.DiaryDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface DiarySearchRepository extends ElasticsearchRepository<DiaryDocument, String> {
    List<DiaryDocument> findByTitleContainingOrContentContaining(String title, String content);
}
