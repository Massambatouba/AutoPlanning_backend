package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.makarimal.projet_gestionautoplanningsecure.security.JwtService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Génère un JWT "technique" (rôle ADMIN) utilisé uniquement
 * par le service de capture PDF Playwright.
 */
@Service
@RequiredArgsConstructor
public class JwtTechTokenService {

    private final javax.sql.DataSource dataSource;

    private final JwtService     jwtService;   // ton helper JWT existant
    private final UserRepository userRepo;     // déjà déclaré dans ApplicationConfig

    /**
     * Retourne un token court‑vécu pour le compte système.
     * @throws IllegalStateException si le compte n’existe pas
     */
 /*   public String buildSystemToken() {
        System.out.println("---- DEBUG JwtTechTokenService ----");
        System.out.println("Total users table      : " + userRepo.count());
        User tech = userRepo.findByEmail("system@autoplanning.local")
                .orElseThrow(() -> new IllegalStateException("""
                         Le compte system@autoplanning.local n'existe pas.
                         Crée‑le dans ta table 'users' avec le rôle ADMIN (ou plus haut)
                         pour autoriser la génération de PDF.
                     """));
        return jwtService.generateToken(tech);
    }

  */


        public String buildSystemToken() {

            try (var conn = dataSource.getConnection()) {      // juste pour le log
                System.out.println("---- DEBUG SYSTEM USER LOOKUP ----");
                System.out.println("JDBC URL        = " + conn.getMetaData().getURL());
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("users.count()    = " + userRepo.count());
            userRepo.findAll().forEach(u ->
                    System.out.println("· " + u.getEmail()
                            + " active=" + u.isActive()
                            + " roles=" + u.getRoles()));

            return userRepo.findByEmail("system@autoplanning.local")
                    .map(jwtService::generateToken)
                    .orElseThrow(() -> new IllegalStateException("""
                         Le compte system@autoplanning.local n'existe pas
                         (base ou schéma inapproprié, inactif ou sans rôle ADMIN).
                         """));
        }



}
