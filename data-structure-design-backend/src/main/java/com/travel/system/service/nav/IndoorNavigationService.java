package com.travel.system.service.nav;

import com.travel.system.dto.IndoorBuildingDemo;
import com.travel.system.dto.IndoorNavigationRequest;
import com.travel.system.dto.IndoorNavigationResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Service
public class IndoorNavigationService {
    private final Map<String, BuildingGraph> demos = buildDemos();

    public List<IndoorBuildingDemo> listBuildings() {
        return demos.values().stream()
                .map(BuildingGraph::toDemo)
                .toList();
    }

    public IndoorNavigationResponse plan(IndoorNavigationRequest request) {
        String buildingId = textOr(request == null ? null : request.getBuildingId(), "teaching-a");
        BuildingGraph graph = demos.getOrDefault(buildingId, demos.get("teaching-a"));
        String from = textOr(request == null ? null : request.getFromNodeId(), graph.defaultFrom);
        String to = textOr(request == null ? null : request.getToNodeId(), graph.defaultTo);

        if (!graph.nodes.containsKey(from)) {
            from = graph.defaultFrom;
        }
        if (!graph.nodes.containsKey(to)) {
            to = graph.defaultTo;
        }

        List<String> path = shortestPath(graph, from, to, request == null ? null : request.getStrategy());
        List<IndoorNavigationResponse.IndoorRouteStep> steps = buildSteps(graph, path);
        IndoorNode fromNode = graph.nodes.get(from);
        IndoorNode toNode = graph.nodes.get(to);

        return new IndoorNavigationResponse(
                graph.id,
                graph.name,
                from,
                fromNode.name,
                to,
                toNode.name,
                steps.stream().mapToDouble(step -> safe(step.getDistance())).sum(),
                steps,
                buildFloorSegments(graph, path, steps),
                buildNotes(graph, path, steps)
        );
    }

    private List<String> shortestPath(BuildingGraph graph, String from, String to, String strategy) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        PriorityQueue<NodeCost> pq = new PriorityQueue<>(Comparator.comparingDouble(NodeCost::cost));
        graph.nodes.keySet().forEach(nodeId -> dist.put(nodeId, Double.POSITIVE_INFINITY));
        dist.put(from, 0.0);
        pq.add(new NodeCost(from, 0.0));

        while (!pq.isEmpty()) {
            NodeCost current = pq.poll();
            if (current.cost > dist.getOrDefault(current.nodeId, Double.POSITIVE_INFINITY)) {
                continue;
            }
            if (current.nodeId.equals(to)) {
                break;
            }
            for (IndoorEdge edge : graph.edges.getOrDefault(current.nodeId, List.of())) {
                double candidate = current.cost + edge.distance;
                if (candidate < dist.getOrDefault(edge.to, Double.POSITIVE_INFINITY)) {
                    dist.put(edge.to, candidate);
                    prev.put(edge.to, current.nodeId);
                    pq.add(new NodeCost(edge.to, candidate));
                }
            }
        }

        if (!from.equals(to) && !prev.containsKey(to)) {
            return List.of(from);
        }

        List<String> path = new ArrayList<>();
        String cursor = to;
        path.add(cursor);
        while (!cursor.equals(from)) {
            cursor = prev.get(cursor);
            if (cursor == null) {
                return List.of(from);
            }
            path.add(0, cursor);
        }
        return path;
    }

    private List<IndoorNavigationResponse.IndoorRouteStep> buildSteps(BuildingGraph graph, List<String> path) {
        List<IndoorNavigationResponse.IndoorRouteStep> steps = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            IndoorNode from = graph.nodes.get(path.get(i));
            IndoorNode to = graph.nodes.get(path.get(i + 1));
            IndoorEdge edge = graph.findEdge(from.id, to.id);
            steps.add(new IndoorNavigationResponse.IndoorRouteStep(
                    from.id,
                    from.name,
                    to.id,
                    to.name,
                    from.floor,
                    to.floor,
                    edge.action,
                    instruction(from, to, edge),
                    edge.distance
            ));
        }
        return steps;
    }

    private List<IndoorNavigationResponse.IndoorFloorSegment> buildFloorSegments(
            BuildingGraph graph,
            List<String> path,
            List<IndoorNavigationResponse.IndoorRouteStep> steps) {
        Map<Integer, List<String>> namesByFloor = new LinkedHashMap<>();
        Map<Integer, Double> distanceByFloor = new LinkedHashMap<>();

        for (String nodeId : path) {
            IndoorNode node = graph.nodes.get(nodeId);
            namesByFloor.computeIfAbsent(node.floor, ignored -> new ArrayList<>());
            List<String> names = namesByFloor.get(node.floor);
            if (names.isEmpty() || !names.get(names.size() - 1).equals(node.name)) {
                names.add(node.name);
            }
        }
        for (IndoorNavigationResponse.IndoorRouteStep step : steps) {
            int floor = step.getFromFloor();
            distanceByFloor.merge(floor, safe(step.getDistance()), Double::sum);
        }

        return namesByFloor.entrySet().stream()
                .map(entry -> new IndoorNavigationResponse.IndoorFloorSegment(
                        entry.getKey(),
                        entry.getKey() + "F 导航段",
                        entry.getValue(),
                        distanceByFloor.getOrDefault(entry.getKey(), 0.0)
                ))
                .toList();
    }

    private List<String> buildNotes(
            BuildingGraph graph,
            List<String> path,
            List<IndoorNavigationResponse.IndoorRouteStep> steps) {
        boolean usesElevator = steps.stream().anyMatch(step -> "elevator".equals(step.getAction()));
        boolean multiFloor = path.stream()
                .map(nodeId -> graph.nodes.get(nodeId).floor)
                .distinct()
                .count() > 1;
        List<String> notes = new ArrayList<>();
        notes.add("演示建筑：" + graph.scenario);
        notes.add("路径按建筑内部节点图使用 Dijkstra 算法计算，可解释为室内版最短路。");
        if (usesElevator) {
            notes.add("路线包含电梯换层，覆盖“大门到电梯、楼层间电梯、楼层内到房间”的验收要求。");
        }
        if (multiFloor) {
            notes.add("本次路线跨越多个楼层，前端按楼层拆分展示，便于课堂演示。");
        }
        return notes;
    }

    private String instruction(IndoorNode from, IndoorNode to, IndoorEdge edge) {
        if ("elevator".equals(edge.action)) {
            return "乘坐电梯：" + from.floor + "F 到 " + to.floor + "F，抵达" + to.name;
        }
        if ("stairs".equals(edge.action)) {
            return "走楼梯：" + from.floor + "F 到 " + to.floor + "F，抵达" + to.name;
        }
        return "从" + from.name + "前往" + to.name;
    }

    private Map<String, BuildingGraph> buildDemos() {
        Map<String, BuildingGraph> map = new LinkedHashMap<>();
        addTeachingBuilding(map);
        addMuseum(map);
        addVisitorCenter(map);
        return map;
    }

    private void addTeachingBuilding(Map<String, BuildingGraph> map) {
        BuildingGraph graph = new BuildingGraph(
                "teaching-a",
                "智慧教学楼 A 座",
                "教学楼",
                "从教学楼大门进入，经过一层大厅和电梯，上到三层教室或五层实验室。",
                "t-gate",
                "t-meeting-501"
        );
        graph.node("t-gate", "教学楼正门", 1, "entrance", 12, 74);
        graph.node("t-lobby", "一层大厅", 1, "hall", 38, 74);
        graph.node("t-elevator-1", "1F 电梯厅", 1, "elevator", 66, 74);
        graph.node("t-office-1", "教务服务窗口", 1, "service", 38, 42);
        graph.node("t-study-1", "一层自习区", 1, "room", 18, 28);
        graph.node("t-restroom-1", "1F 卫生间", 1, "service", 78, 40);
        graph.node("t-elevator-2", "2F 电梯厅", 2, "elevator", 18, 52);
        graph.node("t-corridor-2", "二层连廊", 2, "corridor", 42, 52);
        graph.node("t-room-201", "201 阶梯教室", 2, "room", 70, 28);
        graph.node("t-room-204", "204 研讨室", 2, "room", 70, 52);
        graph.node("t-lab-2", "二层开放实验区", 2, "room", 70, 76);
        graph.node("t-restroom-2", "2F 卫生间", 2, "service", 34, 78);
        graph.node("t-elevator-3", "3F 电梯厅", 3, "elevator", 18, 54);
        graph.node("t-corridor-3", "三层走廊", 3, "corridor", 44, 54);
        graph.node("t-room-301", "301 教室", 3, "room", 72, 22);
        graph.node("t-room-302", "302 多媒体教室", 3, "room", 72, 36);
        graph.node("t-room-305", "305 实验室", 3, "room", 72, 72);
        graph.node("t-printer-3", "三层打印点", 3, "service", 42, 26);
        graph.node("t-restroom-3", "3F 卫生间", 3, "service", 28, 82);
        graph.node("t-elevator-4", "4F 电梯厅", 4, "elevator", 18, 50);
        graph.node("t-corridor-4", "四层教师办公区", 4, "corridor", 42, 50);
        graph.node("t-office-401", "401 教师办公室", 4, "room", 70, 30);
        graph.node("t-office-405", "405 课设工作室", 4, "room", 70, 50);
        graph.node("t-meeting-4", "四层小会议室", 4, "room", 70, 72);
        graph.node("t-restroom-4", "4F 卫生间", 4, "service", 34, 78);
        graph.node("t-elevator-5", "5F 电梯厅", 5, "elevator", 18, 48);
        graph.node("t-open-5", "五层开放办公区", 5, "hall", 42, 48);
        graph.node("t-meeting-501", "501 研讨室", 5, "room", 68, 48);
        graph.node("t-archive-5", "资料室", 5, "room", 68, 26);
        graph.node("t-tea-5", "茶水间", 5, "service", 68, 72);
        graph.link("t-gate", "t-lobby", 12, 18, "walk");
        graph.link("t-lobby", "t-elevator-1", 18, 28, "walk");
        graph.link("t-lobby", "t-office-1", 12, 20, "walk");
        graph.link("t-office-1", "t-study-1", 18, 28, "walk");
        graph.link("t-office-1", "t-restroom-1", 26, 40, "walk");
        graph.link("t-restroom-1", "t-elevator-1", 18, 28, "walk");
        graph.link("t-elevator-1", "t-elevator-2", 2, 18, "elevator");
        graph.link("t-elevator-2", "t-corridor-2", 10, 14, "walk");
        graph.link("t-corridor-2", "t-room-201", 18, 28, "walk");
        graph.link("t-corridor-2", "t-room-204", 16, 24, "walk");
        graph.link("t-corridor-2", "t-lab-2", 22, 34, "walk");
        graph.link("t-corridor-2", "t-restroom-2", 16, 24, "walk");
        graph.link("t-elevator-2", "t-elevator-3", 2, 18, "elevator");
        graph.link("t-elevator-3", "t-corridor-3", 8, 12, "walk");
        graph.link("t-corridor-3", "t-room-301", 18, 28, "walk");
        graph.link("t-corridor-3", "t-room-302", 16, 24, "walk");
        graph.link("t-corridor-3", "t-room-305", 22, 34, "walk");
        graph.link("t-corridor-3", "t-printer-3", 12, 18, "walk");
        graph.link("t-corridor-3", "t-restroom-3", 16, 26, "walk");
        graph.link("t-elevator-3", "t-elevator-4", 2, 18, "elevator");
        graph.link("t-elevator-4", "t-corridor-4", 10, 14, "walk");
        graph.link("t-corridor-4", "t-office-401", 18, 28, "walk");
        graph.link("t-corridor-4", "t-office-405", 16, 24, "walk");
        graph.link("t-corridor-4", "t-meeting-4", 20, 30, "walk");
        graph.link("t-corridor-4", "t-restroom-4", 16, 24, "walk");
        graph.link("t-elevator-4", "t-elevator-5", 2, 18, "elevator");
        graph.link("t-elevator-5", "t-open-5", 12, 18, "walk");
        graph.link("t-open-5", "t-meeting-501", 18, 28, "walk");
        graph.link("t-open-5", "t-archive-5", 16, 24, "walk");
        graph.link("t-open-5", "t-tea-5", 16, 24, "walk");
        map.put(graph.id, graph);
    }

    private void addMuseum(Map<String, BuildingGraph> map) {
        BuildingGraph graph = new BuildingGraph(
                "heritage-museum",
                "景区历史博物馆",
                "博物馆",
                "模拟景区博物馆从入口安检到二层展厅、文创区和三层临展厅的室内导航。",
                "m-gate",
                "m-special-3"
        );
        graph.node("m-gate", "博物馆入口", 1, "entrance", 12, 80);
        graph.node("m-security", "安检口", 1, "service", 34, 80);
        graph.node("m-lobby", "序厅", 1, "hall", 56, 80);
        graph.node("m-elevator-1", "1F 电梯厅", 1, "elevator", 78, 80);
        graph.node("m-ticket", "票务服务台", 1, "service", 34, 52);
        graph.node("m-audio", "语音导览租借处", 1, "service", 56, 52);
        graph.node("m-restroom-1", "1F 卫生间", 1, "service", 78, 52);
        graph.node("m-elevator-2", "2F 电梯厅", 2, "elevator", 20, 54);
        graph.node("m-corridor-2", "二层环形走廊", 2, "corridor", 42, 54);
        graph.node("m-gallery-2", "二层常设展厅", 2, "room", 54, 36);
        graph.node("m-shop-2", "二层文创商店", 2, "service", 78, 68);
        graph.node("m-reading-2", "二层文献阅览区", 2, "room", 70, 28);
        graph.node("m-restroom-2", "2F 卫生间", 2, "service", 36, 78);
        graph.node("m-elevator-3", "3F 电梯厅", 3, "elevator", 20, 52);
        graph.node("m-corridor-3", "三层展廊", 3, "corridor", 44, 52);
        graph.node("m-special-3", "三层临展厅", 3, "room", 70, 52);
        graph.node("m-vr-3", "VR 体验区", 3, "room", 70, 30);
        graph.node("m-lecture-3", "小型报告厅", 3, "room", 70, 74);
        graph.link("m-gate", "m-security", 10, 18, "walk");
        graph.link("m-security", "m-lobby", 14, 25, "walk");
        graph.link("m-lobby", "m-elevator-1", 20, 32, "walk");
        graph.link("m-security", "m-ticket", 14, 22, "walk");
        graph.link("m-ticket", "m-audio", 14, 22, "walk");
        graph.link("m-audio", "m-restroom-1", 18, 28, "walk");
        graph.link("m-restroom-1", "m-elevator-1", 18, 28, "walk");
        graph.link("m-elevator-1", "m-elevator-2", 2, 42, "elevator");
        graph.link("m-elevator-2", "m-corridor-2", 10, 16, "walk");
        graph.link("m-corridor-2", "m-gallery-2", 16, 24, "walk");
        graph.link("m-corridor-2", "m-restroom-2", 18, 30, "walk");
        graph.link("m-gallery-2", "m-reading-2", 14, 22, "walk");
        graph.link("m-gallery-2", "m-shop-2", 24, 40, "walk");
        graph.link("m-elevator-2", "m-elevator-3", 2, 36, "elevator");
        graph.link("m-elevator-3", "m-corridor-3", 12, 18, "walk");
        graph.link("m-corridor-3", "m-special-3", 16, 26, "walk");
        graph.link("m-corridor-3", "m-vr-3", 18, 28, "walk");
        graph.link("m-corridor-3", "m-lecture-3", 20, 30, "walk");
        map.put(graph.id, graph);
    }

    private void addVisitorCenter(Map<String, BuildingGraph> map) {
        BuildingGraph graph = new BuildingGraph(
                "visitor-center",
                "湖畔游客服务中心",
                "游客中心",
                "模拟景区游客中心内的咨询台、母婴室、二层观景咖啡区等游客服务空间。",
                "v-gate",
                "v-rooftop"
        );
        graph.node("v-gate", "游客中心入口", 1, "entrance", 12, 64);
        graph.node("v-desk", "咨询服务台", 1, "service", 38, 64);
        graph.node("v-elevator-1", "1F 电梯厅", 1, "elevator", 64, 64);
        graph.node("v-nursing", "母婴室", 1, "room", 38, 30);
        graph.node("v-first-aid", "医务点", 1, "service", 38, 86);
        graph.node("v-locker", "行李寄存处", 1, "service", 64, 30);
        graph.node("v-ticket", "票务咨询", 1, "service", 64, 86);
        graph.node("v-elevator-2", "2F 电梯厅", 2, "elevator", 20, 54);
        graph.node("v-lounge", "二层休息区", 2, "hall", 46, 54);
        graph.node("v-cafe", "二层观景咖啡区", 2, "room", 70, 54);
        graph.node("v-souvenir", "纪念品店", 2, "service", 70, 28);
        graph.node("v-restroom-2", "2F 卫生间", 2, "service", 70, 78);
        graph.node("v-rooftop", "屋顶观景台", 3, "room", 70, 48);
        graph.node("v-elevator-3", "3F 电梯厅", 3, "elevator", 20, 48);
        graph.node("v-photo-3", "屋顶拍照点", 3, "room", 70, 26);
        graph.node("v-shelter-3", "雨棚休息区", 3, "hall", 70, 72);
        graph.link("v-gate", "v-desk", 12, 20, "walk");
        graph.link("v-desk", "v-elevator-1", 10, 16, "walk");
        graph.link("v-desk", "v-nursing", 18, 30, "walk");
        graph.link("v-desk", "v-first-aid", 16, 28, "walk");
        graph.link("v-elevator-1", "v-locker", 14, 22, "walk");
        graph.link("v-elevator-1", "v-ticket", 14, 22, "walk");
        graph.link("v-elevator-1", "v-elevator-2", 2, 36, "elevator");
        graph.link("v-elevator-2", "v-lounge", 14, 22, "walk");
        graph.link("v-lounge", "v-cafe", 18, 28, "walk");
        graph.link("v-cafe", "v-souvenir", 16, 24, "walk");
        graph.link("v-cafe", "v-restroom-2", 16, 24, "walk");
        graph.link("v-elevator-2", "v-elevator-3", 2, 36, "elevator");
        graph.link("v-elevator-3", "v-rooftop", 18, 30, "walk");
        graph.link("v-rooftop", "v-photo-3", 14, 22, "walk");
        graph.link("v-rooftop", "v-shelter-3", 14, 22, "walk");
        map.put(graph.id, graph);
    }

    private String textOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private double safe(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0 : value;
    }

    private record IndoorNode(String id, String name, int floor, String type, double x, double y) {
    }

    private record IndoorEdge(String from, String to, double distance, double time, String action) {
    }

    private record NodeCost(String nodeId, double cost) {
    }

    private static class BuildingGraph {
        private final String id;
        private final String name;
        private final String type;
        private final String scenario;
        private final String defaultFrom;
        private final String defaultTo;
        private final Map<String, IndoorNode> nodes = new LinkedHashMap<>();
        private final Map<String, List<IndoorEdge>> edges = new HashMap<>();

        private BuildingGraph(String id, String name, String type, String scenario, String defaultFrom, String defaultTo) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.scenario = scenario;
            this.defaultFrom = defaultFrom;
            this.defaultTo = defaultTo;
        }

        private void node(String id, String name, int floor, String type, double x, double y) {
            nodes.put(id, new IndoorNode(id, name, floor, type, x, y));
        }

        private void link(String from, String to, double distance, double time, String action) {
            edges.computeIfAbsent(from, ignored -> new ArrayList<>()).add(new IndoorEdge(from, to, distance, time, action));
            edges.computeIfAbsent(to, ignored -> new ArrayList<>()).add(new IndoorEdge(to, from, distance, time, action));
        }

        private IndoorEdge findEdge(String from, String to) {
            return edges.getOrDefault(from, List.of()).stream()
                    .filter(edge -> edge.to.equals(to))
                    .findFirst()
                    .orElse(new IndoorEdge(from, to, 0.0, 0.0, "walk"));
        }

        private IndoorBuildingDemo toDemo() {
            List<Integer> floors = nodes.values().stream()
                    .map(IndoorNode::floor)
                    .distinct()
                    .sorted()
                    .toList();
            List<IndoorBuildingDemo.IndoorPoint> points = nodes.values().stream()
                    .map(node -> new IndoorBuildingDemo.IndoorPoint(node.id, node.name, node.floor, node.type, node.x, node.y))
                    .toList();
            List<IndoorBuildingDemo.IndoorConnection> connections = edges.values().stream()
                    .flatMap(List::stream)
                    .filter(edge -> edge.from.compareTo(edge.to) < 0)
                    .map(edge -> new IndoorBuildingDemo.IndoorConnection(edge.from, edge.to, edge.action))
                    .toList();
            return new IndoorBuildingDemo(id, name, type, scenario, floors, defaultFrom, defaultTo, points, connections);
        }
    }
}
