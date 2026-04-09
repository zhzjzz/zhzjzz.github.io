package com.travel.system.repository;

import com.travel.system.model.Diary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DiaryRepository {
    @Select("""
            SELECT id, title, content, media_url, media_type, score, views, published_at, destination_id
            FROM diary
            """)
    @Results(id = "diaryResultMap", value = {
            @Result(column = "destination_id", property = "destination.id")
    })
    List<Diary> findAll();

    @Select("""
            SELECT id, title, content, media_url, media_type, score, views, published_at, destination_id
            FROM diary
            WHERE LOWER(title) LIKE CONCAT('%', LOWER(#{title}), '%')
            """)
    @Results(id = "diarySearchResultMap", value = {
            @Result(column = "destination_id", property = "destination.id")
    })
    List<Diary> findByTitleContainingIgnoreCase(String title);

    @Insert("""
            INSERT INTO diary(title, content, media_url, media_type, score, views, published_at, destination_id)
            VALUES(#{title}, #{content}, #{mediaUrl}, #{mediaType}, #{score}, #{views}, #{publishedAt}, #{destination.id})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Diary diary);

    default Diary save(Diary diary) {
        insert(diary);
        return diary;
    }
}
