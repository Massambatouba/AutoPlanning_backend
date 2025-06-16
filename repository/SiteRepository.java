package org.makarimal.projet_gestionautoplanningsecure.repository;


import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {
    List<Site> findByCompanyId(Long companyId);
    Optional<Site> findByCompanyIdAndId(Long companyId, Long id);
    boolean existsByNameAndCompanyId(String name, Long companyId);

    @Query("SELECT s FROM Site s WHERE s.company.id = :companyId AND " +
            "(:city IS NULL OR s.city = :city) AND " +
            "(:active IS NULL OR s.active = :active)")
    List<Site> findByFilters(
            @Param("companyId") Long companyId,
            @Param("city") String city,
            @Param("active") Boolean active
    );

    Optional<Site> findByIdAndCompanyId(Long siteId, Long companyId);

    List<Site> findAllByCompanyId(Long companyId);
}