package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.entity.Role;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailServiceImpl service;

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenFound() {
        User user = new User();
        user.setEmail("user@demo.com");
        user.setRole(Role.DRIVER);
        when(userRepository.findByEmail("user@demo.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("user@demo.com");

        assertEquals("user@demo.com", details.getUsername());
    }

    @Test
    void loadUserByUsername_ShouldThrow_WhenMissing() {
        when(userRepository.findByEmail("missing@demo.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@demo.com"));
    }
}
