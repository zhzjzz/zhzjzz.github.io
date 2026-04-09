package com.travel.system.repository;

import com.travel.system.model.Facility;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FacilityRepository {
    @Select("""
            SELECT id, name, facility_type, latitude, longitude, destination_id
            FROM facility
            """)
    @Results(id = "facilityResultMap", value = {
            @Result(column = "destination_id", property = "destination.id")
    })
    List<Facility> findAll();

    @Select("""
            SELECT id, name, facility_type, latitude, longitude, destination_id
            FROM facility
            WHERE LOWER(facility_type) LIKE CONCAT('%', LOWER(#{type}), '%')
            """)
    @Results(id = "facilitySearchResultMap", value = {
            @Result(column = "destination_id", property = "destination.id")
    })
    List<Facility> findByFacilityTypeContainingIgnoreCase(String type);

    @Insert("""
            INSERT INTO facility(name, facility_type, latitude, longitude, destination_id)
            VALUES(#{name}, #{facilityType}, #{latitude}, #{longitude}, #{destination.id})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Facility facility);

    default Facility save(Facility facility) {
        insert(facility);
        return facility;
    }
}
