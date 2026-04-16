package com.travel.system.service;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.util.PointList;
import com.travel.system.config.GraphHopperProperties;
import com.travel.system.dto.OsmRouteResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class OsmRouteService {
    private final GraphHopper graphHopper;
    private final GraphHopperProperties properties;

    public OsmRouteService(ObjectProvider<GraphHopper> graphHopperProvider, GraphHopperProperties properties) {
        this.graphHopper = graphHopperProvider.getIfAvailable();
        this.properties = properties;
    }

    public OsmRouteResponse route(double startLat, double startLon, double endLat, double endLon) {
        if (graphHopper == null) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "GraphHopper 未启用，请设置 GRAPHHOPPER_ENABLED=true 并提供 OSM 数据文件");
        }

        GHRequest request = new GHRequest(startLat, startLon, endLat, endLon)
                .setProfile(properties.getProfile())
                .setLocale(Locale.CHINA);
        GHResponse response = graphHopper.route(request);
        if (response.hasErrors()) {
            throw new ResponseStatusException(BAD_REQUEST, "导航失败: " + response.getErrors());
        }

        ResponsePath best = response.getBest();
        PointList pointList = best.getPoints();
        List<double[]> path = new ArrayList<>(pointList.size());
        for (int i = 0; i < pointList.size(); i++) {
            path.add(new double[]{pointList.getLat(i), pointList.getLon(i)});
        }
        return new OsmRouteResponse(best.getDistance(), best.getTime(), path);
    }
}
