package com.travel.system.mapper;

import com.travel.system.model.Diary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper for {@link Diary} entity.
 *
 * Provides CRUD operations. The actual SQL statements are defined in
 * {@code resources/mapper/DiaryMapper.xml}.
 *
 * @author 自动生成
 */
@Mapper
public interface DiaryMapper {

    /**
     * Retrieve all diary entries.
     *
     * @return list of {@link Diary}
     */
    List<Diary> findAll();

    /**
     * Find diaries by title (case‑insensitive fuzzy match).
     *
     * @param title title keyword
     * @return matching diaries
     */
    List<Diary> findByTitleContainingIgnoreCase(@Param("title") String title);

    /**
     * Find diaries by title or content (case‑insensitive fuzzy match).
     *
     * @param keyword search keyword
     * @return matching diaries
     */
    List<Diary> findByTitleOrContentContainingIgnoreCase(@Param("keyword") String keyword);

    /**
     * Find diary by primary key.
     *
     * @param id diary id
     * @return diary or {@code null}
     */
    Diary findById(Long id);

    /**
     * Insert a new diary record.
     *
     * @param diary diary entity
     */
    void insert(Diary diary);

    /**
     * Update an existing diary record.
     *
     * @param diary diary entity
     */
    void update(Diary diary);

    /**
     * Save (insert or update) a diary entity.
     *
     * @param diary diary to persist
     * @return persisted diary
     */
    default Diary save(Diary diary) {
        if (diary.getId() == null) {
            insert(diary);
        } else {
            update(diary);
        }
        return diary;
    }
}
