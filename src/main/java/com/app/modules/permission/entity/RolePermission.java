package com.app.modules.permission.entity;

import com.app.common.model.AbstractAuditableModel;
import com.app.modules.permission.entity.Permission;
import com.app.modules.role.entity.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(
        name = "role_permission",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"role_id", "permission_id"})
        },
        indexes = {
                @Index(name = "idx_role_permission_role", columnList = "role_id"),
                @Index(name = "idx_role_permission_permission", columnList = "permission_id")
        }
)
public class RolePermission extends AbstractAuditableModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id")
    private Permission permission;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolePermission that)) return false;

        return role != null && permission != null &&
                role.getId().equals(that.role.getId()) &&
                permission.getId().equals(that.permission.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                role != null ? role.getId() : null,
                permission != null ? permission.getId() : null
        );
    }

}
