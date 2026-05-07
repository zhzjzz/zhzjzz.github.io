package com.travel.system.mapper.nav;

import com.travel.system.model.nav.Poi;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PoiMapper {

    List<Poi> findAll();

    List<Poi> findBySpotName(@Param("spotName") String spotName);

    List<Poi> findBySpotNameAndType(@Param("spotName") String spotName, @Param("type") String type);

    Poi findByPoiId(@Param("poiId") Long poiId);
}
