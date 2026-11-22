package com.resolvr.repository;

import com.resolvr.entity.Employee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    @Query(value = "select nextval('employee_number')", nativeQuery = true)
    long nextEmployeeNumber();

    Optional<Employee> findByUsername(String username);
    Optional<Employee> findByEmployeeCode(String employeeCode);
}
