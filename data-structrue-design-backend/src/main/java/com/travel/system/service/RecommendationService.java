package com.travel.system.service;

import com.travel.system.model.Destination;
import com.travel.system.model.Food;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 推荐服务。
 * 使用小顶堆实现 Top-K 非完全排序，避免对全量数据进行完整排序。
 */
@Service
public class RecommendationService {

    /**
     * 目的地 Top-K 推荐。
     */
    public List<Destination> topKDestinations(List<Destination> data, int k) {
        PriorityQueue<Destination> heap = new PriorityQueue<>(Comparator.comparingDouble(this::destinationScore));
        for (Destination d : data) {
            heap.offer(d);
            if (heap.size() > k) {
                heap.poll();
            }
        }
        return heap.stream().sorted((a, b) -> Double.compare(destinationScore(b), destinationScore(a))).toList();
    }

    /**
     * 美食 Top-K 推荐。
     */
    public List<Food> topKFood(List<Food> data, int k) {
        PriorityQueue<Food> heap = new PriorityQueue<>(Comparator.comparingDouble(this::foodScore));
        for (Food f : data) {
            heap.offer(f);
            if (heap.size() > k) {
                heap.poll();
            }
        }
        return heap.stream().sorted((a, b) -> Double.compare(foodScore(b), foodScore(a))).toList();
    }

    /**
     * 目的地综合评分：热度权重 0.6，评分权重 0.4。
     */
    private double destinationScore(Destination d) {
        return safe(d.getHeat()) * 0.6 + safe(d.getRating()) * 0.4;
    }

    /**
     * 美食综合评分：热度/评分为主，距离为辅（距离越近加分越高）。
     */
    private double foodScore(Food f) {
        double distance = f.getDistanceMeters() == null ? 1000.0 : f.getDistanceMeters();
        return safe(f.getHeat()) * 0.45 + safe(f.getRating()) * 0.45 + Math.max(0, (1000 - distance) / 1000) * 0.1;
    }

    /**
     * 空值兜底，防止评分字段为空导致 NPE。
     */
    private double safe(Double v) {
        return v == null ? 0 : v;
    }
}
