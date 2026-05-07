package com.travel.system.mapper.nav;

import com.travel.system.model.nav.RoadEdge;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoadEdgeMapper {

    List<RoadEdge> findAll();

    List<RoadEdge> findBySpotName(@Param("spotName") String spotName);

    List<RoadEdge> findByNode(@Param("osmid") Long osmid);
}
