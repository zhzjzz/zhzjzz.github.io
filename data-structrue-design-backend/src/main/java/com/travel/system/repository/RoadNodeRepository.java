package com.travel.system.repository;

import com.travel.system.model.RoadNode;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

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

    default RoadNode save(RoadNode roadNode) {
        insert(roadNode);
        return roadNode;
    }
}
