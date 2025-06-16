package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.CreateCompanyRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.Role;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SubscriptionPlanRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public Company createCompany(CreateCompanyRequest request) {
        var subscriptionPlan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new EntityNotFoundException("Subscription plan not found"));

        // Étape 1 : Créer le User sans company
        User admin = User.builder()
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .username(request.getAdminUsername())
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .roles(new ArrayList<>(Set.of(Role.ADMIN)))
                .isActive(true)
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
    public Company updateCompany(Long id, Company request) {
        Company company = getCompany(id);

        var subscriptionPlan = subscriptionPlanRepository.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("Subscription plan not found"));

        company.setName(request.getName());
        company.setAddress(request.getAddress());
        company.setPhone(request.getPhone());
        company.setPhone(request.getName());
        company.setEmail(request.getEmail());
        company.setWebsite(request.getWebsite());
        company.setMaxEmployees(request.getMaxEmployees() != null ? request.getMaxEmployees() : subscriptionPlan.getMaxEmployees());
        company.setMaxSites(request.getMaxSites() != null ? request.getMaxSites() : subscriptionPlan.getMaxSites());

        return companyRepository.save(company);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public void deleteCompany(Long id) {
        Company company = getCompany(id);

        // Désactiver tous les utilisateurs de l'entreprise
        List<User> companyUsers = userRepository.findByCompanyId(id);
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
}