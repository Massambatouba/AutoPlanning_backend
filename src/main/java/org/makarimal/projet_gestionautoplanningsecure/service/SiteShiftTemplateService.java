package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteShiftTemplateRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.makarimal.projet_gestionautoplanningsecure.model.SiteShiftTemplate;
import org.makarimal.projet_gestionautoplanningsecure.model.SiteShiftTemplateAgent;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.SiteShiftTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SiteShiftTemplateService {
    @Autowired
    private final SiteShiftTemplateRepository templateRepository;
    @Autowired
    private final SiteRepository siteRepository;

    @Transactional
    public SiteShiftTemplate createTemplate(Long companyId, Long siteId, SiteShiftTemplateRequest request) {
        Site site = siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Site not found"));

        SiteShiftTemplate template = SiteShiftTemplate.builder()
                .site(site)
                .name(request.getName())
                .dayOfWeek(request.getDayOfWeek())
                .description(request.getDescription())
                .isActive(true)
                .build();

        List<SiteShiftTemplateAgent> agents = request.getAgents().stream()
                .map(agentRequest -> SiteShiftTemplateAgent.builder()
                        .template(template)
                        .agentType(agentRequest.getAgentType())
                        .startTime(agentRequest.getStartTime())
                        .endTime(agentRequest.getEndTime())
                        .requiredCount(agentRequest.getRequiredCount())
                        .notes(agentRequest.getNotes())
                        .build()
                )
                .collect(Collectors.toList());

        template.setAgents(agents);
        return templateRepository.save(template);
    }

    public SiteShiftTemplate getTemplate(Long companyId, Long siteId, Long templateId) {
        return templateRepository.findById(templateId)
                .filter(t -> t.getSite().getId().equals(siteId))
                .filter(t -> t.getSite().getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Template not found"));
    }

    public List<SiteShiftTemplate> getTemplates(Long companyId, Long siteId, Boolean active) {
        if (!siteRepository.findById(siteId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .isPresent()) {
            throw new EntityNotFoundException("Site not found");
        }

        if (active != null) {
            return templateRepository.findBySiteIdAndIsActive(siteId, active);
        }
        return templateRepository.findBySiteId(siteId);
    }

    @Transactional
    public SiteShiftTemplate updateTemplate(
            Long companyId,
            Long siteId,
            Long templateId,
            SiteShiftTemplateRequest request
    ) {
        SiteShiftTemplate template = getTemplate(companyId, siteId, templateId);

        template.setName(request.getName());
        template.setDayOfWeek(request.getDayOfWeek());
        template.setDescription(request.getDescription());

        // Remove existing agents
        template.getAgents().clear();

        // Add new agents
        List<SiteShiftTemplateAgent> agents = request.getAgents().stream()
                .map(agentRequest -> SiteShiftTemplateAgent.builder()
                        .template(template)
                        .agentType(agentRequest.getAgentType())
                        .startTime(agentRequest.getStartTime())
                        .endTime(agentRequest.getEndTime())
                        .requiredCount(agentRequest.getRequiredCount())
                        .notes(agentRequest.getNotes())
                        .build()
                )
                .collect(Collectors.toList());

        template.getAgents().addAll(agents);
        return templateRepository.save(template);
    }

    @Transactional
    public SiteShiftTemplate toggleTemplateStatus(Long companyId, Long siteId, Long templateId) {
        SiteShiftTemplate template = getTemplate(companyId, siteId, templateId);
        template.setActive(!template.isActive());
        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(Long companyId, Long siteId, Long templateId) {
        SiteShiftTemplate template = getTemplate(companyId, siteId, templateId);
        templateRepository.delete(template);
    }
}