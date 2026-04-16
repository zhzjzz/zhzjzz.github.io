package com.travel.system.controller;

import com.travel.system.model.Diary;
import com.travel.system.search.DiaryDocument;
import com.travel.system.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@code DiaryController} 负责处理旅游日记相关的 HTTP 请求。
 *
 * <p>提供以下功能：
 *
 * <ul>
 *   <li>日记列表查询并支持标题/内容全文搜索（优先使用 Elasticsearch）；</li>
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
@Tag(name = "旅游日记", description = "日记查询、创建、全文搜索等相关接口")
public class DiaryController {

    /** 日记业务逻辑服务，负责持久化与全文检索的协调（支持 Elasticsearch）。 */
    private final DiaryService diaryService;

    /**
     * 构造函数注入依赖。
     *
     * @param diaryService    日记业务服务
     */
    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    /**
     * 查询日记列表。
     *
     * <p>优先使用 Elasticsearch 进行全文搜索，若 ES 不可用则回退到 MySQL 查询。
     *
     * @param keyword 可选的关键字（标题或内容）；若为 {@code null} 或空字符串，则返回全部日记
     * @return 符合条件的 {@link Diary} 列表
     */
    @Operation(summary = "查询日记列表", description = "支持标题/内容全文搜索（优先使用 Elasticsearch），无关键字则返回所有日记")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public List<Diary> list(
            @Parameter(description = "关键字，用于模糊匹配标题或内容") @RequestParam(required = false) String keyword) {
        // 通过 Service 层处理搜索，优先使用 ES 全文搜索
        return diaryService.list(keyword);
    }

    /**
     * 创建日记并同步写入 Elasticsearch 索引。
     *
     * @param diary 前端提交的日记实体
     * @return 保存后的日记实体
     */
    @Operation(summary = "创建日记", description = "新增日记并同步写入Elasticsearch索引")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
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
    @Operation(summary = "日记全文搜索", description = "基于Elasticsearch对日记标题和内容进行全文检索")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    @GetMapping("/search")
    public List<DiaryDocument> search(
            @Parameter(description = "搜索关键字，支持分词查询") @RequestParam String keyword) {
        return diaryService.fullTextSearch(keyword);
    }
}
