package com.resolvr.entity;

import com.resolvr.enums.BusinessCriticality;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "it_services")
public class ITService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", nullable = false, unique = true, length = 40)
    private String serviceCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_criticality", nullable = false, length = 30)
    private BusinessCriticality businessCriticality;

    @Column(name = "owner_department", nullable = false, length = 80)
    private String ownerDepartment;

    @Column(name = "operational_status", nullable = false, length = 30)
    private String operationalStatus = "OPERATIONAL";

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ITService() {
    }

    public ITService(String serviceCode, String name, String description,
                     BusinessCriticality businessCriticality, String ownerDepartment, String operationalStatus) {
        this.serviceCode = serviceCode.toUpperCase();
        this.name = name;
        this.description = description;
        this.businessCriticality = businessCriticality;
        this.ownerDepartment = ownerDepartment;
        this.operationalStatus = operationalStatus;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode != null ? serviceCode.toUpperCase() : null; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BusinessCriticality getBusinessCriticality() { return businessCriticality; }
    public void setBusinessCriticality(BusinessCriticality businessCriticality) { this.businessCriticality = businessCriticality; }
    public String getOwnerDepartment() { return ownerDepartment; }
    public void setOwnerDepartment(String ownerDepartment) { this.ownerDepartment = ownerDepartment; }
    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
