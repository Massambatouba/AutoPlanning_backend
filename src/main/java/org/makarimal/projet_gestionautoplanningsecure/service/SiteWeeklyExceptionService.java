package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.SiteWeeklyException;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteWeeklyExceptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteWeeklyExceptionService {

    private final SiteWeeklyExceptionRepository repo;

    /** Sans filtre => toutes les exceptions du site */
    public List<SiteWeeklyException> listAll(Long siteId) {
        return repo.findBySiteIdOrderByStartDateDesc(siteId);
    }

    /** Avec filtre => exceptions qui chevauchent [from;to] */
    public List<SiteWeeklyException> listOverlapping(Long siteId, LocalDate from, LocalDate to) {
        return repo.findOverlapping(siteId, from, to);
    }
}
