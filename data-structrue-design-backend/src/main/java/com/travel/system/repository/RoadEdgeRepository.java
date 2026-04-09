package com.travel.system.repository;

import com.travel.system.model.RoadEdge;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoadEdgeRepository {
    @Select("""
            SELECT id, from_node_id, to_node_id, distance_meters, ideal_speed, congestion, allowed_transport
            FROM road_edge
            WHERE from_node_id = #{fromNodeId}
            """)
    @Results(id = "roadEdgeResultMap", value = {
            @Result(column = "from_node_id", property = "fromNode.id"),
            @Result(column = "to_node_id", property = "toNode.id")
    })
    List<RoadEdge> findByFromNodeId(Long fromNodeId);

    @Insert("""
            INSERT INTO road_edge(from_node_id, to_node_id, distance_meters, ideal_speed, congestion, allowed_transport)
            VALUES(#{fromNode.id}, #{toNode.id}, #{distanceMeters}, #{idealSpeed}, #{congestion}, #{allowedTransport})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoadEdge roadEdge);

    default RoadEdge save(RoadEdge roadEdge) {
        insert(roadEdge);
        return roadEdge;
    }
}
