package com.parkease.auth_service.mapper.Impl;

import com.parkease.auth_service.dtos.AdminUserResponseDto;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserResponseMapper implements Mapper<AdminUserResponseDto, User> {

    private final ModelMapper mapper;

    @Override
    public AdminUserResponseDto mapTo(User user) {
        return mapper.map(user, AdminUserResponseDto.class);
    }

    @Override
    public User mapFrom(AdminUserResponseDto adminUserResponseDto) {
        return mapper.map(adminUserResponseDto, User.class);
    }
}
