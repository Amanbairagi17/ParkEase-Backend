package com.parkease.auth_service.controller;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.service.AuthService;
import com.parkease.auth_service.service.Impl.UserServiceImpl;
import com.parkease.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = {"/user", "/api/user"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('DRIVER','MANAGER','ADMIN')")
public class UserController {
    private final UserServiceImpl userService;
    @GetMapping("/{userId}")
    public ResponseEntity<AuthResponseDto> getUserById(@PathVariable Long userId){
        return ResponseEntity.status(HttpStatus.OK).body(userService.findUserById(userId));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<AuthResponseDto> getUserByEmail(@PathVariable String email) {
        log.info("Request received to fetch user by email. email={}", email);
        AuthResponseDto response = userService.findUserByEmail(email);
        log.info("Returning response for fetch user by email. email={}", email);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

//    @GetMapping("existById/{email}")
//    public Boo
}
