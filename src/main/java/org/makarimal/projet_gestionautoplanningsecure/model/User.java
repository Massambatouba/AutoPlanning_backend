package org.makarimal.projet_gestionautoplanningsecure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Getter @Setter               // ← pour générer tous les accesseurs, dont isManageAllSites()
@Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "users")
public class User implements UserDetails {

    /* ---------- Champs persistés ---------- */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean mustChangePassword = false;

    private Instant passwordChangedAt;

    @Column(nullable=false) private String firstName;
    @Column(nullable=false) private String lastName;
    @Column(nullable=false, unique=true) private String email;

    @Column(nullable=false) private String password;

    @ManyToOne @JoinColumn(name="company_id")
    private Company company;

    @Column(nullable=false, unique=true) private String username;

    @ElementCollection(fetch=FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(name="roles")
    private List<Role> roles = new ArrayList<>();

    private String position;
    private String phone;
    private boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /* ----------- Accès aux sites ----------- */


    @Column(name="manage_all_sites",
            nullable=false,
            columnDefinition="boolean default false")
    private boolean manageAllSites;

    /** Liste des sites autorisés quand <code>manageAllSites == false</code> */
    @ManyToMany
    @JoinTable(name = "admin_sites",
            joinColumns        = @JoinColumn(name="admin_id"),
            inverseJoinColumns = @JoinColumn(name="site_id"))
    private List<Site> managedSites = new ArrayList<>();

    /* ---------- Helpers de rôle / droits ---------- */

    public boolean hasRole(Role r)            { return roles.contains(r); }
    public boolean isSuperAdmin()             { return hasRole(Role.SUPER_ADMIN); }
    public boolean isCompanyAdminGlobal()     { return hasRole(Role.ADMIN) && manageAllSites; }

    /** L’utilisateur peut‑il accéder à ce site ? */
    public boolean canAccessSite(Long siteId) {
        if (isSuperAdmin())            return true;        // gérant plate‑forme
        if (isCompanyAdminGlobal())    return true;        // admin « global » de l’entreprise
        return managedSites.stream().anyMatch(s -> s.getId().equals(siteId));
    }

    /** Tous les sites de la liste sont‑ils autorisés ? */
    public boolean canAccessSiteList(Collection<Long> ids) {
        return ids.stream().allMatch(this::canAccessSite);
    }

    /* ---------- Hooks JPA ---------- */

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    /* ---------- Implémentation UserDetails ---------- */

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }
    @Override public String  getUsername()             { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return isActive; }

    /* ---------- Utilitaire ---------- */

    public Long getCompanyId() {
        return company != null ? company.getId() : null;
    }


}
