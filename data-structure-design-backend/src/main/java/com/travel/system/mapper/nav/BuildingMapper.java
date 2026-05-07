package com.travel.system.mapper.nav;

import com.travel.system.model.nav.Building;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BuildingMapper {

    List<Building> findAll();

    List<Building> findBySpotName(@Param("spotName") String spotName);

    List<Building> findBySpotNameAndType(@Param("spotName") String spotName, @Param("type") String type);

    Building findByBuildingId(@Param("buildingId") Long buildingId);
}
