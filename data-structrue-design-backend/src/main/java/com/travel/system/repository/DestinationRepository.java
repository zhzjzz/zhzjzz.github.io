package com.travel.system.repository;

import com.travel.system.model.Destination;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DestinationRepository {
    @Select("SELECT COUNT(1) FROM destination")
    long count();

    @Select("""
            SELECT id, name, scene_type, category, heat, rating, description, latitude, longitude
            FROM destination
            """)
    List<Destination> findAll();

    @Select("""
            SELECT id, name, scene_type, category, heat, rating, description, latitude, longitude
            FROM destination
            WHERE LOWER(name) LIKE CONCAT('%', LOWER(#{name}), '%')
               OR LOWER(category) LIKE CONCAT('%', LOWER(#{category}), '%')
            """)
    List<Destination> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String category);

    @Insert("""
            INSERT INTO destination(name, scene_type, category, heat, rating, description, latitude, longitude)
            VALUES(#{name}, #{sceneType}, #{category}, #{heat}, #{rating}, #{description}, #{latitude}, #{longitude})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Destination destination);

    default Destination save(Destination destination) {
        insert(destination);
        return destination;
    }
}
