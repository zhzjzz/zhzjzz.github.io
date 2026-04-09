package com.travel.system.repository;

import com.travel.system.model.Food;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;

import java.util.List;

@Mapper
public interface FoodRepository {
    @Select("""
            SELECT id, name, cuisine, store_name, heat, rating, distance_meters, destination_id
            FROM food
            """)
    @Results(id = "foodResultMap", value = {
            @Result(column = "destination_id", property = "destination.id")
    })
    List<Food> findAll();

    @Select("""
            SELECT id, name, cuisine, store_name, heat, rating, distance_meters, destination_id
            FROM food
            WHERE LOWER(name) LIKE CONCAT('%', LOWER(#{name}), '%')
               OR LOWER(cuisine) LIKE CONCAT('%', LOWER(#{cuisine}), '%')
               OR LOWER(store_name) LIKE CONCAT('%', LOWER(#{storeName}), '%')
            """)
    @Results(id = "foodSearchResultMap", value = {
            @Result(column = "destination_id", property = "destination.id")
    })
    List<Food> findByNameContainingIgnoreCaseOrCuisineContainingIgnoreCaseOrStoreNameContainingIgnoreCase(
            String name, String cuisine, String storeName);

    @Insert("""
            INSERT INTO food(name, cuisine, store_name, heat, rating, distance_meters, destination_id)
            VALUES(#{name}, #{cuisine}, #{storeName}, #{heat}, #{rating}, #{distanceMeters}, #{destination.id})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Food food);

    default Food save(Food food) {
        insert(food);
        return food;
    }
}
