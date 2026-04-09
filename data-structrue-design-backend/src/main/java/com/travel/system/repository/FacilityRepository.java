package com.travel.system.repository;

import com.travel.system.model.Facility;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FacilityRepository {
    @Select("""
            SELECT f.id, f.name, f.facility_type, f.latitude, f.longitude, f.destination_id,
                   d.id AS destination_ref_id, d.name AS destination_name, d.scene_type AS destination_scene_type,
                   d.category AS destination_category, d.heat AS destination_heat, d.rating AS destination_rating,
                   d.description AS destination_description, d.latitude AS destination_latitude, d.longitude AS destination_longitude
            FROM facility f
            LEFT JOIN destination d ON d.id = f.destination_id
            """)
    @Results(id = "facilityResultMap", value = {
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
    List<Facility> findAll();

    @Select("""
            SELECT f.id, f.name, f.facility_type, f.latitude, f.longitude, f.destination_id,
                   d.id AS destination_ref_id, d.name AS destination_name, d.scene_type AS destination_scene_type,
                   d.category AS destination_category, d.heat AS destination_heat, d.rating AS destination_rating,
                   d.description AS destination_description, d.latitude AS destination_latitude, d.longitude AS destination_longitude
            FROM facility f
            LEFT JOIN destination d ON d.id = f.destination_id
            WHERE LOWER(f.facility_type) LIKE CONCAT('%', LOWER(#{type}), '%')
            """)
    @Results(id = "facilitySearchResultMap", value = {
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
    List<Facility> findByFacilityTypeContainingIgnoreCase(String type);

    @Insert("""
            INSERT INTO facility(name, facility_type, latitude, longitude, destination_id)
            VALUES(#{name}, #{facilityType}, #{latitude}, #{longitude}, #{destination.id})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Facility facility);

    @Update("""
            UPDATE facility
            SET name = #{name},
                facility_type = #{facilityType},
                latitude = #{latitude},
                longitude = #{longitude},
                destination_id = #{destination.id}
            WHERE id = #{id}
            """)
    int update(Facility facility);

    default Facility save(Facility facility) {
        if (facility.getId() == null) {
            insert(facility);
        } else {
            update(facility);
        }
        return facility;
    }
}
