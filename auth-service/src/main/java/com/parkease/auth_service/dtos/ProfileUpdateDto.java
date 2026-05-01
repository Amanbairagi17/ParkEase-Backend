package com.parkease.auth_service.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateDto {
    
    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 50, message = "Full name must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z ]+$", message = "Name should contain only alphabets and spaces")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter valid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10 digit phone number")
    private String phone;

    private String address;
    private String profilePicUrl;
}
