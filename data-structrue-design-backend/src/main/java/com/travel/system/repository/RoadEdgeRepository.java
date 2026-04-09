package com.travel.system.repository;

import com.travel.system.model.RoadEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoadEdgeRepository extends JpaRepository<RoadEdge, Long> {
    List<RoadEdge> findByFromNodeId(Long fromNodeId);
}
