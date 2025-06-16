package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.SiteScheduleOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SiteScheduleOverrideRepository extends JpaRepository<SiteScheduleOverride, Long> {

    List<SiteScheduleOverride> findAllBySiteId(Long siteId);
    Optional<SiteScheduleOverride> findBySiteIdAndOverrideDate(Long siteId, LocalDate overrideDate);
}
