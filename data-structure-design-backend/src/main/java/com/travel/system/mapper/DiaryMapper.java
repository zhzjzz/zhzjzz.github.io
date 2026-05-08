package com.travel.system.mapper;

import com.travel.system.model.Diary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Diary 的 MyBatis Mapper。
 * SQL 定义在 resources/mapper/DiaryMapper.xml 中。
 */
@Mapper
public interface DiaryMapper {

    /** 查询全部日记。 */
    List<Diary> findAll();

    /** 按标题进行不区分大小写的模糊查询。 */
    List<Diary> findByTitleContainingIgnoreCase(@Param("title") String title);

    /** 按标题或内容进行不区分大小写的模糊查询。 */
    List<Diary> findByTitleOrContentContainingIgnoreCase(@Param("keyword") String keyword);

    /** 按主键查询日记。 */
    Diary findById(Long id);

    /** 插入日记记录。 */
    void insert(Diary diary);

    /** 更新日记记录。 */
    void update(Diary diary);

    /** 保存日记：无 id 时插入，有 id 时更新。 */
    default Diary save(Diary diary) {
        if (diary.getId() == null) {
            insert(diary);
        } else {
            update(diary);
        }
        return diary;
    }
}
