package com.travel.system.repository;

import com.travel.system.model.RoadEdge;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RoadEdgeRepository {
    @Select("""
            SELECT e.id, e.from_node_id, e.to_node_id, e.distance_meters, e.ideal_speed, e.congestion, e.allowed_transport,
                   fn.id AS from_ref_id, fn.name AS from_name, fn.node_type AS from_node_type, fn.latitude AS from_latitude, fn.longitude AS from_longitude,
                   tn.id AS to_ref_id, tn.name AS to_name, tn.node_type AS to_node_type, tn.latitude AS to_latitude, tn.longitude AS to_longitude
            FROM road_edge e
            LEFT JOIN road_node fn ON fn.id = e.from_node_id
            LEFT JOIN road_node tn ON tn.id = e.to_node_id
            WHERE e.from_node_id = #{fromNodeId}
            """)
    @Results(id = "roadEdgeResultMap", value = {
            @Result(column = "from_ref_id", property = "fromNode.id"),
            @Result(column = "from_name", property = "fromNode.name"),
            @Result(column = "from_node_type", property = "fromNode.nodeType"),
            @Result(column = "from_latitude", property = "fromNode.latitude"),
            @Result(column = "from_longitude", property = "fromNode.longitude"),
            @Result(column = "to_ref_id", property = "toNode.id"),
            @Result(column = "to_name", property = "toNode.name"),
            @Result(column = "to_node_type", property = "toNode.nodeType"),
            @Result(column = "to_latitude", property = "toNode.latitude"),
            @Result(column = "to_longitude", property = "toNode.longitude")
    })
    List<RoadEdge> findByFromNodeId(Long fromNodeId);

    @Insert("""
            INSERT INTO road_edge(from_node_id, to_node_id, distance_meters, ideal_speed, congestion, allowed_transport)
            VALUES(#{fromNode.id}, #{toNode.id}, #{distanceMeters}, #{idealSpeed}, #{congestion}, #{allowedTransport})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoadEdge roadEdge);

    @Update("""
            UPDATE road_edge
            SET from_node_id = #{fromNode.id},
                to_node_id = #{toNode.id},
                distance_meters = #{distanceMeters},
                ideal_speed = #{idealSpeed},
                congestion = #{congestion},
                allowed_transport = #{allowedTransport}
            WHERE id = #{id}
            """)
    int update(RoadEdge roadEdge);

    default RoadEdge save(RoadEdge roadEdge) {
        if (roadEdge.getId() == null) {
            insert(roadEdge);
        } else {
            update(roadEdge);
        }
        return roadEdge;
    }
}
