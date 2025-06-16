package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.AuthRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.AuthResponse;
import org.makarimal.projet_gestionautoplanningsecure.dto.RegisterRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.Role;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.makarimal.projet_gestionautoplanningsecure.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

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
                .build();
        user = userRepository.save(user);

        // Générer le token JWT
        String token = jwtService.generateToken(user);

        // Retourner la réponse avec le token, l'utilisateur et l'entreprise
        return AuthResponse.builder()
                .token(token)
                .user(user)
                .company(company)
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
}
