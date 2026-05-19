package com.travel.system.service;

import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.dto.ItineraryImportCreateResponse;
import com.travel.system.dto.ItineraryImportRequest;
import com.travel.system.dto.ItineraryImportResponse;
import com.travel.system.dto.ItineraryMapSpot;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.model.Destination;
import com.travel.system.model.Diary;
import com.travel.system.model.Itinerary;
import com.travel.system.model.ItinerarySpotCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItineraryImportServiceTest {
    private final FakeExtractionService extractionService = new FakeExtractionService();
    private final FakeDestinationMatchService matchService = new FakeDestinationMatchService();
    private final FakeDiaryService diaryService = new FakeDiaryService();
    private final FakeItineraryService itineraryService = new FakeItineraryService();
    private final FakeCandidateService candidateService = new FakeCandidateService();
    private final ItineraryImportService service = new ItineraryImportService(
            extractionService,
            matchService,
            diaryService,
            itineraryService,
            candidateService);

    @Test
    void previewsTextSourceWithMatchedAndUnmatchedSpots() {
        extractionService.plan.getPlaces().add(place("The Bund", 0));
        extractionService.plan.getPlaces().add(place("Unknown Cafe", 1));
        matchService.destinations.add(destination(10L, "The Bund"));

        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");
        request.setText("The Bund then Unknown Cafe");

        ItineraryImportResponse response = service.preview(request);

        assertThat(response.getTitle()).isEqualTo("Imported Route");
        assertThat(response.getSourceType()).isEqualTo("TEXT");
        assertThat(response.getSpots()).hasSize(1);
        assertThat(response.getSpots().get(0).getMatchedDestinationId()).isEqualTo(10L);
        assertThat(response.getUnmatchedSpots()).hasSize(1);
    }

    @Test
    void previewsDiarySourceByLoadingDiaryContent() {
        Diary diary = new Diary();
        diary.setId(7L);
        diary.setTitle("Diary title");
        diary.setContent("The Bund");
        diaryService.diary = diary;
        extractionService.plan.getPlaces().add(place("The Bund", 0));
        matchService.destinations.add(destination(10L, "The Bund"));

        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("DIARY");
        request.setDiaryId(7L);

        ItineraryImportResponse response = service.preview(request);

        assertThat(extractionService.lastText).isEqualTo("Diary title\n\nThe Bund");
        assertThat(response.getSpots()).hasSize(1);
    }

    @Test
    void createsItineraryAndAddsMatchedCandidates() {
        extractionService.plan.getPlaces().add(place("The Bund", 0));
        matchService.destinations.add(destination(10L, "The Bund"));

        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");
        request.setText("The Bund");
        request.setOwner("Zhou");

        ItineraryImportCreateResponse response = service.create(request);

        assertThat(response.getItinerary().getName()).isEqualTo("Imported Route");
        assertThat(response.getItinerary().getOwner()).isEqualTo("Zhou");
        assertThat(candidateService.addedDestinationIds).containsExactly(10L);
        assertThat(response.getPlannerSpots()).hasSize(1);
    }

    @Test
    void rejectsCreateWhenNoDestinationsMatched() {
        extractionService.plan.getPlaces().add(place("Unknown Cafe", 0));

        ItineraryImportRequest request = new ItineraryImportRequest();
        request.setSourceType("TEXT");
        request.setText("Unknown Cafe");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No matched destinations");
    }

    private ExtractedGuidePlan.Place place(String name, int orderIndex) {
        return new ExtractedGuidePlan.Place(name, 1, orderIndex, 120, "", 0.8);
    }

    private Destination destination(Long id, String name) {
        return new Destination(id, name, "scenic", "spot", 1.0, 4.5, "", 31.0 + id, 121.0 + id);
    }

    private static class FakeExtractionService extends TravelGuideExtractionService {
        private final ExtractedGuidePlan plan = new ExtractedGuidePlan("Imported Route", "Summary", new ArrayList<>(), new ArrayList<>());
        private String lastText;

        FakeExtractionService() {
            super(null, null, null);
        }

        @Override
        public ExtractedGuidePlan extract(String text) {
            lastText = text;
            return plan;
        }
    }

    private static class FakeDestinationMatchService extends DestinationMatchService {
        private final List<Destination> destinations = new ArrayList<>();

        FakeDestinationMatchService() {
            super(null);
        }

        @Override
        public MatchResult match(ExtractedGuidePlan.Place place) {
            return destinations.stream()
                    .filter(destination -> destination.getName().equals(place.getName()))
                    .findFirst()
                    .map(destination -> MatchResult.matched(place, destination, 0.95))
                    .orElseGet(() -> MatchResult.unmatched(place, "No matching destination found"));
        }
    }

    private static class FakeDiaryService extends DiaryService {
        private Diary diary;

        FakeDiaryService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public Diary detail(Long id) {
            return diary;
        }
    }

    private static class FakeItineraryService extends ItineraryService {
        private long nextId = 100L;

        FakeItineraryService() {
            super(null, null);
        }

        @Override
        public Itinerary create(Itinerary itinerary) {
            itinerary.setId(nextId++);
            return itinerary;
        }
    }

    private static class FakeCandidateService extends ItinerarySpotCandidateService {
        private final List<Long> addedDestinationIds = new ArrayList<>();

        FakeCandidateService() {
            super(null, null, null);
        }

        @Override
        public ItinerarySpotCandidate addCandidate(Long itineraryId, ItinerarySpotCandidateRequest request) {
            addedDestinationIds.add(request.getDestinationId());
            ItinerarySpotCandidate candidate = new ItinerarySpotCandidate();
            candidate.setItineraryId(itineraryId);
            candidate.setDestinationId(request.getDestinationId());
            return candidate;
        }

        @Override
        public List<ItineraryMapSpot> listMapSpots(Long itineraryId) {
            return addedDestinationIds.stream().map(id -> {
                ItineraryMapSpot spot = new ItineraryMapSpot();
                spot.setDestinationId(id);
                spot.setSpotId(id);
                spot.setSpotName("Imported " + id);
                spot.setLatitude(31.0 + id);
                spot.setLongitude(121.0 + id);
                return spot;
            }).toList();
        }
    }
}
