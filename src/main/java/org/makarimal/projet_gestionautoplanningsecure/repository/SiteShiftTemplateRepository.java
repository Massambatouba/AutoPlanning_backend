package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.SiteShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface SiteShiftTemplateRepository extends JpaRepository<SiteShiftTemplate, Long> {
    List<SiteShiftTemplate> findBySiteId(Long siteId);
    List<SiteShiftTemplate> findBySiteIdAndIsActive(Long siteId, boolean isActive);
}