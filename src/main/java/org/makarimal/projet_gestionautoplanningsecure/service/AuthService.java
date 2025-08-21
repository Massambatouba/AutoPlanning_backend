package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.PasswordResetToken;
import org.makarimal.projet_gestionautoplanningsecure.model.Role;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.PasswordResetTokenRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.makarimal.projet_gestionautoplanningsecure.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository tokenRepo;
    private final MailService mailService;
    @Value("${app.front.url}")
    private String frontUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Créer la société
        Company company = Company.builder()
                .name(request.getCompanyName())
                .subscriptionStatus(Company.SubscriptionStatus.TRIAL)
                .maxEmployees(20)
                .maxSites(2)
                .build();
        company = companyRepository.save(company);

        // Créer l'utilisateur
        Role userRole = Role.valueOf(request.getRoles().get(0));
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(List.of(userRole))
                .username(request.getUsername())
                .company(company)
                .isActive(true)
                .manageAllSites(true)
                .managedSites(Collections.emptyList())
                .mustChangePassword(true)
                .passwordChangedAt(null)
                .build();
        user = userRepository.save(user);

        // Générer le token JWT
        String token = jwtService.generateToken(user);

        // Retourner la réponse avec le token, l'utilisateur et l'entreprise
        return AuthResponse.builder()
                .token(token)
                .user(user)
                .company(company)
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        try {
            // Authentification de l'utilisateur
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Récupérer l'utilisateur après l'authentification
            User user = (User) authentication.getPrincipal();

            // Vérifier si l'utilisateur est actif
            if (!user.isActive()) {
                throw new IllegalArgumentException("User account is deactivated");
            }

            // Générer le token JWT
            String token = jwtService.generateToken(user);

            // Retourner la réponse avec le token, l'utilisateur et l'entreprise
            return AuthResponse.builder()
                    .token(token)
                    .user(user)
                    .company(user.getCompany())
                    .build();

        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Invalid email or password", e);
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed", e);
        }
    }

    /* ========== FORCER CHANGEMENT APRES LOGIN ========== */
    @Transactional
    public void changePassword(User me, ChangePasswordRequest req) {
        User u = userRepository.findById(me.getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        if (!passwordEncoder.matches(req.getOldPassword(), u.getPassword())) {
            throw new IllegalArgumentException("Ancien mot de passe invalide");
        }
        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        u.setMustChangePassword(false);
        u.setPasswordChangedAt(Instant.now());
    }

    /* ========== MOT DE PASSE OUBLIÉ ========== */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email inconnu"));

        // invalider anciens tokens
        tokenRepo.deleteByUser_Id(u.getId());

        // générer un token (random)
        String token = UUID.randomUUID().toString();
        PasswordResetToken t = PasswordResetToken.builder()
                .user(u)
                .token(token)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(30)))
                .build();
        tokenRepo.save(t);

        // envoyer l'email (à brancher selon ton infra)
         String link = frontUrl + "/auth/reset-password?token=" + token;
         mailService.sendPasswordReset(u.getEmail(), link);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken t = tokenRepo.findByToken(req.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));
        if (t.isUsed() || t.isExpired()) {
            throw new IllegalArgumentException("Token expiré ou déjà utilisé");
        }
        User u = t.getUser();
        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        u.setMustChangePassword(false);
        u.setPasswordChangedAt(Instant.now());
        t.setUsedAt(Instant.now());
    }

}
