package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.SiteScheduleOverride;
import org.makarimal.projet_gestionautoplanningsecure.model.SiteScheduleRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
@Repository
public interface SiteScheduleRuleRepository extends JpaRepository<SiteScheduleRule, Long> {
    Optional<SiteScheduleRule> findBySiteId(Long siteId);


}
