package com.travel.system.repository;

import com.travel.system.model.RoadNode;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RoadNodeRepository {
    @Select("""
            SELECT id, name, node_type, latitude, longitude
            FROM road_node
            """)
    List<RoadNode> findAll();

    @Insert("""
            INSERT INTO road_node(name, node_type, latitude, longitude)
            VALUES(#{name}, #{nodeType}, #{latitude}, #{longitude})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoadNode roadNode);

    @Update("""
            UPDATE road_node
            SET name = #{name},
                node_type = #{nodeType},
                latitude = #{latitude},
                longitude = #{longitude}
            WHERE id = #{id}
            """)
    int update(RoadNode roadNode);

    default RoadNode save(RoadNode roadNode) {
        if (roadNode.getId() == null) {
            insert(roadNode);
        } else {
            update(roadNode);
        }
        return roadNode;
    }
}
