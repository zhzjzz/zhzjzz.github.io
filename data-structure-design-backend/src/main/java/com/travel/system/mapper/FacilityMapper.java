package com.travel.system.mapper;

import com.travel.system.model.Facility;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Facility 的 MyBatis Mapper。
 * SQL 定义在 resources/mapper/FacilityMapper.xml 中。
 */
@Mapper
public interface FacilityMapper {

    /** 查询全部设施，包含关联目的地信息。 */
    List<Facility> findAll();

    /** 按设施类型、名称或所属地点进行模糊查询。 */
    List<Facility> findByFacilityTypeContainingIgnoreCase(@Param("type") String type);

    /** 按 ID 列表查询设施。 */
    List<Facility> findByIds(@Param("ids") List<Long> ids);

    /** 插入设施记录。 */
    void insert(Facility facility);

    /** 更新设施记录。 */
    void update(Facility facility);

    /** 保存设施：无 id 时插入，有 id 时更新。 */
    default Facility save(Facility facility) {
        if (facility.getId() == null) {
            insert(facility);
        } else {
            update(facility);
        }
        return facility;
    }
}
