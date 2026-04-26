package com.parkease.auth_service.mapper.Impl;

import com.parkease.auth_service.dtos.SignUpDto;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignUpMapper implements Mapper<SignUpDto, User> {
    private final ModelMapper mapper;
    @Override
    public SignUpDto mapTo(User user) {
        return mapper.map(user, SignUpDto.class);
    }

    @Override
    public User mapFrom(SignUpDto signUpDto) {
        return mapper.map(signUpDto, User.class);
    }
}
