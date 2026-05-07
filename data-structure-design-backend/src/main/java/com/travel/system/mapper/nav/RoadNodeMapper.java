package com.travel.system.mapper.nav;

import com.travel.system.model.nav.RoadNode;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoadNodeMapper {

    List<RoadNode> findAll();

    List<RoadNode> findBySpotName(@Param("spotName") String spotName);

    RoadNode findByOsmid(@Param("osmid") Long osmid);

    RoadNode findFirstBySpotName(@Param("spotName") String spotName);
}
