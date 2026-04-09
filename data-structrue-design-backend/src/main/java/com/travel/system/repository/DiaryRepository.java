package com.travel.system.repository;

import com.travel.system.model.Diary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DiaryRepository {
    @Select("""
            SELECT dr.id, dr.title, dr.content, dr.media_url, dr.media_type, dr.score, dr.views, dr.published_at, dr.destination_id,
                   d.id AS destination_ref_id, d.name AS destination_name, d.scene_type AS destination_scene_type,
                   d.category AS destination_category, d.heat AS destination_heat, d.rating AS destination_rating,
                   d.description AS destination_description, d.latitude AS destination_latitude, d.longitude AS destination_longitude
            FROM diary dr
            LEFT JOIN destination d ON d.id = dr.destination_id
            """)
    @Results(id = "diaryResultMap", value = {
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
    List<Diary> findAll();

    @Select("""
            SELECT dr.id, dr.title, dr.content, dr.media_url, dr.media_type, dr.score, dr.views, dr.published_at, dr.destination_id,
                   d.id AS destination_ref_id, d.name AS destination_name, d.scene_type AS destination_scene_type,
                   d.category AS destination_category, d.heat AS destination_heat, d.rating AS destination_rating,
                   d.description AS destination_description, d.latitude AS destination_latitude, d.longitude AS destination_longitude
            FROM diary dr
            LEFT JOIN destination d ON d.id = dr.destination_id
            WHERE LOWER(dr.title) LIKE CONCAT('%', LOWER(#{title}), '%')
            """)
    @Results(id = "diarySearchResultMap", value = {
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
    List<Diary> findByTitleContainingIgnoreCase(String title);

    @Insert("""
            INSERT INTO diary(title, content, media_url, media_type, score, views, published_at, destination_id)
            VALUES(#{title}, #{content}, #{mediaUrl}, #{mediaType}, #{score}, #{views}, #{publishedAt}, #{destination.id})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Diary diary);

    @Update("""
            UPDATE diary
            SET title = #{title},
                content = #{content},
                media_url = #{mediaUrl},
                media_type = #{mediaType},
                score = #{score},
                views = #{views},
                published_at = #{publishedAt},
                destination_id = #{destination.id}
            WHERE id = #{id}
            """)
    int update(Diary diary);

    default Diary save(Diary diary) {
        if (diary.getId() == null) {
            insert(diary);
        } else {
            update(diary);
        }
        return diary;
    }
}
