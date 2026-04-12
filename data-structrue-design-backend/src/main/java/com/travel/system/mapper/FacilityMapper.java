package com.travel.system.mapper;

import com.travel.system.model.Facility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis Mapper 接口，用于操作 {@link Facility} 实体。
 *
 * <p>复杂的 SQL 查询（联表查询、模糊匹配等）已迁移到对应的 XML 映射文件
 * {@code resources/mapper/FacilityMapper.xml}，保持 Java 接口简洁。
 *
 * <p>主要功能：
 *
 * <ul>
 *   <li>查询全部设施（联表目的地）；</li>
 *   <li>按设施类型模糊搜索；</li>
 *   <li>插入、更新设施记录。</li>
 * </ul>
 *
 * @author 自动生成
 */
@Mapper
public interface FacilityMapper {

    /**
     * 查询全部设施，并左联目的地表获取关联信息。
     *
     * @return {@link Facility} 列表（含关联的目的地信息）
     */
    List<Facility> findAll();

    /**
     * 根据设施类型进行模糊匹配（不区分大小写）。
     *
     * @param type 设施类型关键字
     * @return 匹配的 {@link Facility} 列表
     */
List<Facility> findByFacilityTypeContainingIgnoreCase(@Param("type") String type);

    /**
     * 插入新设施记录。
     *
     * @param facility 设施实体
     */
    void insert(Facility facility);

    /**
     * 更新已有设施记录。
     *
     * @param facility 设施实体
     */
    void update(Facility facility);

    /**
     * 保存设施（新增或更新）。
     *
     * @param facility 设施实体
     * @return 保存后的实体
     */
    default Facility save(Facility facility) {
        if (facility.getId() == null) {
            insert(facility);
        } else {
            update(facility);
        }
        return facility;
    }
}
