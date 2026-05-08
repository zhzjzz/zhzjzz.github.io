package com.travel.system.mapper;

import com.travel.system.model.Itinerary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Itinerary 的 MyBatis Mapper。
 * SQL 定义在 resources/mapper/ItineraryMapper.xml 中。
 */
@Mapper
public interface ItineraryMapper {

    /** 查询全部行程。 */
    List<Itinerary> findAll();

    /** 按主键查询行程。 */
    Itinerary findById(Long id);

    /** 插入行程记录。 */
    void insert(Itinerary itinerary);

    /** 更新行程记录。 */
    void update(Itinerary itinerary);

    /** 乐观锁更新：仅当更新时间与客户端期望值一致时更新。 */
    int updateIfUnchanged(@Param("itinerary") Itinerary itinerary,
                          @Param("expectedUpdatedAt") LocalDateTime expectedUpdatedAt);

    /** 保存行程：无 id 时插入，有 id 时更新。 */
    default Itinerary save(Itinerary itinerary) {
        if (itinerary.getId() == null) {
            insert(itinerary);
        } else {
            update(itinerary);
        }
        return itinerary;
    }
}
