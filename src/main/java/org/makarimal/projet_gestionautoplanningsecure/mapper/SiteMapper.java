package org.makarimal.projet_gestionautoplanningsecure.mapper;

import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeShiftDTO;
import org.makarimal.projet_gestionautoplanningsecure.dto.SiteResponse;
import org.makarimal.projet_gestionautoplanningsecure.model.ScheduleAssignment;
import org.makarimal.projet_gestionautoplanningsecure.model.Site;
import org.springframework.stereotype.Component;

@Component
public class SiteMapper {

    public SiteResponse toDto(Site entity) {
        if (entity == null) {
            return null;
        }

        return SiteResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .address(entity.getAddress())
                .city(entity.getCity())
                .zipCode(entity.getZipCode())
                .country(entity.getCountry())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .managerName(entity.getManagerName())
                .managerEmail(entity.getManagerEmail())
                .managerPhone(entity.getManagerPhone())
                .active(entity.isActive())        // si le champ existe
                .build();
    }

    private EmployeeShiftDTO toDto(ScheduleAssignment a) {
        return EmployeeShiftDTO.builder()
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getFirstName()
                        + " " + a.getEmployee().getLastName())
                .agentType(a.getAgentType().name())      // Enum → String
                .shiftLabel(a.getShift())               // ex. "MATIN"
                .startTime(a.getStartTime().toString()) // "08:00"
                .endTime(a.getEndTime().toString())     // "16:00"
                .build();
    }

}
