package org.makarimal.projet_gestionautoplanningsecure.repository;


import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface EmployeeAvailabilityRepository extends JpaRepository<EmployeeAvailability, Long> {
    List<EmployeeAvailability> findByEmployeeId(Long employeeId);
    void deleteByEmployeeId(Long employeeId);
}