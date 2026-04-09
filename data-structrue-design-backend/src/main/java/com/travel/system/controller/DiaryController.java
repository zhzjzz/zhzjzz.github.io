package com.travel.system.controller;

import com.travel.system.model.Diary;
import com.travel.system.repository.DiaryRepository;
import com.travel.system.search.DiaryDocument;
import com.travel.system.service.DiaryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 旅游日记控制器。
 * 提供日记的增删查、标题检索和全文检索能力。
 */
@RestController
@RequestMapping("/api/diaries")
public class DiaryController {
    private final DiaryRepository diaryRepository;
    private final DiaryService diaryService;

    public DiaryController(DiaryRepository diaryRepository, DiaryService diaryService) {
        this.diaryRepository = diaryRepository;
        this.diaryService = diaryService;
    }

    /**
     * 日记列表查询：
     * - title 为空：返回全部日记；
     * - title 非空：按标题模糊匹配。
     */
    @GetMapping
    public List<Diary> list(@RequestParam(required = false) String title) {
        if (title == null || title.isBlank()) {
            return diaryRepository.findAll();
        }
        return diaryRepository.findByTitleContainingIgnoreCase(title);
    }

    /**
     * 创建日记并同步写入检索索引。
     */
    @PostMapping
    public Diary create(@RequestBody Diary diary) {
        return diaryService.save(diary);
    }

    /**
     * 日记全文检索接口（Elasticsearch）。
     */
    @GetMapping("/search")
    public List<DiaryDocument> search(@RequestParam String keyword) {
        return diaryService.fullTextSearch(keyword);
    }
}
