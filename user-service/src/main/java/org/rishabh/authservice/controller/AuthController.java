package org.rishabh.authservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rishabh.authservice.dto.request.ChangePasswordRequest;
import org.rishabh.authservice.dto.request.LoginRequest;
import org.rishabh.authservice.dto.request.RegisterRequest;
import org.rishabh.authservice.dto.response.ApiResponse;
import org.rishabh.authservice.dto.response.UserResponse;
import org.rishabh.authservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor //annotation on the class tells Lombok to generate a constructor for every final field at compile time.
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", user));
    }

    //HttpServletRequest / HttpServletResponse are passed through because login needs to create and write a session cookie to the HTTP response
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        UserResponse user = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Login successful", user));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success("User retrieved", user));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validateSession() {
        boolean valid = authService.validateSession();
        return ResponseEntity.ok(
                ApiResponse.success("Session is " + (valid ? "valid" : "invalid"), valid));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        authService.changePassword(request, httpRequest);
        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully. Please log in again.", null));
    }
}
