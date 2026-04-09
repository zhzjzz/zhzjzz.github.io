package com.travel.system.service;

import com.travel.system.model.Destination;
import com.travel.system.model.Food;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class RecommendationService {

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

    private double destinationScore(Destination d) {
        return safe(d.getHeat()) * 0.6 + safe(d.getRating()) * 0.4;
    }

    private double foodScore(Food f) {
        double distance = f.getDistanceMeters() == null ? 1000.0 : f.getDistanceMeters();
        return safe(f.getHeat()) * 0.45 + safe(f.getRating()) * 0.45 + Math.max(0, (1000 - distance) / 1000) * 0.1;
    }

    private double safe(Double v) {
        return v == null ? 0 : v;
    }
}
