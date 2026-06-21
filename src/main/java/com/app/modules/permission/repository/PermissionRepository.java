package com.app.modules.permission.repository;

import com.app.common.enums.Status;
import com.app.modules.permission.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

    @Query(value = "SELECT DISTINCT p.slug FROM Permission p " +
                "JOIN RolePermission rp ON rp.permission.id = p.id " +
                "JOIN Role r ON r.id = rp.role.id " +
                "JOIN UserRole ur ON ur.role.id = r.id "+
                "WHERE ur.user.id = :userId AND p.status = 'ACTIVE' AND r.status = 'ACTIVE'")
    Set<String> findPermissionSlugsByUserId(@Param("userId") Long userId);

    @Query("SELECT p.slug FROM Permission p WHERE p.status = 'ACTIVE'")
    Set<String> findAllActivePermissionSlugs();

    List<Permission> findByStatus(Status status);


}