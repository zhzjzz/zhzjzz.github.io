package com.travel.system.repository;

import com.travel.system.model.Itinerary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ItineraryRepository {
    @Select("""
            SELECT id, name, owner, collaborators, strategy, transport_mode, notes, updated_at
            FROM itinerary
            ORDER BY updated_at DESC
            """)
    List<Itinerary> findAll();

    @Insert("""
            INSERT INTO itinerary(name, owner, collaborators, strategy, transport_mode, notes, updated_at)
            VALUES(#{name}, #{owner}, #{collaborators}, #{strategy}, #{transportMode}, #{notes}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Itinerary itinerary);

    @Update("""
            UPDATE itinerary
            SET name = #{name},
                owner = #{owner},
                collaborators = #{collaborators},
                strategy = #{strategy},
                transport_mode = #{transportMode},
                notes = #{notes},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(Itinerary itinerary);

    default Itinerary save(Itinerary itinerary) {
        if (itinerary.getId() == null) {
            insert(itinerary);
        } else {
            update(itinerary);
        }
        return itinerary;
    }
}
