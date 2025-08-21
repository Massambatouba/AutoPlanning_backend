package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findByIsActiveTrue();

    Optional<SubscriptionPlan> findByNameIgnoreCase(String name);
}
