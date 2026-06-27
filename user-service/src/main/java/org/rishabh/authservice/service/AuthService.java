package org.rishabh.authservice.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.rishabh.authservice.dto.request.ChangePasswordRequest;
import org.rishabh.authservice.dto.request.LoginRequest;
import org.rishabh.authservice.dto.request.RegisterRequest;
import org.rishabh.authservice.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    UserResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    void logout(HttpServletRequest request, HttpServletResponse response);

    UserResponse getCurrentUser();

    boolean validateSession();

    void changePassword(ChangePasswordRequest request, HttpServletRequest httpRequest);
}
