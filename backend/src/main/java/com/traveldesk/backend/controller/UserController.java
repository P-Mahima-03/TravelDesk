package com.traveldesk.backend.controller;

import com.traveldesk.backend.dto.auth.UserResponse;
import com.traveldesk.backend.dto.auth.UpdateProfileRequest;
import com.traveldesk.backend.entity.User;
import com.traveldesk.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal UserDetails currentUser) {
        User user = findCurrentUser(currentUser);

        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/me")
    @Transactional
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = findCurrentUser(currentUser);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone() == null ? null : request.phone().trim());

        return ResponseEntity.ok(toUserResponse(userRepository.save(user)));
    }

    private User findCurrentUser(UserDetails currentUser) {
        return userRepository.findByEmail(currentUser.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User profile not found"
                ));
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().getName().name(),
                Boolean.TRUE.equals(user.getIsActive())
        );
    }
}
