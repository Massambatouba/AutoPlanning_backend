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

// src/main/java/.../security/JwtAuthenticationFilter.java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = normalizedPath(request);  // <-- on enlève le context-path (/api)
        log.debug("[JWT] path={}", path);

        // ⛔ NE PAS exclure /schedules/.../send
        if (isPublicEndpoint(path)) {
            log.debug("[JWT] public endpoint -> pass-through");
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        log.debug("[JWT] Authorization={}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JWT] no/bad bearer -> continue without auth");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
            log.debug("[JWT] username={}", userEmail);
        } catch (Exception ex) {
            log.warn("[JWT] token invalid: {}", ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("[JWT] Authentication set for {}", userEmail);
            } else {
                log.debug("[JWT] token not valid for {}", userEmail);
            }
        }

        filterChain.doFilter(request, response);
    }

    /** Retire le context-path (ex: /api) du chemin. */
    private String normalizedPath(HttpServletRequest request) {
        String path = request.getRequestURI();       // ex: /api/schedules/9/send
        String ctx  = request.getContextPath();      // ex: /api
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());     // -> /schedules/9/send
        }
        return path;
    }

    /** Uniquement les vraies routes publiques. */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/auth")
                || path.equals("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }
}
