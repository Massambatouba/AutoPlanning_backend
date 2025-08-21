package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User me,
                                               @RequestBody @Valid ChangePasswordRequest req) {
        authService.changePassword(me, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok().build();
    }
}