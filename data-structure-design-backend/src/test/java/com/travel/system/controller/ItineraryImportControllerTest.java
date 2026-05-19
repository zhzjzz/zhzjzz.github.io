package com.travel.system.controller;

import com.travel.system.dto.ItineraryImportCreateResponse;
import com.travel.system.dto.ItineraryImportRequest;
import com.travel.system.dto.ItineraryImportResponse;
import com.travel.system.model.Itinerary;
import com.travel.system.service.ItineraryImportService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItineraryImportControllerTest {
    private final FakeImportService service = new FakeImportService();
    private final ItineraryImportController controller = new ItineraryImportController(service);

    @Test
    void delegatesPreview() {
        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");

        ItineraryImportResponse response = controller.preview(request);

        assertThat(service.previewRequest).isSameAs(request);
        assertThat(response.getTitle()).isEqualTo("Preview");
    }

    @Test
    void delegatesCreate() {
        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");

        ItineraryImportCreateResponse response = controller.create(request);

        assertThat(service.createRequest).isSameAs(request);
        assertThat(response.getItinerary().getId()).isEqualTo(77L);
    }

    private static class FakeImportService extends ItineraryImportService {
        private ItineraryImportRequest previewRequest;
        private ItineraryImportRequest createRequest;

        FakeImportService() {
            super(null, null, null, null, null);
        }

        @Override
        public ItineraryImportResponse preview(ItineraryImportRequest request) {
            previewRequest = request;
            ItineraryImportResponse response = new ItineraryImportResponse();
            response.setTitle("Preview");
            return response;
        }

        @Override
        public ItineraryImportCreateResponse create(ItineraryImportRequest request) {
            createRequest = request;
            Itinerary itinerary = new Itinerary();
            itinerary.setId(77L);
            return new ItineraryImportCreateResponse(itinerary, new ItineraryImportResponse(), List.of());
        }
    }
}
