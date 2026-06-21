package com.app.modules.permission.entity;

import com.app.common.enums.Status;
import com.app.common.model.AbstractAuditableModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_permission_slug", columnList = "slug")
        }
)
public class Permission extends AbstractAuditableModel {

    @Column(nullable = false)
    private String permissionName;

    @Column(nullable = false, unique = true)
    private String slug;   // USER_CREATE, USER_READ, INVENTORY_UPDATE

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_id", nullable = false)
    private Modules module;

    private Boolean master;

    @Enumerated(EnumType.STRING)
    private Status status;

    public boolean getMaster() {
        return master != null && master;
    }

}
