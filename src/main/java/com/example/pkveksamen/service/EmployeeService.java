package com.example.pkveksamen.service;

import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public boolean createEmployee(String username, String password, String email, String role, String alphaRoleDisplayName) {
        try {
            employeeRepository.createEmployee(username, password, email, role, alphaRoleDisplayName);
            logger.info("Employee created: username={}, email={}, alphaRole={}", username, email, alphaRoleDisplayName);
            return true;
        } catch (DataIntegrityViolationException e) {
            logger.warn("Employee could not be created because email is already in use: {}", email);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error while creating employee with email={}", email, e);
            return false;
        }
    }

    public Integer validateLogin(String username, String password) {
        return employeeRepository.validateLogin(username, password);
    }

    public Employee getEmployeeById(int employeeId) {
        return employeeRepository.findEmployeeById(employeeId);
    }

    public Employee getEmployeeByUsername(String username) {
        return employeeRepository.findEmployeeByUsername(username);
    }

    public List<Employee> getAllTeamMembers() {
        return employeeRepository.getAllTeamMembers();
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.getAllEmployees();
    }
}
