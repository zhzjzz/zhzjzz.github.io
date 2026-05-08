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

    /**

     * 按查询条件读取列表数据；分页、过滤或排序规则由 service 层统一处理。

     */
    public List<Diary> list(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return diaryRepository.findAll();
        }
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword);
    }

    /**

     * 保存或更新实体数据，并返回数据库持久化后的结果。

     */
    public Diary save(Diary diary) {
        return diaryRepository.save(diary);
    }

    /**

     * 执行面向游记内容的关键词检索，关键词为空时返回常规列表结果。

     */
    public List<Diary> fullTextSearch(String keyword) {
        return diaryRepository.findByTitleOrContentContainingIgnoreCase(keyword);
    }
}
