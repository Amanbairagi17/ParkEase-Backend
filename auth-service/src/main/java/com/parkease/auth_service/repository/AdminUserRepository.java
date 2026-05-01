package com.parkease.auth_service.repository;

import com.parkease.auth_service.entity.Role;
import com.parkease.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<User, Long> {

    List<User> findByRole(Role role);

    List<User> findByIsActive(Boolean isActive);

    long countByRole(Role role);

    long countByIsActive(Boolean isActive);

    @Query("SELECT u FROM User u WHERE u.role != com.parkease.auth_service.entity.Role.ADMIN")
    List<User> findAllNonAdminUsers();
}
