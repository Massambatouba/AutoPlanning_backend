package org.makarimal.projet_gestionautoplanningsecure.dto;

import org.makarimal.projet_gestionautoplanningsecure.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        select n
        from   Notification n
        where  n.company.id = :cid
        order  by n.createdAt desc
        """)
    List<Notification> findRecent(@Param("cid") Long companyId, Pageable pageable);
}

