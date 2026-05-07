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

    public Poi getByNearestNodeId(Long nodeId) {
        return poiMapper.findAll().stream()
                .filter(p -> nodeId.equals(p.getNearestNodeId()))
                .findFirst()
                .orElse(null);
    }

    public List<Poi> listBySpot(String spotName) {
        return poiMapper.findBySpotName(spotName);
    }

    public Poi getById(Long poiId) {
        return poiMapper.findByPoiId(poiId);
    }

    public List<Poi> listAll() {
        return poiMapper.findAll();
    }
}
