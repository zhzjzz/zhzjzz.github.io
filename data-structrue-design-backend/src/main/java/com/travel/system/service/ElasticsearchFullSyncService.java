package com.travel.system.service;

import com.travel.system.mapper.*;
import com.travel.system.model.*;
import com.travel.system.repository.*;
import com.travel.system.search.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
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
    private final UserAccountMapper userAccountMapper;

    private final DestinationSearchRepository destinationSearchRepository;
    private final DiarySearchRepository diarySearchRepository;
    private final FacilitySearchRepository facilitySearchRepository;
    private final FoodSearchRepository foodSearchRepository;
    private final ItinerarySearchRepository itinerarySearchRepository;
    private final UserAccountSearchRepository userAccountSearchRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchFullSyncService(DestinationMapper destinationMapper,
                                        DiaryMapper diaryMapper,
                                        FacilityMapper facilityMapper,
                                        FoodMapper foodMapper,
                                        ItineraryMapper itineraryMapper,
                                        UserAccountMapper userAccountMapper,
                                        ObjectProvider<DestinationSearchRepository> destinationSearchRepositoryProvider,
                                        ObjectProvider<DiarySearchRepository> diarySearchRepositoryProvider,
                                        ObjectProvider<FacilitySearchRepository> facilitySearchRepositoryProvider,
                                        ObjectProvider<FoodSearchRepository> foodSearchRepositoryProvider,
                                        ObjectProvider<ItinerarySearchRepository> itinerarySearchRepositoryProvider,
                                        ObjectProvider<UserAccountSearchRepository> userAccountSearchRepositoryProvider,
                                        ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider) {
        this.destinationMapper = destinationMapper;
        this.diaryMapper = diaryMapper;
        this.facilityMapper = facilityMapper;
        this.foodMapper = foodMapper;
        this.itineraryMapper = itineraryMapper;
        this.userAccountMapper = userAccountMapper;
        this.destinationSearchRepository = destinationSearchRepositoryProvider.getIfAvailable();
        this.diarySearchRepository = diarySearchRepositoryProvider.getIfAvailable();
        this.facilitySearchRepository = facilitySearchRepositoryProvider.getIfAvailable();
        this.foodSearchRepository = foodSearchRepositoryProvider.getIfAvailable();
        this.itinerarySearchRepository = itinerarySearchRepositoryProvider.getIfAvailable();
        this.userAccountSearchRepository = userAccountSearchRepositoryProvider.getIfAvailable();
        this.elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();
    }

    /**
     * 删除所有 ES 索引及其中的数据。
     */
    public Map<String, Object> deleteAllIndices() {
        Map<String, Object> result = new LinkedHashMap<>();

        if (elasticsearchOperations == null) {
            result.put("error", "Elasticsearch 不可用，无法删除");
            return result;
        }

        Class<?>[] documentClasses = {
                DestinationDocument.class,
                DiaryDocument.class,
                FacilityDocument.class,
                FoodDocument.class,
                ItineraryDocument.class,
                UserAccountDocument.class
        };
        for (Class<?> clazz : documentClasses) {
            try {
                IndexOperations indexOps = elasticsearchOperations.indexOps(clazz);
                boolean existed = indexOps.exists();
                if (existed) {
                    indexOps.delete();
                    result.put(clazz.getSimpleName(), " deleted OK");
                } else {
                    result.put(clazz.getSimpleName(), " index not existed, skipped");
                }
            } catch (Exception e) {
                result.put(clazz.getSimpleName(), " error: " + e.getMessage());
            }
        }
        result.put("message", "所有 ES 索引已删除");
        return result;
    }

    /**
     * 全量同步：先删除旧索引、按新的 @Field 注解重建索引映射，再批量写入数据。
     */
    public Map<String, Object> syncAllTables() {
        Map<String, Object> result = new LinkedHashMap<>();

        if (elasticsearchOperations == null) {
            result.put("error", "Elasticsearch 不可用，无法同步");
            return result;
        }

        // 删除旧索引并重建（使用 @Field 注解中的新 mapping，包含 IK 分词器配置）
        result.put("reindex", rebuildIndices());

        // 全量写入数据
        result.put("destination", syncDestinations());
        result.put("diary", syncDiaries());
        result.put("facility", syncFacilities());
        result.put("food", syncFoods());
        result.put("itinerary", syncItineraries());
        result.put("user_account", syncUserAccounts());
        return result;
    }

    /**
     * 删除旧索引并按新的 Document 注解重建索引映射
     */
    private Map<String, Object> rebuildIndices() {
        Map<String, Object> reindexResults = new LinkedHashMap<>();
        Class<?>[] documentClasses = {
                DestinationDocument.class,
                DiaryDocument.class,
                FacilityDocument.class,
                FoodDocument.class,
                ItineraryDocument.class,
                UserAccountDocument.class
        };
        for (Class<?> clazz : documentClasses) {
            try {
                IndexOperations indexOps = elasticsearchOperations.indexOps(clazz);
                if (indexOps.exists()) {
                    indexOps.delete();
                }
                indexOps.createWithMapping();
                reindexResults.put(clazz.getSimpleName(), " rebuilt OK");
            } catch (Exception e) {
                reindexResults.put(clazz.getSimpleName(), " error: " + e.getMessage());
            }
        }
        return reindexResults;
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

    private UserAccountDocument toUserAccountDocument(UserAccount userAccount) {
        UserAccountDocument doc = new UserAccountDocument();
        doc.setId(String.valueOf(userAccount.getId()));
        doc.setUsername(userAccount.getUsername());
        doc.setDisplayName(userAccount.getDisplayName());
        doc.setInterests(userAccount.getInterests());
        return doc;
    }
}
