package com.parkease.parkinglot_service.client;

import com.parkease.parkinglot_service.dtos.UserLookupResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", path = "/api/user")
public interface UserServiceClient {

    @GetMapping("/{userId}")
    UserLookupResponseDto getUserById(@PathVariable("userId") Long userId);
}
