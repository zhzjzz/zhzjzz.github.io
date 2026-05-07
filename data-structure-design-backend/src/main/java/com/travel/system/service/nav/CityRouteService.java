package com.travel.system.service.nav;

import com.travel.system.mapper.nav.CityRouteMapper;
import com.travel.system.model.nav.CityRoute;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CityRouteService {

    private final CityRouteMapper cityRouteMapper;

    public CityRouteService(CityRouteMapper cityRouteMapper) {
        this.cityRouteMapper = cityRouteMapper;
    }

    public CityRoute findByFromAndTo(String fromSpot, String toSpot) {
        return cityRouteMapper.findByFromAndTo(fromSpot, toSpot);
    }

    public List<CityRoute> findByFromSpot(String fromSpot) {
        return cityRouteMapper.findByFromSpot(fromSpot);
    }

    public List<CityRoute> findAll() {
        return cityRouteMapper.findAll();
    }
}
