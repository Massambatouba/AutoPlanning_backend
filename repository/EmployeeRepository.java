package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByCompanyId(Long companyId);

    List<Employee> findByCompanyIdAndIsActive(Long companyId, boolean isActive);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByEmployeeCode(String employeeCode);
    boolean existsByEmailAndCompanyId(String email, Long companyId);
    boolean existsByEmployeeCodeAndCompanyId(String employeeCode, Long companyId);

    @Query("SELECT e FROM Employee e WHERE e.company.id = :companyId AND " +
            "(:department IS NULL OR e.department = :department) AND " +
            "(:contractType IS NULL OR e.contractType = :contractType)")
    List<Employee> findByFilters(
            @Param("companyId") Long companyId,
            @Param("department") String department,
            @Param("contractType") Employee.ContractType contractType
    );



    List<Employee> findBySite(Site site);
    // renvoie les actifs pour un site donné
    List<Employee> findBySiteIdAndIsActiveTrue(Long siteId);


    List<Employee> findBySiteAndIsActiveTrueAndAgentTypesContaining(Site site, AgentType type);

    @Query("""
           select e
           from Employee e
           where e.site = :site
             and e.isActive = true
             and :agentType member of e.agentTypes
           """)
    List<Employee> findActiveBySiteAndAgentType(@Param("site") Site site,
                                                @Param("agentType") AgentType agentType);
}