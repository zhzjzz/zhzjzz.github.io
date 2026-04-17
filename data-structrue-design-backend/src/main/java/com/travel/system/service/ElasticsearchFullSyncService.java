package com.travel.system.service;

import com.travel.system.mapper.*;
import com.travel.system.model.*;
import com.travel.system.repository.*;
import com.travel.system.search.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ElasticsearchFullSyncService {

    private final DestinationMapper destinationMapper;
    private final DiaryMapper diaryMapper;
    private final FacilityMapper facilityMapper;
    private final FoodMapper foodMapper;
    private final ItineraryMapper itineraryMapper;
    private final RoadNodeMapper roadNodeMapper;
    private final RoadEdgeMapper roadEdgeMapper;
    private final UserAccountMapper userAccountMapper;

    private final DestinationSearchRepository destinationSearchRepository;
    private final DiarySearchRepository diarySearchRepository;
    private final FacilitySearchRepository facilitySearchRepository;
    private final FoodSearchRepository foodSearchRepository;
    private final ItinerarySearchRepository itinerarySearchRepository;
    private final RoadNodeSearchRepository roadNodeSearchRepository;
    private final RoadEdgeSearchRepository roadEdgeSearchRepository;
    private final UserAccountSearchRepository userAccountSearchRepository;

    public ElasticsearchFullSyncService(DestinationMapper destinationMapper,
                                        DiaryMapper diaryMapper,
                                        FacilityMapper facilityMapper,
                                        FoodMapper foodMapper,
                                        ItineraryMapper itineraryMapper,
                                        RoadNodeMapper roadNodeMapper,
                                        RoadEdgeMapper roadEdgeMapper,
                                        UserAccountMapper userAccountMapper,
                                        ObjectProvider<DestinationSearchRepository> destinationSearchRepositoryProvider,
                                        ObjectProvider<DiarySearchRepository> diarySearchRepositoryProvider,
                                        ObjectProvider<FacilitySearchRepository> facilitySearchRepositoryProvider,
                                        ObjectProvider<FoodSearchRepository> foodSearchRepositoryProvider,
                                        ObjectProvider<ItinerarySearchRepository> itinerarySearchRepositoryProvider,
                                        ObjectProvider<RoadNodeSearchRepository> roadNodeSearchRepositoryProvider,
                                        ObjectProvider<RoadEdgeSearchRepository> roadEdgeSearchRepositoryProvider,
                                        ObjectProvider<UserAccountSearchRepository> userAccountSearchRepositoryProvider) {
        this.destinationMapper = destinationMapper;
        this.diaryMapper = diaryMapper;
        this.facilityMapper = facilityMapper;
        this.foodMapper = foodMapper;
        this.itineraryMapper = itineraryMapper;
        this.roadNodeMapper = roadNodeMapper;
        this.roadEdgeMapper = roadEdgeMapper;
        this.userAccountMapper = userAccountMapper;
        this.destinationSearchRepository = destinationSearchRepositoryProvider.getIfAvailable();
        this.diarySearchRepository = diarySearchRepositoryProvider.getIfAvailable();
        this.facilitySearchRepository = facilitySearchRepositoryProvider.getIfAvailable();
        this.foodSearchRepository = foodSearchRepositoryProvider.getIfAvailable();
        this.itinerarySearchRepository = itinerarySearchRepositoryProvider.getIfAvailable();
        this.roadNodeSearchRepository = roadNodeSearchRepositoryProvider.getIfAvailable();
        this.roadEdgeSearchRepository = roadEdgeSearchRepositoryProvider.getIfAvailable();
        this.userAccountSearchRepository = userAccountSearchRepositoryProvider.getIfAvailable();
    }

    public Map<String, Object> syncAllTables() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("destination", syncDestinations());
        result.put("diary", syncDiaries());
        result.put("facility", syncFacilities());
        result.put("food", syncFoods());
        result.put("itinerary", syncItineraries());
        result.put("road_node", syncRoadNodes());
        result.put("road_edge", syncRoadEdges());
        result.put("user_account", syncUserAccounts());
        return result;
    }

    private Map<String, Object> syncDestinations() {
        List<Destination> rows = destinationMapper.findAll();
        if (destinationSearchRepository == null) {
            return summary(false, rows.size(), 0);
        }
        List<DestinationDocument> docs = rows.stream().map(DestinationDocument::new).collect(Collectors.toList());
        destinationSearchRepository.saveAll(docs);
        return summary(true, rows.size(), docs.size());
    }

    private Map<String, Object> syncDiaries() {
        List<Diary> rows = diaryMapper.findAll();
        if (diarySearchRepository == null) {
            return summary(false, rows.size(), 0);
        }
        List<DiaryDocument> docs = rows.stream().map(this::toDiaryDocument).collect(Collectors.toList());
        diarySearchRepository.saveAll(docs);
        return summary(true, rows.size(), docs.size());
    }

    private Map<String, Object> syncFacilities() {
        List<Facility> rows = facilityMapper.findAll();
        if (facilitySearchRepository == null) {
            return summary(false, rows.size(), 0);
        }
        List<FacilityDocument> docs = rows.stream().map(this::toFacilityDocument).collect(Collectors.toList());
        facilitySearchRepository.saveAll(docs);
        return summary(true, rows.size(), docs.size());
    }

    private Map<String, Object> syncFoods() {
        List<Food> rows = foodMapper.findAll();
        if (foodSearchRepository == null) {
            return summary(false, rows.size(), 0);
        }
        List<FoodDocument> docs = rows.stream().map(this::toFoodDocument).collect(Collectors.toList());
        foodSearchRepository.saveAll(docs);
        return summary(true, rows.size(), docs.size());
    }

    private Map<String, Object> syncItineraries() {
        List<Itinerary> rows = itineraryMapper.findAll();
        if (itinerarySearchRepository == null) {
            return summary(false, rows.size(), 0);
        }
        List<ItineraryDocument> docs = rows.stream().map(this::toItineraryDocument).collect(Collectors.toList());
        itinerarySearchRepository.saveAll(docs);
        return summary(true, rows.size(), docs.size());
    }

    private Map<String, Object> syncRoadNodes() {
        List<RoadNode> rows = roadNodeMapper.findAll();
        if (roadNodeSearchRepository == null) {
            return summary(false, rows.size(), 0);
        }
        List<RoadNodeDocument> docs = rows.stream().map(this::toRoadNodeDocument).collect(Collectors.toList());
        roadNodeSearchRepository.saveAll(docs);
        return summary(true, rows.size(), docs.size());
    }

    private Map<String, Object> syncRoadEdges() {
        List<RoadEdge> rows = roadEdgeMapper.findAll();
        if (roadEdgeSearchRepository == null) {
            return summary(false, rows.size(), 0);
        }
        List<RoadEdgeDocument> docs = rows.stream().map(this::toRoadEdgeDocument).collect(Collectors.toList());
        roadEdgeSearchRepository.saveAll(docs);
        return summary(true, rows.size(), docs.size());
    }

    private Map<String, Object> syncUserAccounts() {
        List<UserAccount> rows = userAccountMapper.findAll();
        if (userAccountSearchRepository == null) {
            return summary(false, rows.size(), 0);
        }
        List<UserAccountDocument> docs = rows.stream().map(this::toUserAccountDocument).collect(Collectors.toList());
        userAccountSearchRepository.saveAll(docs);
        return summary(true, rows.size(), docs.size());
    }

    private Map<String, Object> summary(boolean esAvailable, int dbCount, int indexedCount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("esAvailable", esAvailable);
        row.put("dbCount", dbCount);
        row.put("indexedCount", indexedCount);
        return row;
    }

    private DiaryDocument toDiaryDocument(Diary diary) {
        DiaryDocument doc = new DiaryDocument();
        doc.setId(String.valueOf(diary.getId()));
        doc.setTitle(diary.getTitle());
        doc.setContent(diary.getContent());
        if (diary.getDestination() != null) {
            doc.setDestinationName(diary.getDestination().getName());
        }
        return doc;
    }

    private FacilityDocument toFacilityDocument(Facility facility) {
        FacilityDocument doc = new FacilityDocument();
        doc.setId(String.valueOf(facility.getId()));
        doc.setName(facility.getName());
        doc.setFacilityType(facility.getFacilityType());
        doc.setLatitude(facility.getLatitude());
        doc.setLongitude(facility.getLongitude());
        if (facility.getDestination() != null) {
            doc.setDestinationName(facility.getDestination().getName());
        }
        return doc;
    }

    private FoodDocument toFoodDocument(Food food) {
        FoodDocument doc = new FoodDocument();
        doc.setId(String.valueOf(food.getId()));
        doc.setName(food.getName());
        doc.setCuisine(food.getCuisine());
        doc.setStoreName(food.getStoreName());
        doc.setHeat(food.getHeat());
        doc.setRating(food.getRating());
        if (food.getDestination() != null) {
            doc.setDestinationName(food.getDestination().getName());
        }
        return doc;
    }

    private ItineraryDocument toItineraryDocument(Itinerary itinerary) {
        ItineraryDocument doc = new ItineraryDocument();
        doc.setId(String.valueOf(itinerary.getId()));
        doc.setName(itinerary.getName());
        doc.setOwner(itinerary.getOwner());
        doc.setCollaborators(itinerary.getCollaborators());
        doc.setStrategy(itinerary.getStrategy());
        doc.setTransportMode(itinerary.getTransportMode());
        doc.setNotes(itinerary.getNotes());
        doc.setUpdatedAt(itinerary.getUpdatedAt());
        return doc;
    }

    private RoadNodeDocument toRoadNodeDocument(RoadNode node) {
        RoadNodeDocument doc = new RoadNodeDocument();
        doc.setId(String.valueOf(node.getId()));
        doc.setName(node.getName());
        doc.setNodeType(node.getNodeType());
        doc.setLatitude(node.getLatitude());
        doc.setLongitude(node.getLongitude());
        return doc;
    }

    private RoadEdgeDocument toRoadEdgeDocument(RoadEdge edge) {
        RoadEdgeDocument doc = new RoadEdgeDocument();
        doc.setId(String.valueOf(edge.getId()));
        doc.setDistanceMeters(edge.getDistanceMeters());
        doc.setIdealSpeed(edge.getIdealSpeed());
        doc.setCongestion(edge.getCongestion());
        doc.setAllowedTransport(edge.getAllowedTransport());
        if (edge.getFromNode() != null) {
            doc.setFromNodeId(edge.getFromNode().getId());
            doc.setFromNodeName(edge.getFromNode().getName());
        }
        if (edge.getToNode() != null) {
            doc.setToNodeId(edge.getToNode().getId());
            doc.setToNodeName(edge.getToNode().getName());
        }
        return doc;
    }

    private UserAccountDocument toUserAccountDocument(UserAccount userAccount) {
        UserAccountDocument doc = new UserAccountDocument();
        doc.setId(String.valueOf(userAccount.getId()));
        doc.setUsername(userAccount.getUsername());
        doc.setDisplayName(userAccount.getDisplayName());
        doc.setInterests(userAccount.getInterests());
        return doc;
    }
}
