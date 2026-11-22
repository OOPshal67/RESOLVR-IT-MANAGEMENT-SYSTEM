package com.resolvr.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "technician_skills")
public class TechnicianSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_code", nullable = false, unique = true, length = 40)
    private String skillCode;

    @Column(name = "skill_name", nullable = false, length = 80)
    private String skillName;

    @Column(name = "category_code", nullable = false, length = 40)
    private String categoryCode;

    @Column(length = 255)
    private String description;

    public TechnicianSkill() {
    }

    public TechnicianSkill(String skillCode, String skillName, String categoryCode, String description) {
        this.skillCode = skillCode;
        this.skillName = skillName;
        this.categoryCode = categoryCode;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSkillCode() { return skillCode; }
    public void setSkillCode(String skillCode) { this.skillCode = skillCode; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TechnicianSkill that)) return false;
        return Objects.equals(skillCode, that.skillCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skillCode);
    }
}
