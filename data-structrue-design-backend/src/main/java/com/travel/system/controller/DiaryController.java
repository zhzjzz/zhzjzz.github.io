package com.travel.system.controller;

import com.travel.system.model.Diary;
import com.travel.system.mapper.DiaryMapper;
import com.travel.system.search.DiaryDocument;
import com.travel.system.service.DiaryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code DiaryController} 负责处理旅游日记相关的 HTTP 请求。
 *
 * <p>提供以下功能：
 *
 * <ul>
 *   <li>日记列表查询并支持标题模糊搜索；</li>
 *   <li>新增日记并同步写入 Elasticsearch 索引；</li>
 *   <li>基于 Elasticsearch 的全文检索接口。</li>
 * </ul>
 *
 * <p>该控制器与 {@link DiaryService} 协作完成日记的持久化与检索功能。
 *
 * @author 自动生成
 */
@RestController
@RequestMapping("/api/diaries")
public class DiaryController {

    /** 日记数据持久层仓库。 */
private final DiaryMapper diaryRepository;

    /** 日记业务逻辑服务，负责持久化与全文检索的协调。 */
    private final DiaryService diaryService;

    /**
     * 构造函数注入依赖。
     *
     * @param diaryRepository 日记数据访问层
     * @param diaryService    日记业务服务
     */
public DiaryController(DiaryMapper diaryRepository, DiaryService diaryService) {
        this.diaryRepository = diaryRepository;
        this.diaryService = diaryService;
    }

    /**
     * 查询日记列表。
     *
     * @param title 可选的标题关键字；若为 {@code null} 或空字符串，则返回全部日记
     * @return 符合条件的 {@link Diary} 列表
     */
    @GetMapping
    public List<Diary> list(@RequestParam(required = false) String title) {
        if (title == null || title.isBlank()) {
            // 未指定关键字，返回所有日记记录
            return diaryRepository.findAll();
        }
        // 在标题字段进行模糊匹配（不区分大小写）
        return diaryRepository.findByTitleContainingIgnoreCase(title);
    }

    /**
     * 创建日记并同步写入 Elasticsearch 索引。
     *
     * @param diary 前端提交的日记实体
     * @return 保存后的日记实体
     */
    @PostMapping
    public Diary create(@RequestBody Diary diary) {
        // 通过 DiaryService 确保同时写入数据库与索引
        return diaryService.save(diary);
    }

    /**
     * 日记全文检索接口。
     *
     * <p>利用 Elasticsearch 对日记的标题与内容进行全文检索，支持分词查询。
     *
     * @param keyword 检索关键字
     * @return 匹配的 {@link DiaryDocument} 列表
     */
    @GetMapping("/search")
    public List<DiaryDocument> search(@RequestParam String keyword) {
        return diaryService.fullTextSearch(keyword);
    }
}
