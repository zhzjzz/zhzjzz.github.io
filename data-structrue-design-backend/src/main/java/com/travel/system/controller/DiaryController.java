package com.travel.system.controller;

import com.travel.system.model.Diary;
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
@Tag(name = "旅游日记", description = "日记查询、创建、全文搜索等相关接口")
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
}
