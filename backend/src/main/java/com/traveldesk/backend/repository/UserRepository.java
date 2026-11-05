package com.traveldesk.backend.repository;

import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = "role")
Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    List<User> findByRole_Name(RoleName roleName);

    List<User> findByIsActiveTrue();
}