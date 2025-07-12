package org.makarimal.projet_gestionautoplanningsecure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.makarimal.projet_gestionautoplanningsecure.dto.*;
import org.makarimal.projet_gestionautoplanningsecure.model.*;
import org.makarimal.projet_gestionautoplanningsecure.repository.*;
import org.makarimal.projet_gestionautoplanningsecure.util.AuthServiceHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    /* ---------- Dépendances ---------- */
    private final EmployeeRepository   employeeRepo;
    private final CompanyRepository    companyRepo;
    private final SiteRepository       siteRepo;
    private final AuthServiceHelper    auth;
    private final EmployeeAbsenceRepository employeeAbsenceRepo;

    /* ====================================================================== */
    /*  1.  CRUD                                                              */
    /* ====================================================================== */

    /** Création ----------------------------------------------------------- */
    @Transactional
    public EmployeeResponse create(EmployeeRequest req) {

        Company company = auth.getCurrentUser().getCompany();

        /* unicité mail / code ----------- */
        if (employeeRepo.existsByEmailAndCompanyId(req.getEmail(), company.getId()))
            throw new IllegalArgumentException("Email déjà utilisé dans l’entreprise");

        if (employeeRepo.existsByEmployeeCodeAndCompanyId(req.getEmployeeCode(), company.getId()))
            throw new IllegalArgumentException("Code employé déjà utilisé dans l’entreprise");

        /* site éventuel ----------------- */
        Site site = null;
        if (req.getSiteId() != null) {
            site = siteRepo.findById(req.getSiteId())
                    .filter(s -> s.getCompany().getId().equals(company.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Site inconnu pour l’entreprise"));
        }

        /* constitution de l’entité ------ */
        Employee emp = Employee.builder()
                .company(company)
                .site(site)
                .firstName(req.getFirstName())
                .lastName (req.getLastName())
                .email    (req.getEmail())
                .phone    (req.getPhone())
                .position (req.getPosition())
                .department(req.getDepartment())
                .employeeCode(req.getEmployeeCode())
                .contractType(req.getContractType())
                .maxHoursPerWeek(req.getMaxHoursPerWeek())
                .preferredSites(new ArrayList<>(req.getPreferredSites()))
                .skillSets(new ArrayList<>(req.getSkillSets()))
                .adress (req.getAdress())
                .zipCode(req.getZipCode())
                .city   (req.getCity())
                .country(req.getCountry())
                .agentTypes(new HashSet<>(req.getAgentTypes()))
                .isActive(true)
                .build();

        EmployeePreference pref = EmployeePreference.builder()
                .employee(emp)
                .canWorkWeekends(req.getPreferences().isCanWorkWeekends())
                .canWorkWeeks(req.getPreferences().isCanWorkWeeks())
                .canWorkNights(req.getPreferences().isCanWorkNights())
                .prefersDay(req.getPreferences().isPrefersDay())
                .prefersNight(req.getPreferences().isPrefersNight())
                .noPreference(req.getPreferences().isNoPreference())
                .minHoursPerDay(req.getPreferences().getMinHoursPerDay())
                .maxHoursPerDay(req.getPreferences().getMaxHoursPerDay())
                .minHoursPerWeek(req.getPreferences().getMinHoursPerWeek())
                .maxHoursPerWeek(req.getPreferences().getMaxHoursPerWeek())
                .preferredConsecutiveDays(req.getPreferences().getPreferredConsecutiveDays())
                .minConsecutiveDaysOff(req.getPreferences().getMinConsecutiveDaysOff())
                .build();

        emp.setPreference(pref);

        emp = employeeRepo.save(emp);
        return toDto(emp);
    }

    public List<Employee> getAllForCurrentCompany() {
        Long companyId = auth.getCurrentUser().getCompany().getId();
        return employeeRepo.findByCompanyId(companyId);
    }

    /*

    public List<EmployeeResponse> getAllForCurrentCompany() {
        Long companyId = auth.getCurrentUser().getCompany().getId();
        return employeeRepo.findByCompanyId(companyId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

     */

    public EmployeeResponse toDto(Employee e) {
        // Préférences : on gère le cas où e.getPreference() est null
        EmployeeResponse.PreferenceResponse pr;
        if (e.getPreference() != null) {
            pr = EmployeeResponse.PreferenceResponse.builder()
                    .canWorkWeekends(e.getPreference().isCanWorkWeekends())
                    .canWorkWeeks(e.getPreference().isCanWorkWeeks())
                    .canWorkNights(e.getPreference().isCanWorkNights())
                    .prefersDay(e.getPreference().isPrefersDay())
                    .prefersNight(e.getPreference().isPrefersNight())
                    .noPreference(e.getPreference().isNoPreference())
                    .minHoursPerDay(e.getPreference().getMinHoursPerDay())
                    .maxHoursPerDay(e.getPreference().getMaxHoursPerDay())
                    .minHoursPerWeek(e.getPreference().getMinHoursPerWeek())
                    .maxHoursPerWeek(e.getPreference().getMaxHoursPerWeek())
                    .preferredConsecutiveDays(e.getPreference().getPreferredConsecutiveDays())
                    .minConsecutiveDaysOff(e.getPreference().getMinConsecutiveDaysOff())
                    .build();
        } else {
            // valeurs par défaut (toutes les booleans à false, les Integer à 0)
            pr = EmployeeResponse.PreferenceResponse.builder()
                    .canWorkWeekends(false)
                    .canWorkWeeks(false)
                    .canWorkNights(false)
                    .prefersDay(false)
                    .prefersNight(false)
                    .noPreference(false)
                    .minHoursPerDay(0)
                    .maxHoursPerDay(0)
                    .minHoursPerWeek(0)
                    .maxHoursPerWeek(0)
                    .preferredConsecutiveDays(0)
                    .minConsecutiveDaysOff(0)
                    .build();
        }

        return EmployeeResponse.builder()
                .id(e.getId())
                .companyId(e.getCompany().getId())
                .siteId(e.getSite() != null ? e.getSite().getId() : null)
                .siteName(e.getSite() != null ? e.getSite().getName() : null)
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .position(e.getPosition())
                .department(e.getDepartment())
                .employeeCode(e.getEmployeeCode())
                .contractType(e.getContractType())
                .maxHoursPerWeek(e.getMaxHoursPerWeek())
                .preferredSites(new HashSet<>(e.getPreferredSites()))
                .skillSets(new HashSet<>(e.getSkillSets()))
                .agentTypes(e.getAgentTypes())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .adress (e.getAdress())
                .zipCode(e.getZipCode())
                .city   (e.getCity())
                .country(e.getCountry())
                .preferences(pr)
                .build();
    }


    /** Lecture par id (DTO) ---------------------------------------------- */
    public EmployeeResponse get(Long id) {
        Employee entity = loadEntity(id);
        return toDto(entity);
    }


    public List<Employee> getActiveEmployeesForSite(Long siteId) {
        return employeeRepo.findBySiteIdAndIsActiveTrue(siteId);
    }

    public List<EmployeePlanningDTO> getIsActiveEmployeesForSite(Long siteId) {
        return employeeRepo.findActiveBySite(siteId);
    }






    /** Liste filtrée (DTO) ------------------------------------------------ */
    public List<EmployeeResponse> getByFilters(String department,
                                               Employee.ContractType type) {

        Company company = auth.getCurrentUser().getCompany();
        return employeeRepo
                .findByFilters(company.getId(), department, type)
                .stream()
                .map(this::toDto)
                .toList();
    }



    /** Mise à jour -------------------------------------------------------- */
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest req) {

        Employee emp = loadEntity(id);
        Company  company = emp.getCompany();

        /* collision email / code -------- */
        if (!emp.getEmail().equals(req.getEmail()) &&
                employeeRepo.existsByEmailAndCompanyId(req.getEmail(), company.getId()))
            throw new IllegalArgumentException("Email déjà utilisé");

        if (!emp.getEmployeeCode().equals(req.getEmployeeCode()) &&
                employeeRepo.existsByEmployeeCodeAndCompanyId(req.getEmployeeCode(), company.getId()))
            throw new IllegalArgumentException("Code employé déjà utilisé");

        /* site éventuel ----------------- */
        Site site = null;
        if (req.getSiteId() != null) {
            site = siteRepo.findById(req.getSiteId())
                    .filter(s -> s.getCompany().getId().equals(company.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("Site inconnu"));
        }

        /* mise à jour ------------------- */
        emp.setSite(site);
        emp.setFirstName(req.getFirstName());
        emp.setLastName (req.getLastName());
        emp.setEmail    (req.getEmail());
        emp.setPhone    (req.getPhone());
        emp.setPosition (req.getPosition());
        emp.setDepartment(req.getDepartment());
        emp.setEmployeeCode(req.getEmployeeCode());
        emp.setContractType(req.getContractType());
        emp.setAdress (req.getAdress());
        emp.setZipCode(req.getZipCode());
        emp.setCity   (req.getCity());
        emp.setCountry(req.getCountry());
        emp.setMaxHoursPerWeek(req.getMaxHoursPerWeek());
        emp.setPreferredSites(new ArrayList<>(req.getPreferredSites()));
        emp.setSkillSets(new ArrayList<>(req.getSkillSets()));
        emp.setAgentTypes(new HashSet<>(req.getAgentTypes()));

        employeeRepo.save(emp);
        return toDto(emp);
    }

    /** (Dés)activation ---------------------------------------------------- */
    @Transactional
    public EmployeeResponse toggleStatus(Long id) {
        Employee emp = loadEntity(id);
        emp.setActive(!emp.isActive());
        employeeRepo.save(emp);
        return toDto(emp);
    }

    /* ====================================================================== */
    /*  2.  Aide - absence planifiée                                         */
    /* ====================================================================== */
    public boolean isAbsent(Long employeeId, LocalDate date) {
        List<AbsenceType> planned = List.of(
                AbsenceType.CONGE_PAYE,
                AbsenceType.CONGE_SANS_SOLDE,
                AbsenceType.MALADIE,
                AbsenceType.CONGE_PARENTAL,
                AbsenceType.AUTRE
        );
        return employeeAbsenceRepo.existsByEmployeeIdAndTypeInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                employeeId, planned, date, date);
    }

    /* ====================================================================== */
    /*  3.  Méthodes privées                                                 */
    /* ====================================================================== */

    /** Charge l’entité (sécurisé entreprise) */
    private Employee loadEntity(Long id) {
        Company company = auth.getCurrentUser().getCompany();
        return employeeRepo.findById(id)
                .filter(e -> e.getCompany().getId().equals(company.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Employé introuvable"));
    }

}
