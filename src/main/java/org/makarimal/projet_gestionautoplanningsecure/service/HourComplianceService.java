package org.makarimal.projet_gestionautoplanningsecure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HourComplianceService {

    private final ContractHourRequirementRepository requirementRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public List<ContractHourRequirement> initializeDefaultRequirements(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        List<ContractHourRequirement> requirements = new ArrayList<>();

        // FULL_TIME - 151h (légal français)
        if (!requirementRepository.existsByCompanyIdAndContractType(companyId, Employee.ContractType.FULL_TIME)) {
            requirements.add(ContractHourRequirement.builder()
                    .company(company)
                    .contractType(Employee.ContractType.FULL_TIME)
                    .minimumHoursPerMonth(151)
                    .description("Temps plein - 151h minimum légal")
                    .isActive(true)
                    .build());
        }

        // PART_TIME - 80h par défaut
        if (!requirementRepository.existsByCompanyIdAndContractType(companyId, Employee.ContractType.PART_TIME)) {
            requirements.add(ContractHourRequirement.builder()
                    .company(company)
                    .contractType(Employee.ContractType.PART_TIME)
                    .minimumHoursPerMonth(80)
                    .description("Temps partiel - 80h par défaut")
                    .isActive(true)
                    .build());
        }

        // TEMPORARY - 100h par défaut
        if (!requirementRepository.existsByCompanyIdAndContractType(companyId, Employee.ContractType.TEMPORARY)) {
            requirements.add(ContractHourRequirement.builder()
                    .company(company)
                    .contractType(Employee.ContractType.TEMPORARY)
                    .minimumHoursPerMonth(100)
                    .description("Temporaire - 100h par défaut")
                    .isActive(true)
                    .build());
        }

        // CONTRACT - 120h par défaut
        if (!requirementRepository.existsByCompanyIdAndContractType(companyId, Employee.ContractType.CONTRACT)) {
            requirements.add(ContractHourRequirement.builder()
                    .company(company)
                    .contractType(Employee.ContractType.CONTRACT)
                    .minimumHoursPerMonth(120)
                    .description("Contrat - 120h par défaut")
                    .isActive(true)
                    .build());
        }

        return requirementRepository.saveAll(requirements);
    }

    public List<ContractHourRequirement> getRequirements(Long companyId) {
        return requirementRepository.findByCompanyIdAndIsActive(companyId, true);
    }

    @Transactional
    public ContractHourRequirement updateRequirement(Long companyId, ContractHourRequirementRequest request) {
        ContractHourRequirement requirement = requirementRepository
                .findByCompanyIdAndContractType(companyId, request.getContractType())
                .orElseThrow(() -> new RuntimeException("Requirement not found"));

        // FULL_TIME ne peut pas être modifié (légal)
        if (request.getContractType() == Employee.ContractType.FULL_TIME) {
            throw new IllegalArgumentException("Les exigences FULL_TIME ne peuvent pas être modifiées (151h légal)");
        }

        requirement.setMinimumHoursPerMonth(request.getMinimumHoursPerMonth());
        requirement.setDescription(request.getDescription());

        return requirementRepository.save(requirement);
    }

    public ScheduleComplianceResponseDTO getScheduleCompliance(Long companyId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .filter(s -> s.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        List<ScheduleAssignment> assignments = assignmentRepository.findByScheduleId(scheduleId);

        // Grouper par employé
        Map<Employee, List<ScheduleAssignment>> assignmentsByEmployee = assignments.stream()
                .collect(Collectors.groupingBy(ScheduleAssignment::getEmployee));

        List<EmployeeHoursSummaryDTO> summaries = new ArrayList<>();
        int compliantCount = 0;

        for (Map.Entry<Employee, List<ScheduleAssignment>> entry : assignmentsByEmployee.entrySet()) {
            Employee employee = entry.getKey();
            List<ScheduleAssignment> empAssignments = entry.getValue();

            EmployeeHoursSummaryDTO summary = calculateEmployeeHours(companyId, employee, empAssignments);
            summaries.add(summary);

            if (summary.isCompliant()) {
                compliantCount++;
            }
        }

        int totalEmployees = summaries.size();
        double complianceRate = totalEmployees > 0 ? (compliantCount * 100.0) / totalEmployees : 0;

        return ScheduleComplianceResponseDTO.builder()
                .scheduleId(scheduleId)
                .scheduleName(schedule.getName())
                .month(schedule.getMonth())
                .year(schedule.getYear())
                .totalEmployees(totalEmployees)
                .compliantEmployees(compliantCount)
                .nonCompliantEmployees(totalEmployees - compliantCount)
                .overallComplianceRate(complianceRate)
                .employeeSummaries(summaries)
                .build();
    }

    public ScheduleComplianceResponseDTO getMonthlyCompliance(Long companyId, int month, int year) {
        List<Schedule> schedules = scheduleRepository.findByCompanyIdAndMonthAndYear(companyId, month, year);

        if (schedules.isEmpty()) {
            return ScheduleComplianceResponseDTO.builder()
                    .month(month)
                    .year(year)
                    .totalEmployees(0)
                    .compliantEmployees(0)
                    .nonCompliantEmployees(0)
                    .overallComplianceRate(0.0)
                    .employeeSummaries(new ArrayList<>())
                    .build();
        }

        // Combiner tous les assignments du mois
        List<ScheduleAssignment> allAssignments = schedules.stream()
                .flatMap(s -> assignmentRepository.findByScheduleId(s.getId()).stream())
                .collect(Collectors.toList());

        Map<Employee, List<ScheduleAssignment>> assignmentsByEmployee = allAssignments.stream()
                .collect(Collectors.groupingBy(ScheduleAssignment::getEmployee));

        List<EmployeeHoursSummaryDTO> summaries = new ArrayList<>();
        int compliantCount = 0;

        for (Map.Entry<Employee, List<ScheduleAssignment>> entry : assignmentsByEmployee.entrySet()) {
            Employee employee = entry.getKey();
            List<ScheduleAssignment> empAssignments = entry.getValue();

            EmployeeHoursSummaryDTO summary = calculateEmployeeHours(companyId, employee, empAssignments);
            summaries.add(summary);

            if (summary.isCompliant()) {
                compliantCount++;
            }
        }

        int totalEmployees = summaries.size();
        double complianceRate = totalEmployees > 0 ? (compliantCount * 100.0) / totalEmployees : 0;

        return ScheduleComplianceResponseDTO.builder()
                .month(month)
                .year(year)
                .totalEmployees(totalEmployees)
                .compliantEmployees(compliantCount)
                .nonCompliantEmployees(totalEmployees - compliantCount)
                .overallComplianceRate(complianceRate)
                .employeeSummaries(summaries)
                .build();
    }

    private EmployeeHoursSummaryDTO calculateEmployeeHours(Long companyId, Employee employee, List<ScheduleAssignment> assignments) {
        // Récupérer les exigences pour ce type de contrat
        ContractHourRequirement requirement = requirementRepository
                .findByCompanyIdAndContractType(companyId, employee.getContractType())
                .orElse(null);

        int requiredHours = requirement != null ? requirement.getMinimumHoursPerMonth() : 0;

        // Calculer les heures réelles
        int actualHours = assignments.stream()
                .mapToInt(ScheduleAssignment::getDuration)
                .sum() / 60; // Convertir minutes en heures

        int missingHours = Math.max(0, requiredHours - actualHours);
        boolean isCompliant = actualHours >= requiredHours;
        double compliancePercentage = requiredHours > 0 ? (actualHours * 100.0) / requiredHours : 100.0;

        return EmployeeHoursSummaryDTO.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .contractType(employee.getContractType())
                .requiredHours(requiredHours)
                .actualHours(actualHours)
                .missingHours(missingHours)
                .isCompliant(isCompliant)
                .compliancePercentage(Math.min(100.0, compliancePercentage))
                .build();
    }
}
