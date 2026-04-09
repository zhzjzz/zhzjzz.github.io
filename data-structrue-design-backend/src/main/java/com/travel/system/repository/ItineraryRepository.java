package com.travel.system.repository;

import com.travel.system.model.Itinerary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

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

    default Itinerary save(Itinerary itinerary) {
        insert(itinerary);
        return itinerary;
    }
}
