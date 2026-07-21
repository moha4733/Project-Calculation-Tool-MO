package com.example.pkveksamen.controller.api;

import com.example.pkveksamen.dto.EmployeeResponse;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeApiController {

    private final EmployeeService employeeService;

    public EmployeeApiController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me(Authentication authentication) {
        Employee employee = employeeService.getEmployeeByUsername(authentication.getName());
        if (employee == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(EmployeeResponse.from(employee));
    }

    @GetMapping
    public List<EmployeeResponse> getAllEmployees() {
        return employeeService.getAllEmployees()
                .stream()
                .map(EmployeeResponse::from)
                .toList();
    }
}
