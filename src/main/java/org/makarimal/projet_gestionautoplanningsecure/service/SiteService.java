package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteResponse;
import org.makarimal.projet_gestionautoplanningsecure.mapper.SiteMapper;
import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.CompanyRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.UserRepository;
import org.makarimal.projet_gestionautoplanningsecure.util.AuthServiceHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService {
    private final SiteRepository siteRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final AuthServiceHelper authServiceHelper;
    private final SiteMapper siteMapper;



    @Transactional
    public SiteResponse createSite(SiteRequest request) {
        // Utiliser AuthServiceHelper pour récupérer l'utilisateur courant
        User user = authServiceHelper.getCurrentUser();
        Company company = user.getCompany();

        if (company == null) {
            throw new IllegalStateException("User is not associated with a company");
        }

        // Vérifier si un site avec ce nom existe déjà dans l'entreprise
        if (siteRepository.existsByNameAndCompanyId(request.getName(), company.getId())) {
            throw new IllegalArgumentException("A site with this name already exists in the company");
        }

        // Création du site
        Site site = Site.builder()
                .company(company)
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .phone(request.getPhone())
                .email(request.getEmail())
                .managerName(request.getManagerName())
                .managerEmail(request.getManagerEmail())
                .managerPhone(request.getManagerPhone())
                .active(true)
                .build();

        return mapToResponse(siteRepository.save(site));
    }

    // SiteService.java
// …

    /** Retourne l’entité Site (et vérifie qu’elle appartient bien à la company). */

    private Site getSiteEntity(Long companyId, Long siteId) {
        return siteRepository
                .findByIdAndCompanyId(siteId, companyId)   // ou …AndCompanyId(…) si tu as le champ companyId dans Site
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Aucun site " + siteId + " pour la société " + companyId));
    }


    public SiteResponse getSite(Long companyId, Long siteId) {
        Site site = siteRepository
                .findByIdAndCompanyId(siteId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Site introuvable"));

        return siteMapper.toDto(site);     // <-- mapping ici
    }



    /*public SiteResponse getSite(Long companyId, Long siteId) {
        Site site = siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));
        return mapToResponse(site);
    }

     */
    @Transactional
    public List<SiteResponse> getSites(Long companyId) {
        return siteRepository.findByCompanyId(companyId)
                .stream()
                .map(siteMapper::toDto)
                .toList();
    }


    public List<SiteResponse> getSitesByFilters(Long companyId, String city, Boolean active) {
        return siteRepository.findByFilters(companyId, city, active)
                .stream()
                .map(siteMapper::toDto)
                .toList();
    }

    @Transactional
    public SiteResponse updateSite(Long companyId, Long siteId, SiteRequest request) {
        Site site = getSiteEntity(companyId, siteId);

        if (!site.getName().equals(request.getName()) &&
                siteRepository.existsByNameAndCompanyId(request.getName(), companyId)) {
            throw new IllegalArgumentException("Site with this name already exists");
        }

        site.setName(request.getName());
        site.setAddress(request.getAddress());
        site.setCity(request.getCity());
        site.setZipCode(request.getZipCode());
        site.setCountry(request.getCountry());
        site.setPhone(request.getPhone());
        site.setEmail(request.getEmail());
        site.setManagerName(request.getManagerName());
        site.setManagerEmail(request.getManagerEmail());
        site.setManagerPhone(request.getManagerPhone());

        return mapToResponse(siteRepository.save(site));
    }

/*
    @Transactional
    public SiteResponse toggleSiteStatus(Long companyId, Long siteId) {
        Site site = getSiteEntity(companyId, siteId);
        site.setActive(!site.isActive());
        return mapToResponse(siteRepository.saveAndFlush(site));
    }

 */
@Transactional
public SiteResponse toggleSiteStatus(Long companyId, Long id)
        throws ChangeSetPersister.NotFoundException {

    Site site = siteRepository.findByCompanyIdAndId(companyId, id)
            .orElseThrow(ChangeSetPersister.NotFoundException::new);

    site.setActive(!site.isActive());
    siteRepository.saveAndFlush(site);

    return siteMapper.toDto(site);
}
    private SiteResponse mapToResponse(Site site) {
        return SiteResponse.builder()
                .id(site.getId())
                .name(site.getName())
                .address(site.getAddress())
                .city(site.getCity())
                .zipCode(site.getZipCode())
                .country(site.getCountry())
                .phone(site.getPhone())
                .email(site.getEmail())
                .managerName(site.getManagerName())
                .managerEmail(site.getManagerEmail())
                .managerPhone(site.getManagerPhone())
                .active(site.isActive())
                .build();
    }



}