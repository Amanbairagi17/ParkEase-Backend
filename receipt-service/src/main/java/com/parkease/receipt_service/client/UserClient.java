package com.parkease.receipt_service.client;

import com.parkease.receipt_service.dtos.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "AUTH-SERVICE", path = "/api/internal/users")
public interface UserClient {

    @GetMapping("/{userId}")
    UserResponseDto getUserById(@PathVariable("userId") Long userId);
}