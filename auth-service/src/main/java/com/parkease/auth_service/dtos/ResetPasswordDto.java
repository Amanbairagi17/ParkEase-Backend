package com.parkease.auth_service.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordDto {
    @Schema(description = "Registered email", example = "user1@gmail.com")
    @Email
    private String email;

    @Schema(description = "6 digit OTP", example = "123456")
    @NotBlank
    @Size(min = 6, max = 6)
    private String otp;

    @Schema(description = "New password", example = "NewPass@123")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain 1 lowercase, 1 uppercase, 1 digit, size 8")
    private String newPassword;
}