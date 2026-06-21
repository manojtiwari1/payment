package com.app.modules.role.repository;

import com.app.common.enums.Status;
import com.app.modules.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    boolean existsByRoleNameIgnoreCase(String roleName);

    boolean existsBySlugIgnoreCase(String slug);

    @Query(value = "SELECT DISTINCT r.slug FROM Role r " +
                "JOIN UserRole ur ON ur.role.id = r.id "+
                "WHERE ur.user.id = :userId "+
                "AND r.status = 'ACTIVE' ")
    Set<String> findRoleSlugsByUserId(@Param("userId") Long userId);

    @Query("""
                SELECT COUNT(r) > 0
                FROM UserRole ur
                JOIN ur.role r
                WHERE ur.user.id = :userId
                AND r.master = true
            """)
    boolean existsMasterRoleByUserId(Long userId);

    @Query("SELECT COALESCE(MAX(r.level), 0) FROM Role r")
    Integer findMaxLevel();

    List<Role> findAllByStatusAndLevelGreaterThanEqual(Status status, Integer level);

}
