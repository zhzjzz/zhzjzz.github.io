package com.travel.system.service;

import com.travel.system.dto.ExtractedGuidePlan;
import com.travel.system.dto.ItineraryImportCreateResponse;
import com.travel.system.dto.ItineraryImportRequest;
import com.travel.system.dto.ItineraryImportResponse;
import com.travel.system.dto.ItinerarySpotCandidateRequest;
import com.travel.system.model.Diary;
import com.travel.system.model.Itinerary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ItineraryImportService {
    private final TravelGuideExtractionService extractionService;
    private final DestinationMatchService matchService;
    private final DiaryService diaryService;
    private final ItineraryService itineraryService;
    private final ItinerarySpotCandidateService candidateService;

    public ItineraryImportService(TravelGuideExtractionService extractionService,
                                  DestinationMatchService matchService,
                                  DiaryService diaryService,
                                  ItineraryService itineraryService,
                                  ItinerarySpotCandidateService candidateService) {
        this.extractionService = extractionService;
        this.matchService = matchService;
        this.diaryService = diaryService;
        this.itineraryService = itineraryService;
        this.candidateService = candidateService;
    }

    public ItineraryImportResponse preview(ItineraryImportRequest request) {
        SourceText source = sourceText(request);
        ExtractedGuidePlan plan = extractionService.extract(source.text());
        ItineraryImportResponse response = new ItineraryImportResponse();
        response.setTitle(hasText(plan.getTitle()) ? plan.getTitle() : fallbackTitle());
        response.setSummary(plan.getSummary() == null ? "" : plan.getSummary());
        response.setSourceType(source.sourceType());
        response.setWarnings(plan.getWarnings() == null ? new ArrayList<>() : new ArrayList<>(plan.getWarnings()));

        List<ExtractedGuidePlan.Place> places = plan.getPlaces() == null ? List.of() : plan.getPlaces();
        for (ExtractedGuidePlan.Place place : places) {
            DestinationMatchService.MatchResult match = matchService.match(place);
            if (match.matched()) {
                response.getSpots().add(new ItineraryImportResponse.MatchedSpot(
                        place.getName(),
                        match.destination().getId(),
                        match.destination().getName(),
                        match.destination().getLatitude(),
                        match.destination().getLongitude(),
                        safeDay(place.getDayIndex()),
                        safeOrder(place.getOrderIndex(), response.getSpots().size() + response.getUnmatchedSpots().size()),
                        safeStay(place.getStayMinutes()),
                        Math.min(1.0, Math.max(0.0, (place.getConfidence() == null ? 0.75 : place.getConfidence()) * match.confidence())),
                        place.getReason() == null ? "" : place.getReason()
                ));
            } else {
                response.getUnmatchedSpots().add(new ItineraryImportResponse.UnmatchedSpot(
                        place.getName(),
                        safeDay(place.getDayIndex()),
                        safeOrder(place.getOrderIndex(), response.getSpots().size() + response.getUnmatchedSpots().size()),
                        match.reason()
                ));
            }
        }
        return response;
    }

    public ItineraryImportCreateResponse create(ItineraryImportRequest request) {
        ItineraryImportResponse importResult = preview(request);

        Itinerary itinerary = new Itinerary();
        itinerary.setName(hasText(importResult.getTitle()) ? importResult.getTitle() : fallbackTitle());
        itinerary.setOwner(hasText(request == null ? null : request.getOwner()) ? request.getOwner().trim() : "Imported");
        itinerary.setCollaborators("");
        itinerary.setStrategy("SHORTEST_TIME");
        itinerary.setTransportMode("walk");
        itinerary.setNotes(importResult.getSummary());
        Itinerary created = itineraryService.create(itinerary);

        for (ItineraryImportResponse.MatchedSpot spot : importResult.getSpots()) {
            ItinerarySpotCandidateRequest candidateRequest = new ItinerarySpotCandidateRequest();
            candidateRequest.setDestinationId(spot.getMatchedDestinationId());
            candidateService.addCandidate(created.getId(), candidateRequest);
        }
        return new ItineraryImportCreateResponse(created, importResult, candidateService.listMapSpots(created.getId()));
    }

    private SourceText sourceText(ItineraryImportRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import request is required");
        }
        String sourceType = normalizeSourceType(request.getSourceType());
        if ("TEXT".equals(sourceType)) {
            if (!hasText(request.getText())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guide text is required");
            }
            return new SourceText(sourceType, request.getText().trim());
        }
        if ("DIARY".equals(sourceType)) {
            if (request.getDiaryId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "diaryId is required");
            }
            Diary diary = diaryService.detail(request.getDiaryId());
            if (diary == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Diary not found: " + request.getDiaryId());
            }
            return new SourceText(sourceType, joinDiaryText(diary));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceType must be TEXT or DIARY");
    }

    private String joinDiaryText(Diary diary) {
        String title = diary.getTitle() == null ? "" : diary.getTitle().trim();
        String content = diary.getContent() == null ? "" : diary.getContent().trim();
        String combined = (title + "\n\n" + content).trim();
        if (combined.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diary content is empty");
        }
        return combined;
    }

    private String normalizeSourceType(String sourceType) {
        return sourceType == null || sourceType.isBlank() ? "TEXT" : sourceType.trim().toUpperCase();
    }

    private Integer safeDay(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private Integer safeOrder(Integer value, int fallback) {
        return value == null || value < 0 ? fallback : value;
    }

    private Integer safeStay(Integer value) {
        return value == null || value < 0 ? 120 : value;
    }

    private String fallbackTitle() {
        return "Imported Itinerary " + LocalDate.now();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record SourceText(String sourceType, String text) {
    }
}
