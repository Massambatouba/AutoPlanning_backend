package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteScheduleOverrideRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteScheduleRuleRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteShiftRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.WeeklyScheduleRuleRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteRuleService {
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final SiteShiftRepository shiftRepository;
    @Autowired
    private final SiteScheduleRuleRepository ruleRepository;
    @Autowired
    private final SiteScheduleOverrideRepository overrideRepository;
    @Autowired
    private final WeeklyScheduleRuleRepository weeklyRuleRepository;

    @Transactional
    public SiteShift createShift(Long companyId, Long siteId, SiteShiftRequest request) {
        boolean crossesMidnight = request.getEndTime().isBefore(request.getStartTime());
        if (crossesMidnight) {
            System.out.println("Vacation de nuit détectée : commence à " + request.getStartTime() + ", finit à " + request.getEndTime());
        }
        Site site = siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));

        SiteShift shift = SiteShift.builder()
                .site(site)
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .requiredEmployees(request.getRequiredEmployees())
                .minExperience(request.getMinExperience())
                .requiredSkills(request.getRequiredSkills())
                .build();

        return shiftRepository.save(shift);
    }

    @Transactional
    public List<WeeklyScheduleRule> defineWeeklyScheduleRule(Long companyId, Long siteId, @Valid List<WeeklyScheduleRuleRequest> requests) {
        Site site = siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));

        List<WeeklyScheduleRule> newRules = requests.stream().map(req -> {
            WeeklyScheduleRule rule = WeeklyScheduleRule.builder()
                    .site(site)
                    .dayOfWeek(req.getDayOfWeek())
                    .minEmployees(req.getMinEmployees())
                    .maxEmployees(req.getMaxEmployees())
                    .minExperienceLevel(req.getMinExperienceLevel())
                    .requiresNightShift(req.isRequiresNightShift())
                    .requiresWeekendCoverage(req.isRequiresWeekendCoverage())
                    .requiredSkills(req.getRequiredSkills())
                    .build();

            // Création des AgentSchedule associés
            List<AgentSchedule> agentSchedules = req.getAgents().stream().map(agentReq -> {
                AgentSchedule schedule = new AgentSchedule();
                schedule.setAgentType(agentReq.getAgentType());
                schedule.setStartTime(LocalTime.parse(agentReq.getStartTime()));
                schedule.setEndTime(LocalTime.parse(agentReq.getEndTime()));
                schedule.setRequiredCount(agentReq.getRequiredCount());
                schedule.setNotes(agentReq.getNotes());
                schedule.setWeeklyScheduleRule(rule); // Association à la règle
                return schedule;
            }).collect(Collectors.toList());

            rule.setAgents(agentSchedules); // Liaison à la règle

            return rule;
        }).collect(Collectors.toList());

        return weeklyRuleRepository.saveAll(newRules);
    }

    @Transactional
    public SiteScheduleRule updateScheduleRule(Long companyId, Long siteId, SiteScheduleRuleRequest request) {
        // Vérifier si le site appartient à l'entreprise
        Site site = siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));

        // Chercher une règle de planning existante
        SiteScheduleRule rule = ruleRepository.findBySiteId(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site schedule rule not found"));

        // Mettre à jour les champs de la règle de planning
        rule.setMinEmployeesPerDay(request.getMinEmployeesPerDay());
        rule.setMaxEmployeesPerDay(request.getMaxEmployeesPerDay());
        rule.setMinExperienceLevel(request.getMinExperienceLevel());
        rule.setRequiresNightShift(request.isRequiresNightShift());
        rule.setRequiresWeekendCoverage(request.isRequiresWeekendCoverage());
        rule.setRequiredSkills(request.getRequiredSkills());

        // Enregistrer la règle mise à jour
        return ruleRepository.save(rule);
    }




    public List<SiteShift> getSiteShifts(Long companyId, Long siteId) {
        if (!siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .isPresent()) {
            throw new EntityNotFoundException("Site not found");
        }

        return shiftRepository.findBySiteId(siteId);
    }

    @Transactional
    public void deleteShift(Long companyId, Long siteId, Long shiftId) {
        if (!siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .isPresent()) {
            throw new EntityNotFoundException("Site not found");
        }

        shiftRepository.deleteById(shiftId);
    }




    @Transactional
    public SiteScheduleOverride createOrUpdateOverride(Long companyId, Long siteId, SiteScheduleOverrideRequest request) {
        // Vérifier si le site appartient à l'entreprise
        Site site = siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));

        // Chercher un override existant pour cette date
        SiteScheduleOverride override = overrideRepository
                .findBySiteIdAndOverrideDate(siteId, request.getOverrideDate())
                .orElseGet(() -> SiteScheduleOverride.builder()
                        .site(site)
                        .overrideDate(request.getOverrideDate())
                        .build());

        // Mettre à jour les champs
        override.setMinEmployees(request.getMinEmployees());
        override.setMaxEmployees(request.getMaxEmployees());
        override.setMinExperienceLevel(request.getMinExperienceLevel());
        override.setRequiresNightShift(request.isRequiresNightShift());
        override.setRequiresWeekendCoverage(request.isRequiresWeekendCoverage());
        override.setRequiredSkills(request.getRequiredSkills());

        return overrideRepository.save(override);
    }


    public SiteScheduleRule getScheduleRule(Long companyId, Long siteId) {
        if (!siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .isPresent()) {
            throw new EntityNotFoundException("Site not found");
        }

        return ruleRepository.findBySiteId(siteId)
                .orElseThrow(() -> new EntityNotFoundException("Site schedule rule not found"));
    }

    public List<WeeklyScheduleRule> getWeeklyScheduleRules(Long companyId, Long siteId) {
        Site site = siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));

        return weeklyRuleRepository.findBySiteId(siteId);
    }

    @Transactional
    public List<WeeklyScheduleRule> replaceWeeklyRules(Long companyId, Long siteId, List<WeeklyScheduleRuleRequest> requests) {
        Site site = siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));

        // Supprimer les anciennes règles
        weeklyRuleRepository.deleteBySiteId(siteId);

        // Enregistrer les nouvelles règles
        return defineWeeklyScheduleRule(companyId, siteId, requests);
    }



    @Transactional
    public void deleteWeeklyRules(Long companyId, Long siteId) {
        Site site = siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));

        weeklyRuleRepository.deleteBySiteId(siteId);
    }





}