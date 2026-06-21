package com.app.modules.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {

    private String currentPassword;

    @NotBlank(message = "Mandatory field")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password must be strong, Use combination of number, special character, small and capital letters")
    private String newPassword;

    private Boolean loggedOutFromAllDevices = Boolean.TRUE;
}

