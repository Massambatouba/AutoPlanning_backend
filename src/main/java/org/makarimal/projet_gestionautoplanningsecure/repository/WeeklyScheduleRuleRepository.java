package org.makarimal.projet_gestionautoplanningsecure.repository;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.makarimal.projet_gestionautoplanningsecure.model.WeeklyScheduleRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface WeeklyScheduleRuleRepository extends JpaRepository<WeeklyScheduleRule, Long> {

    List<WeeklyScheduleRule> findBySiteId(Long siteId);

    Optional<WeeklyScheduleRule> findBySiteIdAndDayOfWeek(Long siteId, DayOfWeek dayOfWeek);

    void deleteBySiteId(Long siteId);

    List<WeeklyScheduleRule> findBySiteAndDayOfWeek(Site site, DayOfWeek dayOfWeek);

    List<WeeklyScheduleRule> findAllBySiteId(Long id);
}
