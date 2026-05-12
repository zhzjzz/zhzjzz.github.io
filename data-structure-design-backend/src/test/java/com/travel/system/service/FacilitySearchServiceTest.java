package com.travel.system.service;

import com.travel.system.dto.FacilityQueryResult;
import com.travel.system.mapper.FacilityMapper;
import com.travel.system.model.Destination;
import com.travel.system.model.Facility;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
