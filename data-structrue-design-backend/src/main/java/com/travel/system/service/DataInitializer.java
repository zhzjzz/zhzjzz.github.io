package com.travel.system.service;

import com.travel.system.model.*;
import com.travel.system.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DestinationRepository destinationRepository;
    private final FoodRepository foodRepository;
    private final FacilityRepository facilityRepository;
    private final DiaryRepository diaryRepository;
    private final RoadNodeRepository roadNodeRepository;
    private final RoadEdgeRepository roadEdgeRepository;

    public DataInitializer(DestinationRepository destinationRepository,
                           FoodRepository foodRepository,
                           FacilityRepository facilityRepository,
                           DiaryRepository diaryRepository,
                           RoadNodeRepository roadNodeRepository,
                           RoadEdgeRepository roadEdgeRepository) {
        this.destinationRepository = destinationRepository;
        this.foodRepository = foodRepository;
        this.facilityRepository = facilityRepository;
        this.diaryRepository = diaryRepository;
        this.roadNodeRepository = roadNodeRepository;
        this.roadEdgeRepository = roadEdgeRepository;
    }

    @Override
    public void run(String... args) {
        if (destinationRepository.count() > 0) {
            return;
        }

        Destination bupt = new Destination();
        bupt.setName("北京邮电大学");
        bupt.setSceneType("校园");
        bupt.setCategory("理工类高校");
        bupt.setHeat(5.0);
        bupt.setRating(4.8);
        bupt.setDescription("信息通信特色高校");
        destinationRepository.save(bupt);

        Destination museum = new Destination();
        museum.setName("国家博物馆");
        museum.setSceneType("景区");
        museum.setCategory("5A");
        museum.setHeat(4.7);
        museum.setRating(4.9);
        museum.setDescription("综合性博物馆");
        destinationRepository.save(museum);

        Facility wc = new Facility();
        wc.setName("主楼卫生间");
        wc.setFacilityType("洗手间");
        wc.setDestination(bupt);
        wc.setLatitude(39.965);
        wc.setLongitude(116.351);
        facilityRepository.save(wc);

        Food food = new Food();
        food.setName("老北京炸酱面");
        food.setCuisine("京菜");
        food.setStoreName("校园食堂一层");
        food.setHeat(4.5);
        food.setRating(4.6);
        food.setDistanceMeters(350.0);
        food.setDestination(bupt);
        foodRepository.save(food);

        Diary diary = new Diary();
        diary.setTitle("北邮春日打卡");
        diary.setContent("主楼、图书馆、银杏大道都很值得拍照。");
        diary.setMediaType("image");
        diary.setScore(4.7);
        diary.setViews(120L);
        diary.setPublishedAt(LocalDateTime.now());
        diary.setDestination(bupt);
        diaryRepository.save(diary);

        RoadNode gate = new RoadNode();
        gate.setName("北门");
        gate.setNodeType("入口");
        roadNodeRepository.save(gate);

        RoadNode library = new RoadNode();
        library.setName("图书馆");
        library.setNodeType("建筑");
        roadNodeRepository.save(library);

        RoadEdge edge = new RoadEdge();
        edge.setFromNode(gate);
        edge.setToNode(library);
        edge.setDistanceMeters(500.0);
        edge.setIdealSpeed(75.0);
        edge.setCongestion(0.8);
        edge.setAllowedTransport("walk,bike,shuttle");
        roadEdgeRepository.save(edge);

        RoadEdge reverse = new RoadEdge();
        reverse.setFromNode(library);
        reverse.setToNode(gate);
        reverse.setDistanceMeters(500.0);
        reverse.setIdealSpeed(75.0);
        reverse.setCongestion(0.9);
        reverse.setAllowedTransport("walk,bike,shuttle");
        roadEdgeRepository.save(reverse);
    }
}
