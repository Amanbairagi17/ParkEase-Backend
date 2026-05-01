package com.parkease.auth_service.entity;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final User user;

//    public CustomUserDetails(User user){
//        this.user = user;
//    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(
                new SimpleGrantedAuthority("ROLE_" +user.getRole())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword() != null ? user.getPassword() : "";
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;   // account valid
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;   // not locked
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;   // password valid
    }

    @Override
    public boolean isEnabled() {
        return true;   // active user
    }

    // this user Id needed in jwt creation for role-based access
    public Long getUserId() {
        return user.getUserId();   // ✅ expose userId
    }
}
