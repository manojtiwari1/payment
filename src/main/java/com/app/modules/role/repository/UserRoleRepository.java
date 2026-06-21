package com.app.modules.role.repository;

import com.app.modules.role.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserRole ur WHERE ur.user.id = :userId")
    void deleteByUserId(Long userId);

    boolean existsByRoleId(Long roleId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.role.id = :roleId")
    void deleteAllByRoleId(Long roleId);

}
