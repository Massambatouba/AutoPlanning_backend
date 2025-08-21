package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.CompanyOverview;
import org.makarimal.projet_gestionautoplanningsecure.dto.CreateCompanyRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.UpdateCompanyRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.UpdateSubscriptionRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.Role;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SubscriptionPlanRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SiteRepository siteRepository;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public Company createCompany(CreateCompanyRequest request) {
        var subscriptionPlan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new EntityNotFoundException("Subscription plan not found"));
// contrôles d'unicité applicatifs avant insertion
        if (userRepository.existsByEmailIgnoreCase((request.getAdminEmail())))
            throw new DataIntegrityViolationException("admin email already used");
        if (userRepository.existsByUsernameIgnoreCase((request.getAdminUsername())))
            throw new DataIntegrityViolationException("admin username already used");

        // Étape 1 : Créer le User sans company
        User admin = User.builder()
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .username(request.getAdminUsername())
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .roles(new ArrayList<>(Set.of(Role.ADMIN)))
                .isActive(true)
                .mustChangePassword(true)
                .build();
        userRepository.save(admin);

        // Étape 2 : Créer la company avec ce user comme owner
        Company company = Company.builder()
                .name(request.getCompanyName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .subscriptionStatus(Company.SubscriptionStatus.ACTIVE)
                .subscriptionExpiresAt(LocalDateTime.now().plusMonths(1))
                .maxEmployees(subscriptionPlan.getMaxEmployees())
                .maxSites(subscriptionPlan.getMaxSites())
                .subscriptionPlan(subscriptionPlan)
                .owner(admin)
                .build();
        company = companyRepository.save(company);

        // Étape 3 : Mettre à jour le user pour lui affecter la company
        admin.setCompany(company);
        userRepository.save(admin); // nécessaire pour mettre à jour la FK company_id

        return company;
    }


    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Company getCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Company not found"));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public Company updateCompany(Long id, UpdateCompanyRequest request) {
        Company company = getCompany(id);

        // si on te passe un plan -> recalculer les limites à partir du plan
        if (request.getSubscriptionPlanId() != null) {
            var plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                    .orElseThrow(() -> new EntityNotFoundException("Subscription plan not found"));
            company.setMaxEmployees(plan.getMaxEmployees());
            company.setMaxSites(plan.getMaxSites());
            company.setSubscriptionPlan(plan);
        }

        // merge des champs simples (sans écraser par null)
        if (request.getName() != null) company.setName(request.getName());
        if (request.getAddress() != null) company.setAddress(request.getAddress());
        if (request.getPhone() != null) company.setPhone(request.getPhone());
        if (request.getEmail() != null) company.setEmail(request.getEmail());
        if (request.getWebsite() != null) company.setWebsite(request.getWebsite());

        // overrides explicites si fournis
        if (request.getMaxEmployees() != null) company.setMaxEmployees(request.getMaxEmployees());
        if (request.getMaxSites() != null) company.setMaxSites(request.getMaxSites());

        return companyRepository.save(company);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public void deleteCompany(Long id) {
        Company company = getCompany(id);

        // Désactiver tous les utilisateurs de l'entreprise
        List<User> companyUsers = userRepository.findByCompany_Id(id);
        companyUsers.forEach(user -> user.setActive(false));
        userRepository.saveAll(companyUsers);

        company.setSubscriptionStatus(Company.SubscriptionStatus.INACTIVE);
        companyRepository.save(company);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public Company toggleCompanyStatus(Long id) {
        Company company = getCompany(id);

        if (company.getSubscriptionStatus() == Company.SubscriptionStatus.ACTIVE) {
            company.setSubscriptionStatus(Company.SubscriptionStatus.INACTIVE);
        } else {
            company.setSubscriptionStatus(Company.SubscriptionStatus.ACTIVE);
        }

        return companyRepository.save(company);
    }

    public Page<Company> search(int page, int size, String q, String status) {
        // ⚠️ pas de Sort ici (sinon Spring rajoute "c.createdAt")
        Pageable pageable = PageRequest.of(page, size);

        Company.SubscriptionStatus st = null;
        if (status != null && !status.isBlank()) {
            st = Company.SubscriptionStatus.valueOf(status.toUpperCase());
        }

        String qTrim = (q == null || q.isBlank()) ? null : q.trim();

        return companyRepository.searchNative(
                qTrim,
                (st != null ? st.name() : null),
                pageable
        );
    }

    public CompanyOverview getOverview(Long id) {
        Company c = getCompany(id); // ta méthode existante
        long emp = userRepository.countByCompany_Id(id);
        long sites = siteRepository.countByCompany_Id(id);
        return CompanyOverview.of(c, emp, sites);
    }

    @Transactional
    public CompanyOverview setActive(Long id, boolean active) {
        Company c = getCompany(id);
        c.setActive(active);
        c.setSubscriptionStatus(active ? Company.SubscriptionStatus.ACTIVE
                : Company.SubscriptionStatus.INACTIVE);
        companyRepository.save(c);
        long emp = userRepository.countByCompany_Id(id);
        long sites = siteRepository.countByCompany_Id(id);
        return CompanyOverview.of(c, emp, sites);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public CompanyOverview updateSubscription(Long id, UpdateSubscriptionRequest req) {
        Company c = getCompany(id);

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            c.setSubscriptionStatus(Company.SubscriptionStatus.valueOf(req.getStatus().toUpperCase()));
        }
        if (req.getPlanId() != null) {
            var plan = subscriptionPlanRepository.findById(req.getPlanId())
                    .orElseThrow(() -> new EntityNotFoundException("Plan not found: " + req.getPlanId()));
            c.setSubscriptionPlan(plan);
            c.setMaxEmployees(plan.getMaxEmployees());
            c.setMaxSites(plan.getMaxSites());
        }

        companyRepository.save(c);

        long emp = userRepository.countByCompany_Id(id);
        long sites = siteRepository.countByCompany_Id(id);
        return CompanyOverview.of(c, emp, sites);
    }



}