package com.travel.system.mapper.nav;

import com.travel.system.model.nav.CityRoute;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CityRouteMapper {

    List<CityRoute> findAll();

    List<CityRoute> findByFromSpot(@Param("fromSpot") String fromSpot);

    List<CityRoute> findByToSpot(@Param("toSpot") String toSpot);

    CityRoute findByFromAndTo(@Param("fromSpot") String fromSpot, @Param("toSpot") String toSpot);
}
