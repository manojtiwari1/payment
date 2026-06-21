package com.app.modules.user.response;

import com.app.common.enums.Status;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
public class UserResponseDTO {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String username;

    private String phoneNumber;

    private Status status;

    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;

    private String profilePictureUrl;

    private Set<String> roles;

    private Set<String> roleSlugs;

    private Set<String> permissions;

}
