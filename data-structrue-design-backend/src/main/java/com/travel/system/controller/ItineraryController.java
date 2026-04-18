package com.travel.system.controller;

import com.travel.system.model.Itinerary;
import com.travel.system.mapper.ItineraryMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code ItineraryController} 负责处理多人协作行程（Itinerary）相关的 HTTP 请求。
 *
 * <p>提供两套基本接口：
 *
 * <ul>
 *   <li>查询全部行程；</li>
 *   <li>创建新行程并自动写入 {@code updatedAt} 时间戳，便于后续协作场景的同步与冲突检测。</li>
 * </ul>
 *
 * <p>后续可扩展为支持按用户查询、行程分享、实时协作编辑等高级功能。
 *
 * @author 自动生成
 */
@RestController
@RequestMapping("/api/itineraries")
@Tag(name = "行程管理", description = "行程查询、创建等相关接口")
public class ItineraryController {

    /** 行程数据的 JPA 持久层仓库。 */
private final ItineraryMapper itineraryMapper;

    /**
     * 构造函数注入 {@link ItineraryMapper}。
     *
     * @param itineraryMapper 行程持久化接口
     */
public ItineraryController(ItineraryMapper itineraryMapper) {
        this.itineraryMapper = itineraryMapper;
    }

    /**
     * 查询全部行程。
     *
     * @return 系统中已有的所有 {@link Itinerary}
     */
    @Operation(summary = "查询全部行程", description = "获取系统中所有行程列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping
    public List<Itinerary> list() {
        return itineraryMapper.findAll();
    }

    @Operation(summary = "按 ID 查询行程", description = "根据行程 ID 获取详情，便于多人共享同一个行程链接/编号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "行程不存在")
    })
    @GetMapping("/{id}")
    public Itinerary getById(@PathVariable Long id) {
        Itinerary itinerary = itineraryMapper.findById(id);
        if (itinerary == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程不存在");
        }
        return itinerary;
    }

    /**
     * 创建新行程并自动打上更新时间戳。
     *
     * <p>协作场景下，客户端可通过比对 {@code updatedAt} 判断数据是否过期，
     * 从而实现乐观锁或增量同步。
     *
     * @param itinerary 前端提交的行程实体（JSON → {@link Itinerary}）
     * @return 保存后的实体，其中 {@code updatedAt} 已设置为当前时间
     */
    @Operation(summary = "创建行程", description = "创建新行程并自动设置更新时间戳")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @PostMapping
    public Itinerary create(@RequestBody Itinerary itinerary) {
        // 为协作场景记录最新更新时间，便于后续同步
        itinerary.setUpdatedAt(LocalDateTime.now());
        return itineraryMapper.save(itinerary);
    }

    @Operation(summary = "更新行程", description = "支持多人协作更新；当客户端更新时间早于服务端时返回冲突")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "行程不存在"),
        @ApiResponse(responseCode = "409", description = "行程已被其他协作者更新")
    })
    @PutMapping("/{id}")
    public Itinerary update(@PathVariable Long id, @RequestBody Itinerary itinerary) {
        Itinerary existing = itineraryMapper.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "行程不存在");
        }
        if (itinerary.getUpdatedAt() != null
                && existing.getUpdatedAt() != null
                && itinerary.getUpdatedAt().isBefore(existing.getUpdatedAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该行程已被其他协作者更新，请刷新后重试");
        }

        itinerary.setId(id);
        var expectedUpdatedAt = itinerary.getUpdatedAt();
        itinerary.setUpdatedAt(LocalDateTime.now());
        int changed = itineraryMapper.updateIfUnchanged(itinerary, expectedUpdatedAt);
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该行程已被其他协作者更新，请刷新后重试");
        }
        return itineraryMapper.findById(id);
    }
}
