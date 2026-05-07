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

    public Building getByNearestNodeId(Long nodeId) {
        return buildingMapper.findAll().stream()
                .filter(b -> nodeId.equals(b.getNearestNodeId()))
                .findFirst()
                .orElse(null);
    }

    public List<Building> listBySpot(String spotName) {
        return buildingMapper.findBySpotName(spotName);
    }

    public Building getById(Long buildingId) {
        return buildingMapper.findByBuildingId(buildingId);
    }

    public List<Building> listAll() {
        return buildingMapper.findAll();
    }
}
