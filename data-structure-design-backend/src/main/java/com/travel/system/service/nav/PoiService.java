package com.travel.system.service.nav;

import com.travel.system.mapper.nav.PoiMapper;
import com.travel.system.model.nav.Poi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PoiService {

    private final PoiMapper poiMapper;

    public PoiService(PoiMapper poiMapper) {
        this.poiMapper = poiMapper;
    }

    /**

     * 按景区、类型和关键词筛选建筑物或 POI，空条件表示不限制对应维度。

     */
    public List<Poi> searchBySpotAndType(String spotName, String type, String keyword) {
        List<Poi> pois;
        if (spotName == null || spotName.isBlank()) {
            pois = poiMapper.findAll();
        } else if (type != null && !type.isBlank()) {
            pois = poiMapper.findBySpotNameAndType(spotName.trim(), type.trim());
        } else {
            pois = poiMapper.findBySpotName(spotName.trim());
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase(Locale.ROOT);
            pois = pois.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(kw))
                    .collect(Collectors.toList());
        }
        return pois;
    }

    /**

     * 根据路网节点 ID 查询最近的建筑物或 POI，用于导航终点与地点数据互相转换。

     */
    public Poi getByNearestNodeId(Long nodeId) {
        return poiMapper.findAll().stream()
                .filter(p -> nodeId.equals(p.getNearestNodeId()))
                .findFirst()
                .orElse(null);
    }

    /**

     * 查询指定景区下的全部建筑物或 POI，供前端地点选择列表使用。

     */
    public List<Poi> listBySpot(String spotName) {
        return poiMapper.findBySpotName(spotName);
    }

    /**

     * 根据主键 ID 查询单条数据，找不到时返回空结果，供详情页或关联查询使用。

     */
    public Poi getById(Long poiId) {
        return poiMapper.findByPoiId(poiId);
    }

    /**

     * 查询全部记录，主要用于前端初始化下拉列表和无条件浏览。

     */
    public List<Poi> listAll() {
        return poiMapper.findAll();
    }
}
