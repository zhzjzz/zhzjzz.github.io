package com.travel.system.service;

import com.travel.system.model.*;
import com.travel.system.mapper.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

//@Component
public class DataInitializer implements CommandLineRunner {

    private final DestinationMapper destinationMapper;
    private final FoodMapper foodMapper;
    private final FacilityMapper facilityMapper;
    private final DiaryMapper diaryMapper;
    private final AuthService authService;

    public DataInitializer(DestinationMapper destinationMapper,
                           FoodMapper foodMapper,
                           FacilityMapper facilityMapper,
                           DiaryMapper diaryMapper,
                           AuthService authService) {
        this.destinationMapper = destinationMapper;
        this.foodMapper = foodMapper;
        this.facilityMapper = facilityMapper;
        this.diaryMapper = diaryMapper;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        Destination bupt = ensureBuptDestination();
        authService.ensureSeedUsers();
        ensureFacilities(bupt);
        ensureFood(bupt);
        ensureDiary(bupt);
    }

    private Destination ensureBuptDestination() {
        if (destinationMapper.findAll().size() > 0) {
            return destinationMapper.findAll().stream()
                    .filter(destination -> "北京邮电大学".equals(destination.getName()))
                    .findFirst()
                    .orElseGet(() -> {
                        Destination destination = new Destination();
                        destination.setName("北京邮电大学");
                        destination.setSceneType("校园");
                        destination.setCategory("理工类高校");
                        destination.setHeat(5.0);
                        destination.setRating(4.8);
                        destination.setDescription("信息通信特色高校");
                        destination.setLatitude(39.9652);
                        destination.setLongitude(116.3511);
                        destinationMapper.save(destination);
                        return destination;
                    });
        }

        Destination destination = new Destination();
        destination.setName("北京邮电大学");
        destination.setSceneType("校园");
        destination.setCategory("理工类高校");
        destination.setHeat(5.0);
        destination.setRating(4.8);
        destination.setDescription("信息通信特色高校");
        destination.setLatitude(39.9652);
        destination.setLongitude(116.3511);
        destinationMapper.save(destination);
        return destination;
    }

    private Destination ensureMuseumDestination() {
        return destinationMapper.findAll().stream()
                .filter(destination -> "国家博物馆".equals(destination.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Destination destination = new Destination();
                    destination.setName("国家博物馆");
                    destination.setSceneType("景区");
                    destination.setCategory("5A");
                    destination.setHeat(4.7);
                    destination.setRating(4.9);
                    destination.setDescription("综合性博物馆");
                    destination.setLatitude(39.9050);
                    destination.setLongitude(116.3976);
                    destinationMapper.save(destination);
                    return destination;
                });
    }

    private void ensureFacilities(Destination bupt) {
        if (!facilityMapper.findAll().isEmpty()) {
            return;
        }

        Facility wc = new Facility();
        wc.setName("主楼卫生间");
        wc.setFacilityType("洗手间");
        wc.setDestination(bupt);
        wc.setLatitude(39.9650);
        wc.setLongitude(116.3510);
        facilityMapper.insert(wc);

        Facility cafe = new Facility();
        cafe.setName("校园咖啡角");
        cafe.setFacilityType("咖啡馆");
        cafe.setDestination(bupt);
        cafe.setLatitude(39.9653);
        cafe.setLongitude(116.3514);
        facilityMapper.insert(cafe);

        Facility canteen = new Facility();
        canteen.setName("学生食堂");
        canteen.setFacilityType("食堂");
        canteen.setDestination(bupt);
        canteen.setLatitude(39.9648);
        canteen.setLongitude(116.3508);
        facilityMapper.insert(canteen);
    }

    private void ensureFood(Destination bupt) {
        if (!foodMapper.findAll().isEmpty()) {
            return;
        }

        Food food = new Food();
        food.setName("老北京炸酱面");
        food.setCuisine("京菜");
        food.setStoreName("校园食堂一层");
        food.setRating(4.6);
        food.setDestination(bupt);
        foodMapper.insert(food);
    }

    private void ensureDiary(Destination bupt) {
        if (!diaryMapper.findAll().isEmpty()) {
            return;
        }

        Diary diary = new Diary();
        diary.setTitle("北邮春日打卡");
        diary.setContent("主楼、图书馆、银杏大道都很值得拍照。");
        diary.setMediaType("image");
        diary.setScore(4.7);
        diary.setViews(120L);
        diary.setPublishedAt(LocalDateTime.now());
        diary.setDestination(bupt);
        diaryMapper.insert(diary);
    }
}
