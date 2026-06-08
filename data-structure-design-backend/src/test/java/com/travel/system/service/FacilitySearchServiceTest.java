package com.travel.system.service;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.mapper.FacilityMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.Facility;
import com.travel.system.model.nav.RoadEdge;
import com.travel.system.service.nav.NavigationDataService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FacilitySearchServiceTest {

    private final FacilityMapper facilityMapper = mock(FacilityMapper.class);
    private final FacilitySearchService service = new FacilitySearchService(facilityMapper);

    @Test
    void scenicSearchReturnsOnlyServiceFacilitiesInsideSelectedDestination() {
        Facility dormitory = facility("紫竹院公园_宿舍楼3", "宿舍楼", "building", "紫竹院公园", "景区");
        Facility shop = facility("紫竹院公园_商店26", "商店", "poi", "紫竹院公园", "景区");
        Facility nearbyCampusOffice = facility("北京大学_办公楼2", "办公楼", "building", "北京大学", "校园");
        when(facilityMapper.findAll()).thenReturn(List.of(dormitory, shop, nearbyCampusOffice));

        List<FacilityQueryResult> results = service.searchNearby(
                39.9393,
                116.3147,
                null,
                null,
                1000d,
                "紫竹院公园",
                "景区");

        assertThat(results)
                .extracting(result -> result.getFacility().getName())
                .containsExactly("紫竹院公园_商店26");
    }

    @Test
    void nearbySearchPrefersRoadNetworkDistanceWhenNodeIdsAreAvailable() {
        NavigationDataService navigationDataService = mock(NavigationDataService.class);
        FacilitySearchService routeService = new FacilitySearchService(facilityMapper, navigationDataService);
        Facility routeNear = facility("Route Near Toilet", "toilet", "poi", "Campus", "scenic");
        routeNear.setSourceNearestNodeId(3L);
        routeNear.setLatitude(39.0100);
        routeNear.setLongitude(116.0100);
        Facility straightNear = facility("Straight Near Shop", "shop", "poi", "Campus", "scenic");
        straightNear.setSourceNearestNodeId(2L);
        straightNear.setLatitude(39.0001);
        straightNear.setLongitude(116.0001);
        when(facilityMapper.findAll()).thenReturn(List.of(straightNear, routeNear));
        when(navigationDataService.buildAdjacencyList("Campus")).thenReturn(Map.of(
                1L, List.of(edge(1L, 2L, 1000.0), edge(1L, 3L, 20.0)),
                2L, List.of(edge(2L, 1L, 1000.0)),
                3L, List.of(edge(3L, 1L, 20.0))
        ));

        List<FacilityQueryResult> results = routeService.searchNearby(
                39.0, 116.0, 1L, null, null, null, "Campus", "scenic");

        assertThat(results).extracting(result -> result.getFacility().getName())
                .containsExactly("Route Near Toilet", "Straight Near Shop");
    }

    @Test
    void internalSchoolFacilityTypesAreHiddenInEveryScene() {
        assertThat(FacilitySearchService.isVisibleFacilityTypeForScene("教学楼", null)).isFalse();
        assertThat(FacilitySearchService.isVisibleFacilityTypeForScene("宿舍楼", "校园")).isFalse();
        assertThat(FacilitySearchService.isVisibleFacilityTypeForScene("办公楼", "景区")).isFalse();
        assertThat(FacilitySearchService.isVisibleFacilityTypeForScene("商店", null)).isTrue();
        assertThat(FacilitySearchService.isVisibleFacilityTypeForScene("洗手间", "景区")).isTrue();
    }

    private Facility facility(String name, String type, String sourceType, String destinationName, String category) {
        Destination destination = new Destination();
        destination.setName(destinationName);
        destination.setCategory(category);

        Facility facility = new Facility();
        facility.setName(name);
        facility.setFacilityType(type);
        facility.setSourceType(sourceType);
        facility.setLatitude(39.9393);
        facility.setLongitude(116.3147);
        facility.setDestination(destination);
        return facility;
    }

    private RoadEdge edge(Long from, Long to, Double length) {
        RoadEdge edge = new RoadEdge();
        edge.setU(from);
        edge.setV(to);
        edge.setLength(length);
        return edge;
    }
}
