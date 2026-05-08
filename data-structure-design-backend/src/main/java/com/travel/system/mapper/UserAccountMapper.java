package com.travel.system.mapper;

import com.travel.system.model.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * UserAccount 的 MyBatis Mapper。
 * SQL 定义在 resources/mapper/UserAccountMapper.xml 中。
 */
@Mapper
public interface UserAccountMapper {

    /** 查询用户总数。 */
    long count();

    /** 查询全部用户。 */
    List<UserAccount> findAll();

    /** 按用户名查询用户。 */
    Optional<UserAccount> findByUsername(@Param("username") String username);

    /** 判断用户名是否存在。 */
    boolean existsByUsername(@Param("username") String username);

    /** 按主键查询用户。 */
    Optional<UserAccount> findById(@Param("id") Long id);

    /** 插入用户记录。 */
    void insert(UserAccount userAccount);

    /** 更新用户记录。 */
    void update(UserAccount userAccount);

    /** 保存用户：无 id 时插入，有 id 时更新。 */
    default UserAccount save(UserAccount userAccount) {
        if (userAccount.getId() == null) {
            insert(userAccount);
        } else {
            update(userAccount);
        }
        return userAccount;
    }
}
