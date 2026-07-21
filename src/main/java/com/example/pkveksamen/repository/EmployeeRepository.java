package com.example.pkveksamen.repository;

import com.example.pkveksamen.entity.EmployeeEntity;
import com.example.pkveksamen.entity.RoleEntity;
import com.example.pkveksamen.model.AlphaRole;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.EmployeeRole;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeRepository {

    private final EmployeeJpaRepository employeeJpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeRepository(EmployeeJpaRepository employeeJpaRepository,
                              RoleJpaRepository roleJpaRepository,
                              PasswordEncoder passwordEncoder) {
        this.employeeJpaRepository = employeeJpaRepository;
        this.roleJpaRepository = roleJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void createEmployee(String username, String password, String email, String role, String alphaRoleDisplayName) {
        if (employeeJpaRepository.existsByUsernameOrEmail(username, email)) {
            throw new DataIntegrityViolationException("Username or email already exists");
        }

        RoleEntity alphaRole = roleJpaRepository.findByRoleName(alphaRoleDisplayName)
                .orElseGet(() -> roleJpaRepository.save(new RoleEntity(alphaRoleDisplayName, alphaRoleDisplayName)));

        EmployeeEntity employee = new EmployeeEntity();
        employee.setUsername(username);
        employee.setPassword(passwordEncoder.encode(password));
        employee.setEmail(email);
        employee.setRole(role);
        employee.getAlphaRoles().add(alphaRole);

        employeeJpaRepository.save(employee);
    }

    public Employee findEmployeeById(int employeeId) {
        return employeeJpaRepository.findById((long) employeeId)
                .map(this::toModel)
                .orElse(null);
    }

    public Employee findEmployeeByUsername(String username) {
        return employeeJpaRepository.findByUsername(username)
                .map(this::toModel)
                .orElse(null);
    }

    @Transactional
    public Integer validateLogin(String username, String password) {
        Optional<EmployeeEntity> employee = employeeJpaRepository.findByUsername(username);
        if (employee.isEmpty()) {
            return 0;
        }

        EmployeeEntity entity = employee.get();
        if (passwordEncoder.matches(password, entity.getPassword())) {
            return entity.getId().intValue();
        }

        if (password.equals(entity.getPassword())) {
            entity.setPassword(passwordEncoder.encode(password));
            employeeJpaRepository.save(entity);
            return entity.getId().intValue();
        }

        return 0;
    }

    public List<AlphaRole> findAlphaRolesByEmployeeId(int employeeId) {
        return employeeJpaRepository.findById((long) employeeId)
                .map(this::toAlphaRoles)
                .orElse(List.of());
    }

    public List<Employee> getAllTeamMembers() {
        return employeeJpaRepository.findByRole(EmployeeRole.TEAM_MEMBER.getDisplayName())
                .stream()
                .map(this::toModel)
                .toList();
    }

    public List<Employee> getAllEmployees() {
        return employeeJpaRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(EmployeeEntity::getId))
                .map(this::toModel)
                .toList();
    }

    private Employee toModel(EmployeeEntity entity) {
        Employee employee = new Employee();
        employee.setEmployeeId(entity.getId().intValue());
        employee.setUsername(entity.getUsername());
        employee.setPassword(entity.getPassword());
        employee.setEmail(entity.getEmail());
        employee.setRole(EmployeeRole.fromDisplayName(entity.getRole()));
        employee.setAlphaRoles(toAlphaRoles(entity));
        return employee;
    }

    private List<AlphaRole> toAlphaRoles(EmployeeEntity entity) {
        return entity.getAlphaRoles()
                .stream()
                .map(RoleEntity::getRoleName)
                .map(AlphaRole::fromDisplayName)
                .toList();
    }
}
