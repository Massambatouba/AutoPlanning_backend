package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeAvailabilityRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeePreferenceRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeRequest;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeeAvailability;
import org.makarimal.projet_gestionautoplanningsecure.model.EmployeePreference;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeAvailabilityRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeePreferenceRepository;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeAvailabilityService {
    @Autowired
    private final EmployeeRepository employeeRepository;
    @Autowired
    private final EmployeeAvailabilityRepository availabilityRepository;
    @Autowired
    private final EmployeePreferenceRepository preferenceRepository;

    @Transactional
    public EmployeeAvailability addAvailability(Long companyId, Long employeeId, EmployeeAvailabilityRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .filter(e -> e.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        EmployeeAvailability availability = EmployeeAvailability.builder()
                .employee(employee)
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isRecurring(request.isRecurring())
                .build();

        return availabilityRepository.save(availability);
    }

    public List<EmployeeAvailability> getAvailabilities(Long companyId, Long employeeId) {
        if (!employeeRepository.findById(employeeId)
                .filter(e -> e.getCompany().getId().equals(companyId))
                .isPresent()) {
            throw new EntityNotFoundException("Employee not found");
        }

        return availabilityRepository.findByEmployeeId(employeeId);
    }

    @Transactional
    public void deleteAvailability(Long companyId, Long employeeId, Long availabilityId) {
        if (!employeeRepository.findById(employeeId)
                .filter(e -> e.getCompany().getId().equals(companyId))
                .isPresent()) {
            throw new EntityNotFoundException("Employee not found");
        }

        availabilityRepository.deleteById(availabilityId);
    }

    @Transactional
    public EmployeePreference updatePreferences(Long companyId, Long employeeId, EmployeeRequest.PreferenceRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .filter(e -> e.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        EmployeePreference preference = preferenceRepository.findByEmployeeId(employeeId)
                .orElse(EmployeePreference.builder().employee(employee).build());

        preference.setCanWorkWeekends(request.isCanWorkWeekends());
        preference.setCanWorkNights(request.isCanWorkNights());
        preference.setPrefersDay(request.isPrefersDay());
        preference.setPrefersNight(request.isPrefersNight());
        preference.setNoPreference(request.isNoPreference());
        preference.setCanWorkWeeks(request.isCanWorkWeeks());
        preference.setMinHoursPerDay(request.getMinHoursPerDay());
        preference.setMaxHoursPerDay(request.getMaxHoursPerDay());
        preference.setMinHoursPerWeek(request.getMinHoursPerWeek());
        preference.setMaxHoursPerWeek(request.getMaxHoursPerWeek());
        preference.setPreferredConsecutiveDays(request.getPreferredConsecutiveDays());
        preference.setMinConsecutiveDaysOff(request.getMinConsecutiveDaysOff());

        return preferenceRepository.save(preference);
    }

    // EmployeeAvailabilityService.java

    @Transactional
    public EmployeePreference updatePreferences(Long companyId,
                                                Long employeeId,
                                                EmployeePreferenceRequest req) {
        // on adapte vers l’autre DTO et on délègue
        return updatePreferences(companyId, employeeId, toPrefReq(req));
    }


    private static EmployeeRequest.PreferenceRequest toPrefReq(EmployeePreferenceRequest r) {
        return new EmployeeRequest.PreferenceRequest(
                r.isCanWorkWeekends(),
                r.isCanWorkWeeks(),
                r.isCanWorkNights(),
                r.isPrefersDay(),
                r.isPrefersNight(),
                r.isNoPreference(),
                r.getMinHoursPerDay(),
                r.getMaxHoursPerDay(),
                r.getMinHoursPerWeek(),
                r.getMaxHoursPerWeek(),
                r.getPreferredConsecutiveDays(),
                r.getMinConsecutiveDaysOff()
        );
    }


    public EmployeePreference getPreferences(Long companyId, Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .filter(e -> e.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

     /*   return preferenceRepository.findByEmployeeId(employeeId).orElseGet(() ->
                EmployeePreference.builder()
                        .employee(employee)
                        .canWorkWeekends(true)
                        .canWorkNights(true)
                        .prefersDay(false)
                        .prefersNight(false)
                        .noPreference(true)
                        .minHoursPerDay(0)
                        .maxHoursPerDay(24)
                        .minHoursPerWeek(0)
                        .maxHoursPerWeek(999) // plafond large
                        .preferredConsecutiveDays(0)
                        .minConsecutiveDaysOff(0)
                        .build()
        );

      */
        return preferenceRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> EmployeePreference.builder().employee(employee).build());
    }

}