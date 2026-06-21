package com.app.common.model;

import com.app.common.model.AbstractIdentifiableModel;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base class for all auditable JPA entities.
 *
 * <p>Populated automatically by Spring Data JPA auditing. The {@code createdBy} and
 * {@code updatedBy} columns store the Keycloak {@code preferred_username} claim (or "SYSTEM"
 * when no authenticated principal is present).
 *
 * <p><strong>Schema note:</strong> {@code created_by} and {@code updated_by} are {@code VARCHAR}
 * columns. If you are migrating from a prior schema that stored {@code BIGINT} user IDs,
 * run an ALTER TABLE migration before deploying this version.
 */
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
@Setter
public class AbstractAuditableModel extends AbstractIdentifiableModel {

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 255)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 255)
    private String deletedBy;
}
