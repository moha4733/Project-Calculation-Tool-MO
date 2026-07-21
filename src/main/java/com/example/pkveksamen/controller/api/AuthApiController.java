package com.example.pkveksamen.controller.api;

import com.example.pkveksamen.dto.AuthResponse;
import com.example.pkveksamen.dto.EmployeeResponse;
import com.example.pkveksamen.dto.ErrorResponse;
import com.example.pkveksamen.dto.LoginRequest;
import com.example.pkveksamen.dto.RegisterEmployeeRequest;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.security.JwtService;
import com.example.pkveksamen.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final EmployeeService employeeService;
    private final JwtService jwtService;

    public AuthApiController(EmployeeService employeeService, JwtService jwtService) {
        this.employeeService = employeeService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterEmployeeRequest request) {
        if (request.username() == null || request.password() == null || request.email() == null
                || request.role() == null || request.alphaRole() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Username, password, email, role and alphaRole are required"));
        }

        boolean created = employeeService.createEmployee(
                request.username(),
                request.password(),
                request.email(),
                request.role().getDisplayName(),
                request.alphaRole().getDisplayName()
        );

        if (!created) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Username or email is already in use"));
        }

        Employee employee = employeeService.getEmployeeByUsername(request.username());
        String token = jwtService.generateToken(employee);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, EmployeeResponse.from(employee)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Username and password are required"));
        }

        Integer employeeId = employeeService.validateLogin(request.username(), request.password());
        if (employeeId == null || employeeId <= 0) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid username or password"));
        }

        Employee employee = employeeService.getEmployeeById(employeeId);
        String token = jwtService.generateToken(employee);
        return ResponseEntity.ok(new AuthResponse(token, EmployeeResponse.from(employee)));
    }
}
