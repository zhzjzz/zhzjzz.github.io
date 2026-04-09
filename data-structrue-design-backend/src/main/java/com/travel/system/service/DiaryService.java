package com.travel.system.service;

import com.travel.system.model.Diary;
import com.travel.system.repository.DiaryRepository;
import com.travel.system.repository.DiarySearchRepository;
import com.travel.system.search.DiaryDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiaryService {
    private final DiaryRepository diaryRepository;
    private final DiarySearchRepository diarySearchRepository;

    public DiaryService(DiaryRepository diaryRepository, DiarySearchRepository diarySearchRepository) {
        this.diaryRepository = diaryRepository;
        this.diarySearchRepository = diarySearchRepository;
    }

    public Diary save(Diary diary) {
        Diary saved = diaryRepository.save(diary);
        DiaryDocument doc = new DiaryDocument();
        doc.setId(String.valueOf(saved.getId()));
        doc.setTitle(saved.getTitle());
        doc.setContent(saved.getContent());
        if (saved.getDestination() != null) {
            doc.setDestinationName(saved.getDestination().getName());
        }
        try {
            diarySearchRepository.save(doc);
        } catch (Exception ignored) {
        }
        return saved;
    }

    public List<DiaryDocument> fullTextSearch(String keyword) {
        return diarySearchRepository.findByTitleContainingOrContentContaining(keyword, keyword);
    }
}
