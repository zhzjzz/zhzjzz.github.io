package com.travel.system.service.nav;

import com.travel.system.mapper.nav.BuildingMapper;
import com.travel.system.model.nav.Building;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class BuildingService {

    private final BuildingMapper buildingMapper;

    public BuildingService(BuildingMapper buildingMapper) {
        this.buildingMapper = buildingMapper;
    }

    /**

     * 按景区、类型和关键词筛选建筑物或 POI，空条件表示不限制对应维度。

     */
    public List<Building> searchBySpotAndType(String spotName, String type, String keyword) {
        List<Building> buildings;
        if (spotName == null || spotName.isBlank()) {
            buildings = buildingMapper.findAll();
        } else if (type != null && !type.isBlank()) {
            buildings = buildingMapper.findBySpotNameAndType(spotName.trim(), type.trim());
        } else {
            buildings = buildingMapper.findBySpotName(spotName.trim());
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase(Locale.ROOT);
            buildings = buildings.stream()
                    .filter(b -> b.getName() != null && b.getName().toLowerCase(Locale.ROOT).contains(kw))
                    .collect(Collectors.toList());
        }
        return buildings;
    }

    /**

     * 根据路网节点 ID 查询最近的建筑物或 POI，用于导航终点与地点数据互相转换。

     */
    public Building getByNearestNodeId(Long nodeId) {
        return buildingMapper.findAll().stream()
                .filter(b -> nodeId.equals(b.getNearestNodeId()))
                .findFirst()
                .orElse(null);
    }

    /**

     * 查询指定景区下的全部建筑物或 POI，供前端地点选择列表使用。

     */
    public List<Building> listBySpot(String spotName) {
        return buildingMapper.findBySpotName(spotName);
    }

    /**

     * 根据主键 ID 查询单条数据，找不到时返回空结果，供详情页或关联查询使用。

     */
    public Building getById(Long buildingId) {
        return buildingMapper.findByBuildingId(buildingId);
    }

    /**

     * 查询全部记录，主要用于前端初始化下拉列表和无条件浏览。

     */
    public List<Building> listAll() {
        return buildingMapper.findAll();
    }
}
