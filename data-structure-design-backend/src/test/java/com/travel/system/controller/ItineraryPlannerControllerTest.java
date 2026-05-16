package com.travel.system.controller;

import com.travel.system.dto.ItineraryPlannerPreviewRequest;
import com.travel.system.dto.ItineraryPlannerPreviewResponse;
import com.travel.system.service.ItineraryPlannerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItineraryPlannerControllerTest {
    @Test
    void previewDelegatesToPlannerService() {
        ItineraryPlannerService service = mock(ItineraryPlannerService.class);
        ItineraryPlannerController controller = new ItineraryPlannerController(service);

        ItineraryPlannerPreviewRequest request = new ItineraryPlannerPreviewRequest();
        ItineraryPlannerPreviewResponse response = new ItineraryPlannerPreviewResponse();
        response.setWarnings(List.of("sample"));
        when(service.preview(request)).thenReturn(response);

        assertThat(controller.preview(request).getWarnings()).containsExactly("sample");
    }
}
