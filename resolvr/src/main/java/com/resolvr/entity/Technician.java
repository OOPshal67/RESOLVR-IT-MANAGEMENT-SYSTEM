package com.resolvr.entity;

import com.resolvr.enums.Role;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "technicians")
public class Technician extends User {

    @Column(name = "technician_code", nullable = false, unique = true, length = 30)
    private String technicianCode;

    @Column(nullable = false, length = 80)
    private String specialization;

    @Column(name = "max_workload", nullable = false)
    private int maxWorkload = 5;

    @Column(name = "current_workload", nullable = false)
    private int currentWorkload = 0;

    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "experience_years", nullable = false)
    private int experienceYears = 1;

    @Column(name = "team_lead", nullable = false)
    private boolean teamLead = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "technician_skill_mapping",
        joinColumns = @JoinColumn(name = "technician_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<TechnicianSkill> skills = new HashSet<>();

    public Technician() {
        super();
    }

    public Technician(String username, String email, String passwordHash, String fullName,
                      String department, String phone, String technicianCode, String specialization,
                      int maxWorkload, int experienceYears, boolean teamLead) {
        super(username, email, passwordHash, fullName, Role.ROLE_TECHNICIAN, department, phone);
        this.technicianCode = technicianCode;
        this.specialization = specialization;
        this.maxWorkload = Math.max(1, maxWorkload);
        this.experienceYears = Math.max(0, experienceYears);
        this.teamLead = teamLead;
        this.available = true;
        this.currentWorkload = 0;
    }

    @Override
    public String getRoleIdentifierCode() {
        return technicianCode;
    }

    public boolean canAcceptAssignment() {
        return isActive() && available && currentWorkload < maxWorkload;
    }

    public void incrementWorkload() {
        this.currentWorkload++;
    }

    public void decrementWorkload() {
        if (this.currentWorkload > 0) {
            this.currentWorkload--;
        }
    }

    public void addSkill(TechnicianSkill skill) {
        if (skill != null) {
            this.skills.add(skill);
        }
    }

    public boolean hasSkillForCategory(String categoryCode) {
        if (categoryCode == null) return false;
        return skills.stream().anyMatch(s ->
            categoryCode.equalsIgnoreCase(s.getCategoryCode()) ||
            categoryCode.equalsIgnoreCase(s.getSkillCode())
        );
    }

    public int getUtilizationPercentage() {
        if (maxWorkload <= 0) return 100;
        return Math.min(100, (int) Math.round((currentWorkload * 100.0) / maxWorkload));
    }

    public String getTechnicianCode() { return technicianCode; }
    public void setTechnicianCode(String technicianCode) { this.technicianCode = technicianCode; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public int getMaxWorkload() { return maxWorkload; }
    public void setMaxWorkload(int maxWorkload) { this.maxWorkload = Math.max(1, maxWorkload); }
    public int getCurrentWorkload() { return currentWorkload; }
    public void setCurrentWorkload(int currentWorkload) { this.currentWorkload = Math.max(0, currentWorkload); }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = Math.max(0, experienceYears); }
    public boolean isTeamLead() { return teamLead; }
    public void setTeamLead(boolean teamLead) { this.teamLead = teamLead; }
    public Set<TechnicianSkill> getSkills() { return skills; }
    public void setSkills(Set<TechnicianSkill> skills) { this.skills = skills; }
}
