// src/main/java/org/makarimal/projet_gestionautoplanningsecure/config/SecurityConfig.java
package org.makarimal.projet_gestionautoplanningsecure.config;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor         // ➜ injection par constructeur, pas besoin de @Autowired
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider  authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // --- API REST stateless ---------------------------------------------------------
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // --- CORS ----------------------------------------------------------------------
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // --- Règles d’accès -------------------------------------------------------------
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics (même chemin que dans JwtAuthenticationFilter)
                        .requestMatchers("/auth/**", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()
                        // Exemple d’endpoint public supplémentaire
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/absences/**").permitAll()
                        //Swagger
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/platform-admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(
                                "/auth/login", "/auth/register",
                                "/auth/forgot-password", "/auth/reset-password")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST ,"/schedules/*/send", "/schedules/*/send/**")
                        .hasAnyRole("SUPER_ADMIN","ADMIN","SITE_ADMIN")
                        // Tout le reste nécessite un JWT valide
                        .anyRequest().authenticated())

                // --- Provider + Filtre JWT ------------------------------------------------------
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ---------------------------------------------------------------------------------------
    // CORS : autorise ton front Angular (http://localhost:4200) à parler à l’API
    // ---------------------------------------------------------------------------------------
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
