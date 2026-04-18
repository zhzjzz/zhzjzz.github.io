package com.travel.system.controller;

import com.travel.system.service.ElasticsearchFullSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Elasticsearch 同步", description = "一键将可检索业务表数据全量同步到 Elasticsearch")
public class ElasticsearchSyncController {

    private final ElasticsearchFullSyncService elasticsearchFullSyncService;

    public ElasticsearchSyncController(ElasticsearchFullSyncService elasticsearchFullSyncService) {
        this.elasticsearchFullSyncService = elasticsearchFullSyncService;
    }

    @Operation(summary = "全量同步可检索业务表到 ES", description = "按表全量读取 MySQL 并批量写入 Elasticsearch 索引（不包含当前未纳入检索的表）")
    @ApiResponse(responseCode = "200", description = "同步完成")
    @PostMapping("/sync-all")
    public Map<String, Object> syncAll() {
        return elasticsearchFullSyncService.syncAllTables();
    }

    @Operation(summary = "删除所有 ES 索引及数据", description = "删除 Elasticsearch 中所有业务索引及其中的数据")
    @ApiResponse(responseCode = "200", description = "删除完成")
    @DeleteMapping("/delete-all")
    public Map<String, Object> deleteAll() {
        return elasticsearchFullSyncService.deleteAllIndices();
    }

    @Operation(summary = "先删除所有 ES 数据，再全量导入", description = "先删除 Elasticsearch 中所有业务索引，然后按表全量读取 MySQL 并批量写入 Elasticsearch")
    @ApiResponse(responseCode = "200", description = "重置并同步完成")
    @PostMapping("/reset-and-sync")
    public Map<String, Object> resetAndSync() {
        Map<String, Object> result = new LinkedHashMap<>();
        // 第一步：删除所有 ES 索引
        result.put("step1_delete", elasticsearchFullSyncService.deleteAllIndices());
        // 第二步：全量同步（会重建索引并写入数据）
        result.put("step2_sync", elasticsearchFullSyncService.syncAllTables());
        return result;
    }
}
