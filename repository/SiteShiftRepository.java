package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.SiteShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface SiteShiftRepository extends JpaRepository<SiteShift, Long> {
    List<SiteShift> findBySiteId(Long siteId);
    void deleteBySiteId(Long siteId);
}