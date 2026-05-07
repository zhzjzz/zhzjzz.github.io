package com.travel.system.service.nav;

import com.travel.system.mapper.nav.RoadEdgeMapper;
import com.travel.system.mapper.nav.RoadNodeMapper;
import com.travel.system.mapper.nav.SpotMapper;
import com.travel.system.model.nav.RoadEdge;
import com.travel.system.model.nav.RoadNode;
import com.travel.system.model.nav.Spot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NavigationDataService {

    private final RoadNodeMapper roadNodeMapper;
    private final RoadEdgeMapper roadEdgeMapper;
    private final SpotMapper spotMapper;

    public NavigationDataService(RoadNodeMapper roadNodeMapper,
                                 RoadEdgeMapper roadEdgeMapper,
                                 SpotMapper spotMapper) {
        this.roadNodeMapper = roadNodeMapper;
        this.roadEdgeMapper = roadEdgeMapper;
        this.spotMapper = spotMapper;
    }

    public List<RoadNode> loadNodes(String spotName) {
        if (spotName == null || spotName.isBlank()) {
            return roadNodeMapper.findAll();
        }
        return roadNodeMapper.findBySpotName(resolveNodeSpotName(spotName.trim()));
    }

    public List<RoadEdge> loadEdges(String spotName) {
        if (spotName == null || spotName.isBlank()) {
            return roadEdgeMapper.findAll();
        }
        return roadEdgeMapper.findBySpotName(resolveEdgeSpotName(spotName.trim()));
    }

    public RoadNode getNodeByOsmid(Long osmid) {
        return roadNodeMapper.findByOsmid(osmid);
    }

    /**
     * 根据景点名称查找 spots 表中的记录。
     */
    public Spot getSpotByName(String spotName) {
        return spotMapper.findByName(spotName);
    }

    /**
     * 获取某景区的出入口节点（nodes 表中该 spot_name 的第一条记录）。
     * 用作三段式导航中第二段城市交通的起/终点。
     */
    public RoadNode getGateNode(String spotName) {
        if (spotName == null || spotName.isBlank()) {
            return null;
        }
        return roadNodeMapper.findFirstBySpotName(resolveNodeSpotName(spotName.trim()));
    }

    /**
     * 将节点序列转为经纬度坐标数组，格式 [lat, lng]，供前端 Polyline 使用。
     */
    public List<double[]> pathToCoordinates(List<Long> nodeIds) {
        List<double[]> coords = new ArrayList<>();
        for (Long osmid : nodeIds) {
            RoadNode node = getNodeByOsmid(osmid);
            if (node != null) {
                coords.add(new double[]{node.getY(), node.getX()});
            }
        }
        return coords;
    }

    /**
     * 构建邻接表：key 为节点 osmid，value 为以该节点为起点的所有边。
     * 图是无向的，所以每条边同时加入 u→v 和 v→u。
     */
    public Map<Long, List<RoadEdge>> buildAdjacencyList(String spotName) {
        List<RoadEdge> edges = loadEdges(spotName);
        Map<Long, List<RoadEdge>> adj = new HashMap<>();
        for (RoadEdge e : edges) {
            adj.computeIfAbsent(e.getU(), k -> new ArrayList<>()).add(e);
            RoadEdge reverse = new RoadEdge();
            reverse.setU(e.getV());
            reverse.setV(e.getU());
            reverse.setSpotName(e.getSpotName());
            reverse.setLength(e.getLength());
            reverse.setCongestionBase(e.getCongestionBase());
            reverse.setAllowedVehicles(e.getAllowedVehicles());
            reverse.setGeometry(e.getGeometry());
            adj.computeIfAbsent(e.getV(), k -> new ArrayList<>()).add(reverse);
        }
        return adj;
    }

    private String resolveNodeSpotName(String spotName) {
        List<RoadNode> exact = roadNodeMapper.findBySpotName(spotName);
        if (!exact.isEmpty()) {
            return spotName;
        }
        return bestMatchingSpotName(
                roadNodeMapper.findAll().stream()
                        .map(RoadNode::getSpotName)
                        .toList(),
                spotName
        );
    }

    private String resolveEdgeSpotName(String spotName) {
        List<RoadEdge> exact = roadEdgeMapper.findBySpotName(spotName);
        if (!exact.isEmpty()) {
            return spotName;
        }
        return bestMatchingSpotName(
                roadEdgeMapper.findAll().stream()
                        .map(RoadEdge::getSpotName)
                        .toList(),
                spotName
        );
    }

    private String bestMatchingSpotName(List<String> candidates, String spotName) {
        return candidates.stream()
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .filter(name -> name.contains(spotName) || spotName.contains(name))
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .findFirst()
                .orElse(spotName);
    }
}
