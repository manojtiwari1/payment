package com.app.modules.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogInRequestDTO {

    @NotBlank(message = "Please enter your email or mobile number")
    private String userName;

    @NotBlank(message = "Please enter your password")
    private String password;

}
