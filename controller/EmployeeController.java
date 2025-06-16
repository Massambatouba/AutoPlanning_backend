package org.makarimal.projet_gestionautoplanningsecure.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.Employee;
import org.makarimal.projet_gestionautoplanningsecure.repository.EmployeeRepository;
import org.makarimal.projet_gestionautoplanningsecure.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    @Autowired
    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;

    /* ---------- C R U D ------------------------------------------------ */

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest body) {
        return ResponseEntity.ok(employeeService.create(body));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.get(id));
    }



    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> list(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Employee.ContractType contractType) {
        List<EmployeeResponse> dtos = employeeService.getByFilters(department, contractType);
        return ResponseEntity.ok(dtos);
    }
   /* @GetMapping("/all")
    public ResponseEntity<List<Employee>> listAll(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Employee.ContractType contractType) {

        return ResponseEntity.ok(employeeRepository.findAll());
    }

    */

    @GetMapping("/all")
    public ResponseEntity<List<EmployeeResponse>> listAll() {
        List<Employee> emps = employeeService.getAllForCurrentCompany();
        // si vous voulez renvoyer des DTOs plutôt que l’entité brute :
        List<EmployeeResponse> dtos = emps.stream()
                .map(employeeService::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }


    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest body) {

        return ResponseEntity.ok(employeeService.update(id, body));
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<EmployeeResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.toggleStatus(id));
    }
}
