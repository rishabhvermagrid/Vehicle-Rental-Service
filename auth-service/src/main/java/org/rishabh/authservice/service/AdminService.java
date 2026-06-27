package org.rishabh.authservice.service;

import org.rishabh.authservice.dto.request.AssignRoleRequest;
import org.rishabh.authservice.dto.response.UserResponse;

import java.util.List;

public interface AdminService {

    List<UserResponse> getAllUsers();

    UserResponse assignRole(Long userId, AssignRoleRequest request);
}
