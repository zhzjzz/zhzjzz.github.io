package com.travel.system.mapper.nav;

import com.travel.system.model.nav.Spot;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SpotMapper {

    List<Spot> findAll();

    List<Spot> findByKeyword(@Param("keyword") String keyword);

    List<Spot> findByCategory(@Param("category") String category);

    Spot findBySpotId(@Param("spotId") Long spotId);

    Spot findByName(@Param("name") String name);
}
