package org.rishabh.authservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rishabh.authservice.dto.request.AssignRoleRequest;
import org.rishabh.authservice.dto.response.ApiResponse;
import org.rishabh.authservice.dto.response.UserResponse;
import org.rishabh.authservice.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest request) {
        UserResponse user = adminService.assignRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully", user));
    }
}
