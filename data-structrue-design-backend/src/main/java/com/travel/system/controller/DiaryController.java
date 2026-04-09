package com.travel.system.controller;

import com.travel.system.model.Diary;
import com.travel.system.repository.DiaryRepository;
import com.travel.system.search.DiaryDocument;
import com.travel.system.service.DiaryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diaries")
public class DiaryController {
    private final DiaryRepository diaryRepository;
    private final DiaryService diaryService;

    public DiaryController(DiaryRepository diaryRepository, DiaryService diaryService) {
        this.diaryRepository = diaryRepository;
        this.diaryService = diaryService;
    }

    @GetMapping
    public List<Diary> list(@RequestParam(required = false) String title) {
        if (title == null || title.isBlank()) {
            return diaryRepository.findAll();
        }
        return diaryRepository.findByTitleContainingIgnoreCase(title);
    }

    @PostMapping
    public Diary create(@RequestBody Diary diary) {
        return diaryService.save(diary);
    }

    @GetMapping("/search")
    public List<DiaryDocument> search(@RequestParam String keyword) {
        return diaryService.fullTextSearch(keyword);
    }
}
