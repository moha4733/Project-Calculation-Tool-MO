package com.example.pkveksamen.repository;

import com.example.pkveksamen.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, Long> {
    Optional<EmployeeEntity> findByUsername(String username);

    Optional<EmployeeEntity> findByEmail(String email);

    boolean existsByUsernameOrEmail(String username, String email);

    List<EmployeeEntity> findByRole(String role);
}
