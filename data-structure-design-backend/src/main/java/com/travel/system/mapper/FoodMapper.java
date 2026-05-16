package com.travel.system.mapper;

import com.travel.system.model.Food;
import com.travel.system.dto.FoodPlaceAnchor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Food 的 MyBatis Mapper。
 * SQL 定义在 resources/mapper/FoodMapper.xml 中。
 */
@Mapper
public interface FoodMapper {

    /** 查询全部美食，包含关联目的地信息。 */
    List<Food> findAll();

    /** 按名称、菜系或店名进行模糊查询。 */
    List<Food> findByKeyword(@Param("keyword") String keyword);

    List<String> findCuisines();

    List<FoodPlaceAnchor> findPlaceAnchors();

    /** 插入美食记录。 */
    void insert(Food food);

    /** 更新美食记录。 */
    void update(Food food);

    /** 保存美食：无 id 时插入，有 id 时更新。 */
    default Food save(Food food) {
        if (food.getId() == null) {
            insert(food);
        } else {
            update(food);
        }
        return food;
    }
}
