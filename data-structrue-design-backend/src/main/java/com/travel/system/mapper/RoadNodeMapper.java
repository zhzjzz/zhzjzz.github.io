package com.travel.system.mapper;

import com.travel.system.model.RoadNode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MyBatis Mapper for {@link RoadNode} entity.
 *
 * Provides CRUD operations. SQL statements are defined in
 * {@code resources/mapper/RoadNodeMapper.xml}.
 *
 * @author 自动生成
 */
@Mapper
public interface RoadNodeMapper {

    /**
     * Retrieve all road nodes.
     *
     * @return list of {@link RoadNode}
     */
    List<RoadNode> findAll();

    /**
     * Find road node by primary key.
     *
     * @param id node id
     * @return {@link RoadNode} or {@code null}
     */
    RoadNode findById(Long id);

    /**
     * Insert a new road node.
     *
     * @param roadNode node entity
     */
    void insert(RoadNode roadNode);

    /**
     * Update an existing road node.
     *
     * @param roadNode node entity
     */
    void update(RoadNode roadNode);

    /**
     * Save (insert or update) a road node.
     *
     * @param roadNode node to persist
     * @return persisted node
     */
    default RoadNode save(RoadNode roadNode) {
        if (roadNode.getId() == null) {
            insert(roadNode);
        } else {
            update(roadNode);
        }
        return roadNode;
    }
}
