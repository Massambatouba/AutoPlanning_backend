package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.ScheduleAssignmentRepository;
import org.makarimal.projet_gestionautoplanningsecure.service.AssignmentGenerator;
import org.makarimal.projet_gestionautoplanningsecure.service.ScheduleGeneratorService;
import org.makarimal.projet_gestionautoplanningsecure.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final AssignmentGenerator generator;
    private final ScheduleAssignmentRepository assignmentRepository;
    private final ScheduleGeneratorService scheduleGeneratorService;



    @PostMapping("/generate")
    public ResponseEntity<Schedule> generateSchedule(
            @AuthenticationPrincipal User user,
            @RequestParam Long siteId,
            @RequestParam Integer month,
            @RequestParam Integer year) {

        Schedule schedule = scheduleGeneratorService
                .generateSchedule(
                        user.getCompany().getId(),
                        siteId,
                        month,
                        year
                );
        return ResponseEntity.ok(schedule);
    }


    /* ------------------------------------------------------------------ */
    /*  1.  Création / rafraîchissement d’un planning                     */
    /* ------------------------------------------------------------------ */

    @PostMapping
    public ResponseEntity<Schedule> createOrRefresh(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ScheduleRequest request) {

        Schedule schedule =
                scheduleService.createOrRefresh(user.getCompany().getId(), request);

        return ResponseEntity.ok(schedule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Schedule> updateSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ScheduleRequest request
    ) {
        Schedule schedule = scheduleService.updateSchedule(user.getCompany().getId(), id, request);
        return ResponseEntity.ok(schedule);
    }

    /* ------------------------------------------------------------------ */
    /*  2.  Génération automatique des affectations                       */
    /* ------------------------------------------------------------------ */

    @PostMapping("/{id}/generate-assignments")
    public ResponseEntity<Void> generate(@PathVariable Long id) {
        generator.generateForSchedule(id);   // on appelle la méthode, on ignore son "résultat" (void)
        return ResponseEntity.ok().build();  // on renvoie juste 200 OK
    }

    /* ------------------------------------------------------------------ */
    /*  3.  Lecture d’un planning                                         */
    /* ------------------------------------------------------------------ */

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Schedule schedule = scheduleService.getSchedule(user.getCompany().getId(), id);
        List<ScheduleAssignment> assigns = assignmentRepository.findByScheduleId(id);

        return ResponseEntity.ok(scheduleService.toDto(schedule, assigns));
    }

    /*  Liste filtrée                                                     */
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getSchedules(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean published) {

        List<Schedule> list = scheduleService.getSchedulesByFilters(
                user.getCompany().getId(), siteId, month, year, published);

        List<ScheduleResponse> dtos = list.stream()
                .map(schedule -> {
                    List<ScheduleAssignment> assigns = assignmentRepository.findByScheduleId(schedule.getId());
                    return scheduleService.toDto(schedule, assigns);
                }).toList();

        return ResponseEntity.ok(dtos);
    }

    /* ------------------------------------------------------------------ */
    /*  4.  Publication / envoi                                           */
    /* ------------------------------------------------------------------ */

    @PostMapping("/{id}/publish")
    public ResponseEntity<ScheduleResponse> publishSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Schedule updated = scheduleService.publishSchedule(user.getCompany().getId(), id);
        List<ScheduleAssignment> assigns = assignmentRepository.findByScheduleId(updated.getId());

        return ResponseEntity.ok(scheduleService.toDto(updated, assigns));
    }



    /* ------------------------------------------------------------------ */
    /*  5.  CRUD sur les affectations manuelles                           */
    /* ------------------------------------------------------------------ */

    @PostMapping("/{id}/assignments")
    public ResponseEntity<AssignmentDTO> addAssignment(
            @AuthenticationPrincipal User user,
            @PathVariable("id") Long scheduleId,
            @Valid @RequestBody ScheduleAssignmentRequest request) {

        ScheduleAssignment a = scheduleService.addAssignment(
                user.getCompany().getId(), scheduleId, request);

        return ResponseEntity.ok(AssignmentDTO.of(a));
    }


    @PutMapping("/assignments/{id}")
    public ResponseEntity<AssignmentDTO> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleAssignmentRequest request) {

        ScheduleAssignment updated = scheduleService.updateAssignment(id, request);
        return ResponseEntity.ok(AssignmentDTO.of(updated));
    }



    @GetMapping("/{id}/assignments")
    public ResponseEntity<List<ScheduleAssignment>> getScheduleAssignments(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Schedule schedule = scheduleService.getSchedule(user.getCompany().getId(), id);
        List<ScheduleAssignment> assignments = assignmentRepository.findByScheduleId(id);
        return ResponseEntity.ok(assignments);
    }


    @PostMapping("/{id}/validate")
    public ResponseEntity<ScheduleResponse> validate(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Schedule updated = scheduleService.validateSchedule(user.getCompany().getId(), id);
        List<ScheduleAssignment> assigns = assignmentRepository.findByScheduleId(updated.getId());

        return ResponseEntity.ok(scheduleService.toDto(updated, assigns));
    }


    @PostMapping("/{id}/send")
    public ResponseEntity<ScheduleResponse> sendSchedule(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        Schedule updated = scheduleService.sendSchedule(user.getCompany().getId(), id);
        List<ScheduleAssignment> assigns = assignmentRepository.findByScheduleId(updated.getId());

        return ResponseEntity.ok(scheduleService.toDto(updated, assigns));
    }



    @DeleteMapping("/{scheduleId}/assignments/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(
            @AuthenticationPrincipal User user,
            @PathVariable Long scheduleId,
            @PathVariable Long assignmentId) {

        scheduleService.deleteAssignment(
                user.getCompany().getId(), scheduleId, assignmentId);

        return ResponseEntity.noContent().build();
    }



    /* ------------------------------------------------------------------ */
    /*  6.  Planning par employé (vue calendrier)                         */
    /* ------------------------------------------------------------------ */

    @GetMapping("/employee-planning")
    public ResponseEntity<List<EmployeeScheduleDTO>> getEmployeePlanning(
            @AuthenticationPrincipal User user) {

        List<ScheduleAssignment> assignments =
                assignmentRepository.findBySchedule_Company_Id(user.getCompany().getId());

        Map<Employee, List<ScheduleAssignment>> grouped =
                assignments.stream()
                        .collect(Collectors.groupingBy(ScheduleAssignment::getEmployee));

        List<EmployeeScheduleDTO> result = grouped.entrySet().stream()
                .map(e -> {
                    Employee emp = e.getKey();
                    List<AssignmentDTO> dto = e.getValue().stream()
                            .map(AssignmentDTO::of)   // ← plus besoin de “new …”
                            .collect(Collectors.toList());
                    return new EmployeeScheduleDTO(
                            emp.getId(),
                            emp.getFirstName() + " " + emp.getLastName(),
                            dto);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
