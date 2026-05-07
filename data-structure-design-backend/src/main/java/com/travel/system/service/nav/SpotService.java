package com.travel.system.service.nav;

import com.travel.system.mapper.nav.SpotMapper;
import com.travel.system.model.nav.Spot;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class SpotService {

    private final SpotMapper spotMapper;

    public SpotService(SpotMapper spotMapper) {
        this.spotMapper = spotMapper;
    }

    public List<Spot> search(String keyword, String category, String sortBy, Integer limit) {
        List<Spot> spots;
        if (keyword != null && !keyword.isBlank()) {
            spots = spotMapper.findByKeyword(keyword.trim());
        } else if (category != null && !category.isBlank()) {
            spots = spotMapper.findByCategory(category.trim());
        } else {
            spots = spotMapper.findAll();
        }

        int k = (limit != null && limit > 0) ? limit : spots.size();

        if ("hotness".equalsIgnoreCase(sortBy)) {
            return topKByHotness(spots, k);
        } else if ("rating".equalsIgnoreCase(sortBy)) {
            return topKByRating(spots, k);
        }
        return spots;
    }

    public Spot getById(Long spotId) {
        return spotMapper.findBySpotId(spotId);
    }

    public List<Spot> listAll() {
        return spotMapper.findAll();
    }

    /**
     * 使用小顶堆取 hotness Top-K，不进行全排序。
     * 用户自行实现。
     */
    public List<Spot> topKByHotness(List<Spot> spots, int k) {
        if (k <= 0 || spots.isEmpty()) {
            return List.of();
        }
        PriorityQueue<Spot> heap = new PriorityQueue<>(
                Comparator.comparingInt(s -> s.getHotness() != null ? s.getHotness() : 0));
        for (Spot s : spots) {
            heap.offer(s);
            if (heap.size() > k) {
                heap.poll();
            }
        }
        return heap.stream()
                .sorted((a, b) -> Integer.compare(
                        b.getHotness() != null ? b.getHotness() : 0,
                        a.getHotness() != null ? a.getHotness() : 0))
                .toList();
    }

    /**
     * 使用小顶堆取 rating Top-K，不进行全排序。
     * 用户自行实现。
     */
    public List<Spot> topKByRating(List<Spot> spots, int k) {
        if (k <= 0 || spots.isEmpty()) {
            return List.of();
        }
        PriorityQueue<Spot> heap = new PriorityQueue<>(
                Comparator.comparingDouble(s -> s.getRating() != null ? s.getRating() : 0.0));
        for (Spot s : spots) {
            heap.offer(s);
            if (heap.size() > k) {
                heap.poll();
            }
        }
        return heap.stream()
                .sorted((a, b) -> Double.compare(
                        b.getRating() != null ? b.getRating() : 0.0,
                        a.getRating() != null ? a.getRating() : 0.0))
                .toList();
    }
}
