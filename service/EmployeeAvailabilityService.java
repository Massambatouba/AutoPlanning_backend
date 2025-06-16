package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeeAvailabilityRequest;
import org.makarimal.projet_gestionautoplanningsecure.dto.EmployeePreferenceRequest;
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
    public EmployeePreference updatePreferences(Long companyId, Long employeeId, EmployeePreferenceRequest request) {
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
        preference.setMinHoursPerDay(request.getMinHoursPerDay());
        preference.setMaxHoursPerDay(request.getMaxHoursPerDay());
        preference.setMinHoursPerWeek(request.getMinHoursPerWeek());
        preference.setMaxHoursPerWeek(request.getMaxHoursPerWeek());
        preference.setPreferredConsecutiveDays(request.getPreferredConsecutiveDays());
        preference.setMinConsecutiveDaysOff(request.getMinConsecutiveDaysOff());

        return preferenceRepository.save(preference);
    }

    public EmployeePreference getPreferences(Long companyId, Long employeeId) {
        if (!employeeRepository.findById(employeeId)
                .filter(e -> e.getCompany().getId().equals(companyId))
                .isPresent()) {
            throw new EntityNotFoundException("Employee not found");
        }

        return preferenceRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee preferences not found"));
    }
}