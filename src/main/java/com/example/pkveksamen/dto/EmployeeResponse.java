package com.example.pkveksamen.dto;

import com.example.pkveksamen.model.AlphaRole;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.EmployeeRole;

import java.util.List;

public record EmployeeResponse(
        int employeeId,
        String username,
        String email,
        EmployeeRole role,
        List<AlphaRole> alphaRoles
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getEmployeeId(),
                employee.getUsername(),
                employee.getEmail(),
                employee.getRole(),
                employee.getAlphaRoles()
        );
    }
}
