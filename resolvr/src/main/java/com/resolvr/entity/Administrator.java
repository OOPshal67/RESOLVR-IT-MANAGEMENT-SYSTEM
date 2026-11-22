package com.resolvr.entity;

import com.resolvr.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "administrators")
public class Administrator extends User {

    @Column(name = "admin_code", nullable = false, unique = true, length = 30)
    private String adminCode;

    @Column(name = "access_level", nullable = false, length = 40)
    private String accessLevel = "FULL_SYSTEM";

    @Column(name = "management_region", nullable = false, length = 60)
    private String managementRegion = "GLOBAL";

    public Administrator() {
        super();
    }

    public Administrator(String username, String email, String passwordHash, String fullName,
                         String department, String phone, String adminCode, String accessLevel, String managementRegion) {
        super(username, email, passwordHash, fullName, Role.ROLE_ADMIN, department, phone);
        this.adminCode = adminCode;
        this.accessLevel = accessLevel;
        this.managementRegion = managementRegion;
    }

    @Override
    public String getRoleIdentifierCode() {
        return adminCode;
    }

    public String getAdminCode() { return adminCode; }
    public void setAdminCode(String adminCode) { this.adminCode = adminCode; }
    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
    public String getManagementRegion() { return managementRegion; }
    public void setManagementRegion(String managementRegion) { this.managementRegion = managementRegion; }
}
