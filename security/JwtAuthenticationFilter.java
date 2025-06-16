// src/main/java/org/makarimal/projet_gestionautoplanningsecure/security/JwtAuthenticationFilter.java
package org.makarimal.projet_gestionautoplanningsecure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component               // ➜ déclarée une seule fois ; on ne crée PAS un second bean ailleurs
@RequiredArgsConstructor // ➜ injection par constructeur ; pas besoin de @Autowired
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        log.info("➡️  JwtFilter déclenché pour {}", path);

        // Endpoints publics : on ne touche pas au header JWT
        if (isPublicEndpoint(request)) {
            log.info("⏭️  Endpoint public → on ne traite pas le JWT");
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        log.info("🔎 Authorization header = {}", authHeader);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("⏭️  Pas de Bearer → on continue la chaîne sans authentifier");
            log.debug("Authorization header absent ou mal formé : {}", authHeader);
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);   // « Bearer « + espace = 7 »
        String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
            log.info("📧 Username extrait du token = {}", userEmail);
        } catch (Exception ex) {          // signature, expiration, format, …
            log.debug("JWT invalide : {}", ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Si aucun autre filtre n’a déjà authentifié l’utilisateur
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authentication posée pour {}", userEmail);
            } else {
                log.debug("JWT signature/claims invalides pour {}", userEmail);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/auth");
    }
}
