package com.parkease.auth_service.dtos;

import com.parkease.auth_service.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SignUpDto {
    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 50,
            message = "Full name must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z ]+$",
            message = "Name should contain only alphabets and spaces")
    private String fullName;


    @NotBlank(message = "Email is required")
    @Email(message = "Enter valid email format")
    private String email;


    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20,
            message = "Password must be between 8 and 20 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
            message = "Password must contain uppercase, lowercase, number and special character"
    )
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Enter valid 10 digit phone number"
    )
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;
}
