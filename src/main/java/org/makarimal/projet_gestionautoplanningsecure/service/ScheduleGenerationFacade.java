package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.model.Schedule;
import org.makarimal.projet_gestionautoplanningsecure.model.User;
import org.makarimal.projet_gestionautoplanningsecure.repository.ScheduleAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class ScheduleGenerationFacade {
    private final ScheduleService scheduleService;
    private final ScheduleGeneratorService scheduleGeneratorService;
    private final ScheduleAssignmentRepository assignmentRepository;

    @Transactional
    public Schedule regenerateV2(User user, Long scheduleId) throws AccessDeniedException {
        // charger le planning
        Schedule s = scheduleService.getSchedule(user.getCompany().getId(), scheduleId);

        // purger les affectations (dans la transaction)
        assignmentRepository.deleteByScheduleId(s.getId());

        // relancer la génération V2
        return scheduleGeneratorService.generateScheduleV2(
                user.getCompany().getId(),
                s.getSite().getId(),
                s.getMonth(),
                s.getYear()
        );
    }
}

