package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeDocument;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {
    List<EmployeeDocument> findByEmployeeId(Long employeeId);

    @Query("""
           select d
           from EmployeeDocument d
           where d.expiryDate between :from and :to
             and d.employee.company.id = :companyId
           """)
    List<EmployeeDocument> findExpiringBetween(@Param("from") LocalDate from,
                                               @Param("to")   LocalDate to,
                                               @Param("companyId") Long companyId);
}


