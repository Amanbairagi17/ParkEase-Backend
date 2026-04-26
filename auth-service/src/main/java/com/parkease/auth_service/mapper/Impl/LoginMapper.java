package com.parkease.auth_service.mapper.Impl;

import com.parkease.auth_service.dtos.LoginDto;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginMapper implements Mapper<LoginDto, User> {
    private final ModelMapper mapper;

    @Override
    public LoginDto mapTo(User user) {
        return mapper.map(user, LoginDto.class);
    }

    @Override
    public User mapFrom(LoginDto loginDto) {
        return mapper.map(loginDto, User.class);
    }
}
