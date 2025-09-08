package org.makarimal.projet_gestionautoplanningsecure.repository;


import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeePlanningDTO;
import org.makarimal.projet_gestionautoplanningsecure.model.AgentType;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByCompanyId(Long companyId);

    // EmployeeRepository.java
    @Query("""
       select e
       from   Employee e
       join fetch e.company
       where  e.id = :id
       """)
    Optional<Employee> findWithCompanyById(@Param("id") Long id);


    //List<Employee> findByCompanyIdAndIsActive(Long companyId, boolean isActive);
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
    //List<Employee> findBySiteIdAndIsActiveTrue(Long siteId);


    List<Employee> findBySiteAndIsActiveTrueAndAgentTypesContaining(Site site, AgentType type);
/*
    @Query("""
           select e
           from Employee e
           where e.site = :site
             and e.isActive = true
             and :agentType member of e.agentTypes
           """)
    List<Employee> findIsActiveBySiteAndAgentType(@Param("site") Site site,
                                                @Param("agentType") AgentType agentType);

 */


    @Query("SELECT e FROM Employee e WHERE e.site.id = :siteId AND e.isActive = true")
    List<Employee> findBySiteIdAndIsActiveTrue(@Param("siteId") Long siteId);


    @Query("""
           select e
             from Employee e
            where e.site.id = :siteId
              and e.isActive = true
            order by e.lastName, e.firstName
           """)
    List<Employee> findActiveBySite(@Param("siteId") Long siteId);

    long countByCompany_Id(Long cid);

    @Query("""
     select e from Employee e
     where e.site.id = :siteId
     order by e.lastName, e.firstName
  """)
    List<Employee> findBySiteIdOrderByLastNameAsc(@Param("siteId") Long siteId);

    @Query("""
        select e
        from Employee e
        where e.company.id = :companyId
          and (
                :q is null or :q = '' or
                lower(e.firstName) like lower(concat('%', :q, '%')) or
                lower(e.lastName)  like lower(concat('%', :q, '%')) or
                lower(coalesce(e.email, '')) like lower(concat('%', :q, '%'))
          )
          and e.id not in (
            select ee.id
            from Site s join s.employees ee
            where s.id = :siteId
          )
        order by e.lastName, e.firstName
        """)
    List<Employee> searchCandidatesForSite(
            @Param("companyId") Long companyId,
            @Param("siteId")     Long siteId,
            @Param("q")          String q
    );






}


