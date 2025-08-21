package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.AdminCmd;
import org.makarimal.projet_gestionautoplanningsecure.dto.UserDto;
import org.makarimal.projet_gestionautoplanningsecure.model.Role;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository repo;
    private final SiteRepository siteRepo;
    private final CompanyRepository companyRepo;
    private final PasswordEncoder encoder;


    private List<Site> resolveSites(Long companyId, List<Long> ids){
        if (ids==null || ids.isEmpty()) return List.of();
        List<Site> sites = siteRepo.findAllById(ids);
        if (sites.size()!=ids.size())
            throw new IllegalArgumentException("Site inconnu dans la liste");
        if (sites.stream().anyMatch(s -> !s.getCompany().getId().equals(companyId)))
            throw new IllegalArgumentException("Site ne dépend pas de l’entreprise");
        return sites;
    }

    /* ---------- CREATE ---------- */
    @Transactional
    public User create(User creator, AdminCmd cmd) throws AccessDeniedException {

        /* ─── 1) validation de la combinaison allSites / siteIds ─────────── */
        boolean listEmpty = cmd.siteIds() == null || cmd.siteIds().isEmpty();

        if (cmd.allSites() && !listEmpty) {
            throw new IllegalArgumentException(
                    "Quand allSites=true, siteIds doit être omis ou vide");
        }
        if (!cmd.allSites() && listEmpty) {
            throw new IllegalArgumentException(
                    "Vous devez fournir au moins un site si allSites=false");
        }

        /* ─── 2) vérification du droit du créateur ───────────────────────── */
        creator = repo.findByIdWithSites(creator.getId())
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));
        if (!creator.isSuperAdmin() && !creator.isCompanyAdminGlobal()) {
            // l’admin‑site créateur ne peut accorder que les sites qu’il gère déjà
            Long firstSiteId = cmd.allSites()
                    ? creator.getManagedSites().get(0).getId()  // quelconque
                    : cmd.siteIds().get(0);

            if (!creator.canAccessSite(firstSiteId)) {
                throw new AccessDeniedException("Vous n’avez pas accès au site " + firstSiteId);
            }
        }

        /* ─── 3) construction de la liste de sites pour le nouvel admin ──── */
        List<Site> managedSites = cmd.allSites()
                ? List.of()                                       // vide → flag manageAllSites = true
                : resolveSites(cmd.companyId(), cmd.siteIds());

        /* ─── 4) unicité de l’email ──────────────────────────────────────── */
        if (repo.existsByEmail(cmd.email())) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }

        /* ─── 5) création de l’utilisateur ───────────────────────────────── */
        User u = User.builder()
                .firstName(cmd.firstName())
                .lastName(cmd.lastName())
                .email(cmd.email())
                .username(cmd.email())
                .password(encoder.encode(cmd.tempPassword()))
                .roles(List.of(Role.SITE_ADMIN))
                .isActive(true)
                .company(companyRepo.getReferenceById(cmd.companyId()))
                .managedSites(managedSites)     // vide si allSites = true
                .manageAllSites(cmd.allSites()) // flag booléen dans l’entité
                .build();

        return repo.save(u);
    }

    @Transactional
    public User updateAccess(User editor, Long adminId, boolean manageAll, List<Long> siteIds)
            throws AccessDeniedException {

        User target = repo.getReferenceById(adminId);

        if (!editor.isSuperAdmin() && !editor.getCompanyId().equals(target.getCompanyId())) {
            throw new AccessDeniedException("Accès refusé");
        }

        if (manageAll) {
            target.setManageAllSites(true);
            target.getManagedSites().clear();
            return target;
        }

        // validations pour la liste
        if (siteIds == null || siteIds.isEmpty()) {
            throw new IllegalArgumentException("Liste de sites vide interdite (manageAllSites=false).");
        }

        // vérifications d’appartenance à la société + existence
        List<Site> sites = resolveSites(target.getCompanyId(), siteIds);
        target.setManageAllSites(false);
        target.setManagedSites(sites);
        return target;
    }

    @Transactional
    public User getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin introuvable : " + id));
    }


    /* ---------- UPDATE ---------- */
    @Transactional
    public User updateSites(User editor, Long adminId, List<Long> siteIds)
            throws AccessDeniedException {

        User target = repo.getReferenceById(adminId);

        // contrôle d’accès de l’éditeur
        if (!editor.hasRole(Role.SUPER_ADMIN) &&
                !editor.canAccessSiteList(siteIds))
            throw new AccessDeniedException("Accès refusé");

        target.setManageAllSites(false); // bascule en mode “liste”
        target.setManagedSites(resolveSites(target.getCompanyId(), siteIds));
        return target;
    }

    @Transactional
    public User grantGlobalAccess(User editor, Long adminId)
            throws AccessDeniedException {

        User target = repo.getReferenceById(adminId);

        if (!editor.hasRole(Role.SUPER_ADMIN) &&
                !editor.getCompanyId().equals(target.getCompanyId()))
            throw new AccessDeniedException("Accès refusé");

        target.setManageAllSites(true);
        target.getManagedSites().clear();
        return target;
    }

    /* ---------- ACTIVE / DESACTIVE ---------- */
    @Transactional
    public void toggleActive(Long id) {
        User u = repo.getReferenceById(id);
        u.setActive(!u.isActive());
    }

    /* ---------- DELETE ---------- */
    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    /* ---------- LISTE ADMIN D'UNE SOCIETE ---------- */
    public List<UserDto> listAdmins(Long companyId){
        return repo.findByCompany_Id(companyId).stream()
                .filter(u -> u.hasRole(Role.ADMIN) || u.hasRole(Role.SUPER_ADMIN) || u.hasRole(Role.SITE_ADMIN))
                .map(UserDto::of)
                .toList();
    }

    @Transactional
    public User setActive(Long adminId, boolean active, User actor) throws AccessDeniedException {
        User target = repo.getReferenceById(adminId);

        // protections: même société + pas soi-même
        if (actor.getId().equals(adminId)) {
            throw new IllegalArgumentException("Vous ne pouvez pas changer votre propre statut.");
        }
        if (!actor.isSuperAdmin() && !actor.getCompanyId().equals(target.getCompanyId())) {
            throw new AccessDeniedException("Accès refusé");
        }

        target.setActive(active);
        return target;
    }

    @Transactional
    public User revokeGlobalAccess(User editor, Long adminId) throws AccessDeniedException {
        User target = repo.getReferenceById(adminId);
        if (!editor.isSuperAdmin() && !editor.getCompanyId().equals(target.getCompanyId())) {
            throw new AccessDeniedException("Accès refusé");
        }
        target.setManageAllSites(false);
        target.getManagedSites().clear();
        return target;
    }

}




