package com.travel.system.repository;

import com.travel.system.model.Food;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FoodRepository {
    @Select("""
            SELECT f.id, f.name, f.cuisine, f.store_name, f.heat, f.rating, f.distance_meters, f.destination_id,
                   d.id AS destination_ref_id, d.name AS destination_name, d.scene_type AS destination_scene_type,
                   d.category AS destination_category, d.heat AS destination_heat, d.rating AS destination_rating,
                   d.description AS destination_description, d.latitude AS destination_latitude, d.longitude AS destination_longitude
            FROM food f
            LEFT JOIN destination d ON d.id = f.destination_id
            """)
    @Results(id = "foodResultMap", value = {
            @Result(column = "destination_ref_id", property = "destination.id"),
            @Result(column = "destination_name", property = "destination.name"),
            @Result(column = "destination_scene_type", property = "destination.sceneType"),
            @Result(column = "destination_category", property = "destination.category"),
            @Result(column = "destination_heat", property = "destination.heat"),
            @Result(column = "destination_rating", property = "destination.rating"),
            @Result(column = "destination_description", property = "destination.description"),
            @Result(column = "destination_latitude", property = "destination.latitude"),
            @Result(column = "destination_longitude", property = "destination.longitude")
    })
    List<Food> findAll();

    @Select("""
            SELECT f.id, f.name, f.cuisine, f.store_name, f.heat, f.rating, f.distance_meters, f.destination_id,
                   d.id AS destination_ref_id, d.name AS destination_name, d.scene_type AS destination_scene_type,
                   d.category AS destination_category, d.heat AS destination_heat, d.rating AS destination_rating,
                   d.description AS destination_description, d.latitude AS destination_latitude, d.longitude AS destination_longitude
            FROM food f
            LEFT JOIN destination d ON d.id = f.destination_id
            WHERE LOWER(f.name) LIKE CONCAT('%', LOWER(#{name}), '%')
               OR LOWER(f.cuisine) LIKE CONCAT('%', LOWER(#{cuisine}), '%')
               OR LOWER(f.store_name) LIKE CONCAT('%', LOWER(#{storeName}), '%')
            """)
    @Results(id = "foodSearchResultMap", value = {
            @Result(column = "destination_ref_id", property = "destination.id"),
            @Result(column = "destination_name", property = "destination.name"),
            @Result(column = "destination_scene_type", property = "destination.sceneType"),
            @Result(column = "destination_category", property = "destination.category"),
            @Result(column = "destination_heat", property = "destination.heat"),
            @Result(column = "destination_rating", property = "destination.rating"),
            @Result(column = "destination_description", property = "destination.description"),
            @Result(column = "destination_latitude", property = "destination.latitude"),
            @Result(column = "destination_longitude", property = "destination.longitude")
    })
    List<Food> findByNameContainingIgnoreCaseOrCuisineContainingIgnoreCaseOrStoreNameContainingIgnoreCase(
            String name, String cuisine, String storeName);

    @Insert("""
            INSERT INTO food(name, cuisine, store_name, heat, rating, distance_meters, destination_id)
            VALUES(#{name}, #{cuisine}, #{storeName}, #{heat}, #{rating}, #{distanceMeters}, #{destination.id})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Food food);

    @Update("""
            UPDATE food
            SET name = #{name},
                cuisine = #{cuisine},
                store_name = #{storeName},
                heat = #{heat},
                rating = #{rating},
                distance_meters = #{distanceMeters},
                destination_id = #{destination.id}
            WHERE id = #{id}
            """)
    int update(Food food);

    default Food save(Food food) {
        if (food.getId() == null) {
            insert(food);
        } else {
            update(food);
        }
        return food;
    }
}
