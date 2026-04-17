package com.travel.system.controller;

import com.travel.system.service.ElasticsearchFullSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Elasticsearch 同步", description = "一键将业务表数据全量同步到 Elasticsearch")
public class ElasticsearchSyncController {

    private final ElasticsearchFullSyncService elasticsearchFullSyncService;

    public ElasticsearchSyncController(ElasticsearchFullSyncService elasticsearchFullSyncService) {
        this.elasticsearchFullSyncService = elasticsearchFullSyncService;
    }

    @Operation(summary = "全量同步所有业务表到 ES", description = "按表全量读取 MySQL 并批量写入 Elasticsearch 索引")
    @ApiResponse(responseCode = "200", description = "同步完成")
    @PostMapping("/sync-all")
    public Map<String, Object> syncAll() {
        return elasticsearchFullSyncService.syncAllTables();
    }
}
