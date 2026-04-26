package com.parkease.auth_service.mapper.Impl;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor

public class AuthResponseMapper implements Mapper<AuthResponseDto, User> {
    private final ModelMapper mapper;

    @Override
    public AuthResponseDto mapTo(User user) {
        return mapper.map(user, AuthResponseDto.class);
    }

    @Override
    public User mapFrom(AuthResponseDto authResponseDto) {
        return mapper.map(authResponseDto, User.class);
    }
}
