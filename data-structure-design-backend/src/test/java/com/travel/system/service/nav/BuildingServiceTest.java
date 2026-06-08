package com.travel.system.service.nav;

import com.travel.system.mapper.nav.BuildingMapper;
import com.travel.system.model.nav.Building;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildingServiceTest {

    private final BuildingMapper buildingMapper = mock(BuildingMapper.class);
    private final BuildingService service = new BuildingService(buildingMapper);

    @Test
    void listBySpotHidesInternalSchoolBuildings() {
        Building teaching = building(1L, "雍和宫", "雍和宫_教学楼1", "教学楼");
        Building dormitory = building(2L, "雍和宫", "雍和宫_宿舍楼1", "宿舍楼");
        Building shop = building(3L, "雍和宫", "雍和宫_商店1", "商店");
        Building toilet = building(4L, "雍和宫", "雍和宫_洗手间1", "洗手间");
        when(buildingMapper.findBySpotName("雍和宫")).thenReturn(List.of(teaching, dormitory, shop, toilet));

        List<Building> results = service.listBySpot("雍和宫");

        assertThat(results)
                .extracting(Building::getName)
                .containsExactly("雍和宫_商店1", "雍和宫_洗手间1");
    }

    private Building building(Long id, String spotName, String name, String type) {
        return new Building(id, spotName, name, type, id + 100);
    }
}
