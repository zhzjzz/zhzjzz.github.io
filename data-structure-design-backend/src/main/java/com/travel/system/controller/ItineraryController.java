package com.travel.system.controller;

import com.travel.system.model.Itinerary;
import com.travel.system.service.ItineraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/itineraries")
@Tag(name = "行程管理", description = "行程查询、创建、更新等相关接口")
public class ItineraryController {

    private final ItineraryService itineraryService;

    public ItineraryController(ItineraryService itineraryService) {
        this.itineraryService = itineraryService;
    }
    /**
     * 按查询条件读取列表数据；分页、过滤或排序规则由 service 层统一处理。
     */

    @Operation(summary = "查询全部行程")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public List<Itinerary> list() {
        return itineraryService.findAll();
    }

    @Operation(summary = "按 ID 查询行程")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "行程不存在")
    })
    /**
     * 根据主键 ID 查询单条数据，找不到时返回空结果，供详情页或关联查询使用。
     */
    @GetMapping("/{id}")
    public Itinerary getById(@PathVariable Long id) {
        Itinerary itinerary = itineraryService.findById(id);
        if (itinerary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程不存在");
        }
        return itinerary;
    }

    @Operation(summary = "创建行程")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    /**
     * 处理新增资源请求，将前端提交的数据交给 service 保存，并返回保存后的对象。
     */
    @PostMapping
    public Itinerary create(@RequestBody Itinerary itinerary) {
        return itineraryService.create(itinerary);
    }

    @Operation(summary = "更新行程", description = "支持多人协作更新；乐观锁冲突时返回 409")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "行程不存在"),
        @ApiResponse(responseCode = "409", description = "行程已被其他协作者更新")
    })
    /**
     * 根据路径中的 ID 更新已有数据，并返回更新后的对象，避免前端传入 ID 与路径 ID 不一致。
     */
    @PutMapping("/{id}")
    public Itinerary update(@PathVariable Long id, @RequestBody Itinerary itinerary) {
        Itinerary result = itineraryService.update(id, itinerary);
        if (result == null) {
            Itinerary existing = itineraryService.findById(id);
            if (existing == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程不存在");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该行程已被其他协作者更新，请刷新后重试");
        }
        return result;
    }
}
