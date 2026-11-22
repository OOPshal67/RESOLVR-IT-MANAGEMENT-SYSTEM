package com.resolvr.entity;

import com.resolvr.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees")
public class Employee extends User {

    @Column(name = "employee_code", nullable = false, unique = true, length = 30)
    private String employeeCode;

    @Column(name = "job_title", nullable = false, length = 80)
    private String jobTitle;

    @Column(name = "office_location", nullable = false, length = 80)
    private String officeLocation;

    public Employee() {
        super();
    }

    public Employee(String username, String email, String passwordHash, String fullName,
                    String department, String phone, String employeeCode, String jobTitle, String officeLocation) {
        super(username, email, passwordHash, fullName, Role.ROLE_EMPLOYEE, department, phone);
        this.employeeCode = employeeCode;
        this.jobTitle = jobTitle;
        this.officeLocation = officeLocation;
    }

    @Override
    public String getRoleIdentifierCode() {
        return employeeCode;
    }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getOfficeLocation() { return officeLocation; }
    public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }
}
