package com.parkease.vehicle_service.client;

import com.parkease.vehicle_service.dtos.UserLookupResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "AUTH-SERVICE", path = "/api/user")
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserLookupResponseDto getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/email/{email}")
    UserLookupResponseDto getUserByEmail(@PathVariable("email") String email);
}