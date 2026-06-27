package org.rishabh.authservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.rishabh.authservice.dto.request.AssignRoleRequest;
import org.rishabh.authservice.dto.response.UserResponse;
import org.rishabh.authservice.entity.User;
import org.rishabh.authservice.exception.UserNotFoundException;
import org.rishabh.authservice.repository.UserRepository;
import org.rishabh.authservice.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse assignRole(Long userId, AssignRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        user.setRole(request.getRole());
        return UserResponse.from(userRepository.save(user));
    }
}
