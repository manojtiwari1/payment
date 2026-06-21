package com.app.modules.user.repository;


import com.app.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByPhoneNumber(String mobileNo);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
                SELECT DISTINCT u FROM User u
                LEFT JOIN FETCH u.userRoles ur
                LEFT JOIN FETCH ur.role r
                LEFT JOIN FETCH r.rolePermissions rp
                LEFT JOIN FETCH rp.permission p
                WHERE u.id = :userId
            """)
    Optional<User> findUserWithRolesAndPermissions(@Param("userId") Long userId);

    @Query("""
                SELECT u
                FROM UserRole ur
                JOIN ur.user u
                WHERE ur.role.id = :roleId
                AND u.status = 'ACTIVE'
            """)
    Page<User> findUsersByRoleId(Long roleId, Pageable pageable);

    @Query("""
                SELECT u
                FROM User u
                WHERE u.id IN :userIds
                AND u.status = 'ACTIVE'
            """)
    List<User> findAllActiveByIdIn(List<Long> userIds);

    @Query("""
                SELECT u
                FROM User u
                WHERE u.id IN :userIds
                AND u.status = 'ACTIVE'
            """)
    List<User> findAllActiveByIdIn(Set<Long> userIds);

}
