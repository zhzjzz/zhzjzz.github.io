package com.travel.system.service;

import com.travel.system.model.Destination;
import com.travel.system.model.Food;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * {@code RecommendationService} 提供基于热度与评分的 Top‑K 推荐算法。
 *
 * <p>实现思路：
 *
 * <ul>
 *   <li>使用小顶堆（{@link PriorityQueue}）维护当前最好的 {@code k} 条记录，避免对全量数据进行完整排序；</li>
 *   <li>为目的地和美食分别定义不同的综合评分函数 {@link #destinationScore(Destination)} 与 {@link #foodScore(Food)}，兼顾评分与关联目的地热度等因素；</li>
 *   <li>最终将堆中的元素转为列表并按照评分降序返回。</li>
 * </ul>
 *
 *
 */
@Service
public class RecommendationService {

    public enum DestinationRankingMode {
        RATING,
        HEAT,
        COMPOSITE
    }

    /**
     * 返回目的地的 Top‑K 推荐列表。
     *
     * @param data 待排序的目的地集合
     * @param k    需要返回的记录数
     * @return 按综合评分降序排列的 {@link Destination} 列表
     */
    public List<Destination> topKDestinations(List<Destination> data, int k) {
        return topKDestinations(data, k, DestinationRankingMode.COMPOSITE);
    }

    /**

     * 按照评分、热度或综合评分计算目的地排序分数，并返回前 k 个推荐结果。

     */
    public List<Destination> topKDestinations(List<Destination> data, int k, DestinationRankingMode mode) {
        DestinationRankingMode rankingMode = mode == null ? DestinationRankingMode.COMPOSITE : mode;
        ScoreContext context = ScoreContext.from(data);
        PriorityQueue<Destination> heap = new PriorityQueue<>(
                Comparator.comparingDouble(destination -> destinationScore(destination, rankingMode, context))
        );
        for (Destination d : data) {
            heap.offer(d);
            if (heap.size() > k) {
                heap.poll();
            }
        }
        return heap.stream()
                .sorted((a, b) -> Double.compare(
                        destinationScore(b, rankingMode, context),
                        destinationScore(a, rankingMode, context)
                ))
                .toList();
    }

    /**
     * 返回美食的 Top‑K 推荐列表。
     *
     * @param data 待排序的美食集合
     * @param k    需要返回的记录数
     * @return 按综合评分降序排列的 {@link Food} 列表
     */
    public List<Food> topKFood(List<Food> data, int k) {
        PriorityQueue<Food> heap = new PriorityQueue<>(Comparator.comparingDouble(this::foodScore));
        for (Food f : data) {
            heap.offer(f);
            if (heap.size() > k) {
                heap.poll();
            }
        }
        return heap.stream()
                .sorted((a, b) -> Double.compare(foodScore(b), foodScore(a)))
                .toList();
    }

    /**
     * 目的地综合评分：热度占比 0.6，评分占比 0.4。
     *
     * @param d 目的地实体
     * @return 计算后的分数，范围 0~∞
     */
    private double destinationScore(Destination d) {
        return safe(d.getHeat()) * 0.6 + safe(d.getRating()) * 0.4;
    }

    /**

     * 根据推荐模式计算单个目的地的排序分数；综合模式会同时考虑归一化评分和热度。

     */
    private double destinationScore(Destination d, DestinationRankingMode mode, ScoreContext context) {
        return switch (mode) {
            case RATING -> safe(d.getRating());
            case HEAT -> safe(d.getHeat());
            case COMPOSITE -> normalized(safe(d.getHeat()), context.minHeat(), context.maxHeat()) * 0.6
                    + normalized(safe(d.getRating()), context.minRating(), context.maxRating()) * 0.4;
        };
    }

    /**
     * 美食综合评分：评分 80% + 关联目的地热度 20%。
     *
     * @param f 美食实体
     * @return 计算后的分数
     */
    private double foodScore(Food f) {
        Double destinationHeat = f.getDestination() == null ? null : f.getDestination().getHeat();
        return safe(f.getRating()) * 0.8 + safe(destinationHeat) * 0.2;
    }

    /**
     * 防止 {@code null} 值导致 NPE 的工具方法。
     *
     * @param v 可能为 {@code null} 的 {@link Double}
     * @return 若为 {@code null} 则返回 0，否则返回原值
     */
    private double safe(Double v) {
        return v == null ? 0 : v;
    }

    /**

     * 将数值按最小值和最大值归一化到 0 到 1 区间；当区间无差异时返回 1。

     */
    private double normalized(double value, double min, double max) {
        if (Double.compare(max, min) == 0) {
            return 0;
        }
        return (value - min) / (max - min);
    }

    /**

     * 保存推荐算法使用的评分和热度范围，避免为每个目的地重复扫描全量数据。

     */
    private record ScoreContext(double minHeat, double maxHeat, double minRating, double maxRating) {
        static ScoreContext from(List<Destination> data) {
            double minHeat = data.stream().mapToDouble(d -> d.getHeat() == null ? 0 : d.getHeat()).min().orElse(0);
            double maxHeat = data.stream().mapToDouble(d -> d.getHeat() == null ? 0 : d.getHeat()).max().orElse(0);
            double minRating = data.stream().mapToDouble(d -> d.getRating() == null ? 0 : d.getRating()).min().orElse(0);
            double maxRating = data.stream().mapToDouble(d -> d.getRating() == null ? 0 : d.getRating()).max().orElse(0);
            return new ScoreContext(minHeat, maxHeat, minRating, maxRating);
        }
    }
}
