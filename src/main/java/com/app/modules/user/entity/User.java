package com.app.modules.user.entity;

import com.app.common.enums.AuthProvider;
import com.app.common.enums.Status;
import com.app.common.model.AbstractAuditableModel;
import com.app.modules.picture.model.Picture;
import com.app.modules.role.entity.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name="users",
        indexes = {
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_email", columnList = "email")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@SQLDelete(sql="UPDATE User set status='DELETED', deleted_at=NOW() where id= ?")
@SQLRestriction("status <> 'DELETED'")
public class User extends AbstractAuditableModel {

    private String firstName;

    private String lastName;

    @Column(unique = true)
    private String phoneNumber;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    /**
     * BCrypt-hashed password. Nullable: users provisioned via an external provider
     * (SSO/SYSTEM) may have no local password and therefore cannot password-login.
     */
    @Column(name = "password")
    private String password;

    private Instant lastLoggedInAt;

    /**
     * Merchant this user acts on behalf of (e.g. "M123"). Set for MERCHANT-role users;
     * null for ADMIN/operational users. Carried as a JWT claim and used to scope payments.
     */
    @Column(name = "merchant_code")
    private String merchantCode;

    @Enumerated(EnumType.STRING)
    private Status status;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.SYSTEM;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "picture_id")
    private Picture picture;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Transient
    private Long childCount;


}
