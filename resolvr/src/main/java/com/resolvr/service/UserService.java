package com.resolvr.service;

import com.resolvr.dto.RegistrationRequest;
import com.resolvr.entity.*;
import com.resolvr.exception.InvalidIncidentException;
import com.resolvr.exception.UserNotFoundException;
import com.resolvr.repository.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final TechnicianRepository technicianRepository;
    private final AdministratorRepository administratorRepository;
    private final TechnicianSkillRepository skillRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       EmployeeRepository employeeRepository,
                       TechnicianRepository technicianRepository,
                       AdministratorRepository administratorRepository,
                       TechnicianSkillRepository skillRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.technicianRepository = technicianRepository;
        this.administratorRepository = administratorRepository;
        this.skillRepository = skillRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public Employee registerEmployee(RegistrationRequest request) {
        AccountAccessService.requirePassword(request.getPassword());
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new InvalidIncidentException("Username '" + request.getUsername() + "' is already registered.");
        }
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new InvalidIncidentException("Email '" + request.getEmail() + "' is already registered.");
        }

        String empCode = "EMP" + employeeRepository.nextEmployeeNumber();
        Employee employee = new Employee(
            request.getUsername().trim(),
            request.getEmail().trim(),
            passwordEncoder.encode(request.getPassword()),
            request.getFullName().trim(),
            request.getDepartment().trim(),
            request.getPhone(),
            empCode,
            request.getJobTitle() != null ? request.getJobTitle() : "Employee",
            request.getOfficeLocation() != null ? request.getOfficeLocation() : "Main Campus"
        );
        Employee saved = employeeRepository.save(employee);
        auditService.recordSystemAction(saved.getUsername(), "ROLE_EMPLOYEE", "USER_REGISTERED",
                "Employee registered with code " + empCode, saved.getEmail());
        return saved;
    }

    public Technician createTechnician(String username, String email, String rawPassword, String fullName,
                                       String department, String phone, String specialization,
                                       int maxWorkload, int experienceYears, boolean teamLead,
                                       List<Long> skillIds, String actorUsername) {
        if (userRepository.existsByUsername(username.trim())) {
            throw new InvalidIncidentException("Username '" + username + "' already exists.");
        }
        AccountAccessService.requirePassword(rawPassword);
        String techCode = "TECH" + technicianRepository.nextTechnicianNumber();
        Technician tech = new Technician(
            username.trim(),
            email.trim(),
            passwordEncoder.encode(rawPassword),
            fullName.trim(),
            department.trim(),
            phone,
            techCode,
            specialization,
            maxWorkload,
            experienceYears,
            teamLead
        );
        if (skillIds != null && !skillIds.isEmpty()) {
            Set<TechnicianSkill> skills = new HashSet<>(skillRepository.findAllById(skillIds));
            tech.setSkills(skills);
        }
        Technician saved = technicianRepository.save(tech);
        auditService.recordSystemAction(actorUsername, "ROLE_ADMIN", "TECHNICIAN_CREATED",
                "Created technician " + saved.getFullName() + " (" + techCode + ")", specialization);
        return saved;
    }

    public void updateTechnicianSkillsAndAvailability(Long technicianId, boolean available, int maxWorkload,
                                                      List<Long> skillIds, String actorUsername) {
        Technician tech = getTechnicianById(technicianId);
        tech.setAvailable(available);
        tech.setMaxWorkload(Math.max(1, maxWorkload));
        if (skillIds != null) {
            tech.setSkills(new HashSet<>(skillRepository.findAllById(skillIds)));
        }
        technicianRepository.save(tech);
        auditService.recordSystemAction(actorUsername, "ROLE_ADMIN", "TECHNICIAN_UPDATED",
                "Updated skills/capacity for technician " + tech.getFullName(),
                "Available=" + available + ", MaxWorkload=" + maxWorkload);
    }

    public void toggleUserActiveStatus(Long userId, String actorUsername) {
        User user = getUserById(userId);
        if (user.getUsername().equals(actorUsername)) {
            throw new InvalidIncidentException("You cannot deactivate your own account.");
        }
        if (user.isActive()) {
            user.deactivate();
        } else {
            user.activate();
        }
        userRepository.save(user);
        auditService.recordSystemAction(actorUsername, "ROLE_ADMIN", "USER_STATUS_CHANGED",
                "Changed active status of " + user.getUsername() + " to " + user.isActive(), null);
    }

    public void updateUserProfile(User user, String fullName, String email, String department, String phone) {
        user = getUserById(user.getId());
        user.updateProfile(fullName, email, department, phone);
        userRepository.save(user);
        auditService.recordSystemAction(user.getUsername(), user.getRole().name(), "PROFILE_UPDATED", "Updated contact details", null);
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User ID not found: " + id));
    }

    @Transactional(readOnly = true)
    public Employee getEmployeeByUsername(String username) {
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Employee not found: " + username));
    }

    @Transactional(readOnly = true)
    public Technician getTechnicianByUsername(String username) {
        return technicianRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Technician not found: " + username));
    }

    @Transactional(readOnly = true)
    public Technician getTechnicianById(Long id) {
        return technicianRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Technician ID not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Technician> getAllTechnicians() {
        return technicianRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TechnicianSkill> getAllSkills() {
        return skillRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Administrator> getAllAdministrators() {
        return administratorRepository.findAll();
    }
}
