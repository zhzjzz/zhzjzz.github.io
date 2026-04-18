package com.travel.system.mapper;

import com.travel.system.model.Itinerary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis Mapper for {@link Itinerary} entity.
 *
 * Provides basic CRUD operations. SQL statements are defined in
 * {@code resources/mapper/ItineraryMapper.xml}.
 *
 * @author 自动生成
 */
@Mapper
public interface ItineraryMapper {

    /**
     * Retrieve all itineraries.
     *
     * @return list of {@link Itinerary}
     */
    List<Itinerary> findAll();

    /**
     * Find itinerary by primary key.
     *
     * @param id itinerary id
     * @return itinerary or {@code null}
     */
    Itinerary findById(Long id);

    /**
     * Insert a new itinerary.
     *
     * @param itinerary itinerary entity
     */
    void insert(Itinerary itinerary);

    /**
     * Update an existing itinerary.
     *
     * @param itinerary itinerary entity
     */
    void update(Itinerary itinerary);

    /**
     * 带乐观锁条件更新：仅当更新时间匹配时更新。
     *
     * @param itinerary        行程实体
     * @param expectedUpdatedAt 客户端期望的更新时间
     * @return 受影响行数
     */
    int updateIfUnchanged(@Param("itinerary") Itinerary itinerary,
                          @Param("expectedUpdatedAt") LocalDateTime expectedUpdatedAt);

    /**
     * Save (insert or update) an itinerary.
     *
     * @param itinerary itinerary to persist
     * @return persisted itinerary
     */
    default Itinerary save(Itinerary itinerary) {
        if (itinerary.getId() == null) {
            insert(itinerary);
        } else {
            update(itinerary);
        }
        return itinerary;
    }
}
