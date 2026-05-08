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

    /**

     * 查询两个景区之间的城市交通配置，用于跨景区导航的中间城市段。

     */
    public CityRoute findByFromAndTo(String fromSpot, String toSpot) {
        return cityRouteMapper.findByFromAndTo(fromSpot, toSpot);
    }

    /**

     * 查询从指定景区出发的全部城市交通线路，供路线候选和调试使用。

     */
    public List<CityRoute> findByFromSpot(String fromSpot) {
        return cityRouteMapper.findByFromSpot(fromSpot);
    }

    /**

     * 查询全部数据记录，具体 SQL 由 XML mapper 或 mapper 接口维护。

     */
    public List<CityRoute> findAll() {
        return cityRouteMapper.findAll();
    }
}
