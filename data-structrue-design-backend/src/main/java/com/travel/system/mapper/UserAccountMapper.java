package com.travel.system.mapper;

import com.travel.system.model.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * MyBatis Mapper for {@link UserAccount}.
 * Replaces JPA UserAccountRepository.
 */
@Mapper
public interface UserAccountMapper {

    /**
     * 查询用户总数。
     *
     * @return 用户数量
     */
    long count();

    /**
     * 根据用户名查找用户。
     *
     * @param username 用户名
     * @return 用户Optional
     */
    Optional<UserAccount> findByUsername(@Param("username") String username);

    /**
     * 检查用户名是否存在。
     *
     * @param username 用户名
     * @return true if exists
     */
    boolean existsByUsername(@Param("username") String username);

    /**
     * 根据ID查找用户。
     *
     * @param id 用户ID
     * @return 用户Optional
     */
    Optional<UserAccount> findById(@Param("id") Long id);

    /**
     * 插入新用户。
     *
     * @param userAccount 用户实体
     */
    void insert(UserAccount userAccount);

    /**
     * 更新用户信息。
     *
     * @param userAccount 用户实体
     */
    void update(UserAccount userAccount);

    /**
     * 保存用户（新增或更新）。
     *
     * @param userAccount 用户实体
     * @return 保存后的用户
     */
    default UserAccount save(UserAccount userAccount) {
        if (userAccount.getId() == null) {
            insert(userAccount);
        } else {
            update(userAccount);
        }
        return userAccount;
    }
}
