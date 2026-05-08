package com.travel.system.controller;

import com.travel.system.model.Diary;
import com.travel.system.model.DiaryComment;
import com.travel.system.service.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diaries")
@Tag(name = "旅游日记", description = "日记查询、创建、全文搜索、热度评分和交流分享接口")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @Operation(summary = "查询日记列表", description = "支持标题/内容模糊搜索，无关键字则返回所有日记")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public List<Diary> list(
            @Parameter(description = "关键字，用于模糊匹配标题或内容") @RequestParam(required = false) String keyword) {
        return diaryService.list(keyword);
    }

    @Operation(summary = "创建日记")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping
    public Diary create(@RequestBody Diary diary) {
        return diaryService.save(diary);
    }

    @Operation(summary = "日记全文搜索", description = "对日记标题和内容进行全文检索")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    @GetMapping("/search")
    public List<Diary> search(
            @Parameter(description = "搜索关键字") @RequestParam String keyword) {
        return diaryService.fullTextSearch(keyword);
    }

    @Operation(summary = "热门公开游记", description = "按热度评分和浏览量返回公开日记")
    @GetMapping("/hot")
    public List<Diary> hot(@RequestParam(defaultValue = "6") int limit) {
        return diaryService.hot(limit);
    }

    @Operation(summary = "分享链接查询", description = "通过分享 token 获取公开日记")
    @GetMapping("/share/{token}")
    public Diary shared(@PathVariable String token) {
        return diaryService.shared(token);
    }

    @Operation(summary = "记录日记互动", description = "支持 like、favorite、share 三类互动")
    @PostMapping("/{id}/interactions/{type}")
    public Diary interact(@PathVariable Long id, @PathVariable String type) {
        return diaryService.interact(id, type);
    }

    @Operation(summary = "查询日记评论")
    @GetMapping("/{id}/comments")
    public List<DiaryComment> comments(@PathVariable Long id) {
        return diaryService.comments(id);
    }

    @Operation(summary = "发布日记评论")
    @PostMapping("/{id}/comments")
    public DiaryComment comment(@PathVariable Long id, @RequestBody DiaryComment comment) {
        return diaryService.comment(id, comment);
    }
}
