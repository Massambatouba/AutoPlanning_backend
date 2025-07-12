package org.makarimal.projet_gestionautoplanningsecure.repository;



import org.makarimal.projet_gestionautoplanningsecure.model.ContractHourRequirement;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractHourRequirementRepository extends JpaRepository<ContractHourRequirement, Long> {
    List<ContractHourRequirement> findByCompanyIdAndIsActive(Long companyId, boolean isActive);
    Optional<ContractHourRequirement> findByCompanyIdAndContractType(Long companyId, Employee.ContractType contractType);
    boolean existsByCompanyIdAndContractType(Long companyId, Employee.ContractType contractType);
}