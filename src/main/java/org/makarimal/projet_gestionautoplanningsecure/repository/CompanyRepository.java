package org.makarimal.projet_gestionautoplanningsecure.repository;


import org.makarimal.projet_gestionautoplanningsecure.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    long countByActiveTrue();

    @Query(value = """
        select c.*
        from companies c
        where (:q is null or c.name ILIKE ('%' || :q || '%'))
          and (:status is null or c.subscription_status = :status)
        order by c.created_at desc
        """,
            countQuery = """
        select count(*)
        from companies c
        where (:q is null or c.name ILIKE ('%' || :q || '%'))
          and (:status is null or c.subscription_status = :status)
        """,
            nativeQuery = true)
    Page<Company> searchNative(@Param("q") String q,
                               @Param("status") String status,  // <- chaîne ENUM en .name()
                               Pageable page);
}