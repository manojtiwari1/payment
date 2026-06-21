package com.app.modules.user.request;

import com.app.common.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRequestDTO {

    private Long roleId;

    @NotBlank(message = "Name is mandatory field")
    private String firstName;

    private String lastName;

    @NotBlank(message = "Email cannot be empty")
    @Email(regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")
    private String email;

    @Pattern(regexp = "^\\d{6,15}$", message = "Please enter a valid mobile number")
    private String mobileNo;

    @NotBlank(message = "Password is mandatory field")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
            message = "Password is not strong enough, Use combination of number, special character and alphabet")
    private String password;

    @NotBlank(message = "Confirm password is mandatory field")
    private String confirmPassword;

    /** Optional merchant code (e.g. "M123") for MERCHANT-role users; leave null for admin/ops users. */
    private String merchantCode;

    private Status status = Status.ACTIVE;

}

