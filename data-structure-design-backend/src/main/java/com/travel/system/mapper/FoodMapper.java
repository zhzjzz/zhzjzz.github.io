package com.travel.system.mapper;

import com.travel.system.model.Food;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper 接口，用于操作 {@link Food} 实体。
 *
 * <p>复杂的联表查询与多字段模糊搜索已迁移至 XML 映射文件
 * {@code resources/mapper/FoodMapper.xml}，避免在注解中编写过长 SQL。
 *
 * <p>主要功能：
 *
 * <ul>
 *   <li>查询全部美食（联表目的地）；</li>
 *   <li>按名称、菜系、店名进行多字段模糊搜索；</li>
 *   <li>插入、更新美食记录。</li>
 * </ul>
 *
 * @author 自动生成
 */
@Mapper
public interface FoodMapper {

    /**
     * 查询全部美食，并左联目的地表获取关联信息。
     *
     * @return {@link Food} 列表（含关联的目的地信息）
     */
    List<Food> findAll();

    /**
     * 根据关键字在名称、菜系、店名字段进行模糊匹配（不区分大小写）。
     *
     * @param keyword 关键字
     * @return 匹配的 {@link Food} 列表
     */
    List<Food> findByKeyword(@Param("keyword") String keyword);

    /**
     * 插入新美食记录。
     *
     * @param food 美食实体
     */
    void insert(Food food);

    /**
     * 更新已有美食记录。
     *
     * @param food 美食实体
     */
    void update(Food food);

    /**
     * 保存美食（新增或更新）。
     *
     * @param food 美食实体
     * @return 保存后的实体
     */
    default Food save(Food food) {
        if (food.getId() == null) {
            insert(food);
        } else {
            update(food);
        }
        return food;
    }
}
