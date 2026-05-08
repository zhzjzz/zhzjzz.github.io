package com.travel.system.mapper;

import com.travel.system.model.Destination;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Destination 的 MyBatis Mapper。
 * SQL 定义在 resources/mapper/DestinationMapper.xml 中。
 */
@Mapper
public interface DestinationMapper {

    /** 查询全部目的地。 */
    List<Destination> findAll();

    /** 按名称、分类或关键词进行模糊搜索。 */
    List<Destination> findByKeyword(@Param("keyword") String keyword);

    /** 插入目的地记录。 */
    int insert(Destination destination);

    /** 更新目的地记录。 */
    int update(Destination destination);

    /** 保存目的地：无 id 时插入，有 id 时更新。 */
    default Destination save(Destination destination) {
        if (destination.getId() == null) {
            insert(destination);
        } else {
            update(destination);
        }
        return destination;
    }
}
