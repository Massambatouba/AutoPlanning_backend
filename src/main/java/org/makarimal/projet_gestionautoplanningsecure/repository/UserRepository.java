package org.makarimal.projet_gestionautoplanningsecure.repository;

import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findByCompany_Id(Long companyId);
    boolean existsByIdAndManagedSites_Id(Long userId, Long siteId);

    // UserRepository.java
    @Query("""
       select u
       from   User u
       left join fetch u.managedSites
       where  u.id = :id
       """)
    Optional<User> findByIdWithSites(@Param("id") Long id);

    long countByCompany_Id(Long companyId);
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);
}
