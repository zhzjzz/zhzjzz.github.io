package com.travel.system.mapper;

import com.travel.system.model.Destination;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis 映射接口，用于操作 {@link Destination} 实体。
 *
 * <p>本接口将复杂的查询语句提取到对应的 XML 映射文件（``DestinationMapper.xml``）中，
 * 以保持 Java 代码的简洁并遵循 “SQL 不写在注解里” 的约束。
 *
 * <p>常用的查询方法：
 *
 * <ul>
 *   <li>{@link #findAll()} – 查询全部目的地；</li>
 *   <li>{@link #findByKeyword(String)} – 按名称或类别进行模糊搜索；</li>
 *   <li>{@link #save(Destination)} – 插入或更新目的地记录（使用 MyBatis 的 {@code insert} / {@code update} 语句）。</li>
 * </ul>
 *
 * @author 自动生成
 */
@Mapper
public interface DestinationMapper {

    /**
     * 查询所有目的地。
     *
     * @return {@link Destination} 列表
     */
    List<Destination> findAll();

    /**
     * 根据关键字在 {@code name} 或 {@code category} 字段进行模糊匹配（不区分大小写）。
     *
     * @param keyword 关键字
     * @return 匹配的 {@link Destination}
     */
    List<Destination> findByKeyword(@Param("keyword") String keyword);

    /**
     * 插入新目的地记录。
     *
     * @param destination 目的地实体
     */
    int insert(Destination destination);

    /**
     * 更新已有目的地记录。
     *
     * @param destination 目的地实体
     */
    int update(Destination destination);

    /**
     * 保存（插入或更新）目的地实体。
     *
     * @param destination 要保存的实体
     * @return 保存后的实体
     */
    default Destination save(Destination destination) {
        if (destination.getId() == null) {
            insert(destination);
        } else {
            update(destination);
        }
        return destination;
    }
}
