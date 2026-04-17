package com.travel.system.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.PointList;
import com.travel.system.dto.OsmRouteResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class OsmRouteService {
    private static final String PROFILE_CAR = "car";
    private static final String PROFILE_BIKE = "bike";
    private static final String PROFILE_FOOT = "foot";
    private static final String PROFILE_PUBLIC_TRANSPORT = "public_transport";
    // 官方建议前端入参：car / bike / walk / public_transport；其余为兼容别名。
    private static final Map<String, String> MODE_PROFILE_MAPPING = Map.ofEntries(
            Map.entry("car", PROFILE_CAR),
            Map.entry("bike", PROFILE_BIKE),
            Map.entry("bicycle", PROFILE_BIKE),
            Map.entry("foot", PROFILE_FOOT),
            Map.entry("walk", PROFILE_FOOT),
            Map.entry("walking", PROFILE_FOOT),
            Map.entry("public_transport", PROFILE_PUBLIC_TRANSPORT),
            Map.entry("public-transport", PROFILE_PUBLIC_TRANSPORT),
            Map.entry("transit", PROFILE_PUBLIC_TRANSPORT),
            Map.entry("bus", PROFILE_PUBLIC_TRANSPORT),
            Map.entry("subway", PROFILE_PUBLIC_TRANSPORT),
            Map.entry("pt", PROFILE_PUBLIC_TRANSPORT)
    );

    private final GraphHopper graphHopper;

    public OsmRouteService(ObjectProvider<GraphHopper> graphHopperProvider) {
        this.graphHopper = graphHopperProvider.getIfAvailable();
    }

    public OsmRouteResponse route(double startLat, double startLon, double endLat, double endLon, String mode) {
        if (graphHopper == null) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "GraphHopper 不可用，请检查 GRAPHHOPPER_OSM_FILE 路径和 OSM 数据文件");
        }

        String profile = resolveProfile(mode);
        GHRequest request = new GHRequest(startLat, startLon, endLat, endLon)
                .setProfile(profile)
                .setLocale(Locale.CHINA);
        GHResponse response = graphHopper.route(request);
        if (response.hasErrors()) {
            String details = response.getErrors().stream()
                    .map(Throwable::getMessage)
                    .collect(Collectors.joining("; "));
            throw new ResponseStatusException(BAD_REQUEST, "导航失败: " + details);
        }

        ResponsePath best = response.getBest();
        PointList pointList = best.getPoints();
        List<double[]> path = new ArrayList<>(pointList.size());
        for (int i = 0; i < pointList.size(); i++) {
            path.add(new double[]{pointList.getLat(i), pointList.getLon(i)});
        }
        return new OsmRouteResponse(best.getDistance(), best.getTime(), path);
    }

    private String resolveProfile(String mode) {
        String normalized = mode == null ? PROFILE_CAR : mode.trim().toLowerCase(Locale.ROOT);
        String profile = MODE_PROFILE_MAPPING.get(normalized);
        if (profile == null) {
            throw new ResponseStatusException(BAD_REQUEST, "不支持的出行方式: " + mode);
        }
        return profile;
    }
}
