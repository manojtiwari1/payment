package com.app.modules.role.entity;

import com.app.common.enums.Status;
import com.app.common.model.AbstractAuditableModel;
import com.app.modules.permission.entity.Permission;
import com.app.modules.permission.entity.RolePermission;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_role_slug", columnList = "slug")
        }
)
public class Role extends AbstractAuditableModel {

    @Column(nullable = false, length = 100)
    private String roleName;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;   // ADMIN, SUPER_ADMIN, INVENTORY_MANAGER

    @Enumerated(EnumType.STRING)
    private Status status;

    private Boolean master;

    @Column(nullable = false)
    private Integer level;

    @OneToMany(
            mappedBy = "role",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<RolePermission> rolePermissions = new HashSet<>();

    public boolean getMaster() {
        return master != null && master;
    }

    // Replace full permission set
    public void replacePermissions(Set<Permission> permissions) {
        this.rolePermissions.clear();
        permissions.forEach(permission -> {
            RolePermission rp = new RolePermission();
            rp.setRole(this);
            rp.setPermission(permission);
            this.rolePermissions.add(rp);
        });
    }

    public void updatePermissions(Set<Permission> permissions) {

        Set<Long> newPermissionIds = permissions.stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        // Remove ones not present
        this.rolePermissions.removeIf(rp ->
                !newPermissionIds.contains(rp.getPermission().getId())
        );

        // Add new ones
        Set<Long> existingIds = this.rolePermissions.stream()
                .map(rp -> rp.getPermission().getId())
                .collect(Collectors.toSet());

        permissions.stream()
                .filter(p -> !existingIds.contains(p.getId()))
                .forEach(permission -> {
                    RolePermission rp = new RolePermission();
                    rp.setRole(this);
                    rp.setPermission(permission);
                    this.rolePermissions.add(rp);
                });
    }

}
