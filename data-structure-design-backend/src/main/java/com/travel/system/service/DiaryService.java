package com.travel.system.service;

import com.travel.system.model.Diary;
import com.travel.system.mapper.DiaryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiaryService {

    private final DiaryMapper diaryRepository;

    public DiaryService(DiaryMapper diaryRepository) {
        this.diaryRepository = diaryRepository;
    }

    public List<Diary> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return diaryRepository.findAll();
        }
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword);
    }

    public Diary save(Diary diary) {
        return diaryRepository.save(diary);
    }

    public List<Diary> fullTextSearch(String keyword) {
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword);
    }
}
