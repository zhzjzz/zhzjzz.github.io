package com.travel.system.service;

import com.travel.system.model.Diary;
import com.travel.system.mapper.DiarySearchRepository;
import com.travel.system.mapper.DiaryMapper;
import com.travel.system.search.DiaryDocument;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiaryService {
private final DiaryMapper diaryRepository;
    private final DiarySearchRepository diarySearchRepository;

public DiaryService(DiaryMapper diaryRepository,
                        ObjectProvider<DiarySearchRepository> diarySearchRepositoryProvider) {
        this.diaryRepository = diaryRepository;
        this.diarySearchRepository = diarySearchRepositoryProvider.getIfAvailable();
    }

    public Diary save(Diary diary) {
        Diary saved = diaryRepository.save(diary);
        if (diarySearchRepository != null) {
            DiaryDocument doc = toDocument(saved);
            try {
                diarySearchRepository.save(doc);
            } catch (Exception ignored) {
            }
        }
        return saved;
    }

    public List<DiaryDocument> fullTextSearch(String keyword) {
        if (diarySearchRepository != null) {
            return diarySearchRepository.findByTitleContainingOrContentContaining(keyword, keyword);
        }
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword).stream()
                .map(this::toDocument)
                .toList();
    }

    private DiaryDocument toDocument(Diary diary) {
        DiaryDocument doc = new DiaryDocument();
        doc.setId(String.valueOf(diary.getId()));
        doc.setTitle(diary.getTitle());
        doc.setContent(diary.getContent());
        if (diary.getDestination() != null) {
            doc.setDestinationName(diary.getDestination().getName());
        }
        return doc;
    }
}
