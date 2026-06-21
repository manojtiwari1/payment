package com.app.modules.permission.entity;

import com.app.common.enums.Status;
import com.app.common.model.AbstractAuditableModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(
        name = "module",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_module_slug", columnList = "slug"),
                @Index(name = "idx_module_parent", columnList = "parent_id")
        }
)
public class Modules extends AbstractAuditableModel {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Modules parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private Set<Modules> children = new HashSet<>();

    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    private Status status;

}
