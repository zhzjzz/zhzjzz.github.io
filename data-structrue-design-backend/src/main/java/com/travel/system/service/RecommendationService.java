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
 * <p>该实现是演示性质，实际项目可根据业务需求引入更复杂的机器学习或协同过滤模型。
 *
 * @author 自动生成
 */
@Service
public class RecommendationService {

    /**
     * 返回目的地的 Top‑K 推荐列表。
     *
     * @param data 待排序的目的地集合
     * @param k    需要返回的记录数
     * @return 按综合评分降序排列的 {@link Destination} 列表
     */
    public List<Destination> topKDestinations(List<Destination> data, int k) {
        PriorityQueue<Destination> heap = new PriorityQueue<>(Comparator.comparingDouble(this::destinationScore));
        for (Destination d : data) {
            heap.offer(d);
            if (heap.size() > k) {
                heap.poll();
            }
        }
        return heap.stream()
                .sorted((a, b) -> Double.compare(destinationScore(b), destinationScore(a)))
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
}
