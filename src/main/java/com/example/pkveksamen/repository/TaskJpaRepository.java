package com.example.pkveksamen.repository;

import com.example.pkveksamen.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, Long> {
    List<TaskEntity> findByAssignedEmployeeIdOrderById(long employeeId);

    List<TaskEntity> findBySubProjectIdOrderById(long subProjectId);

    List<TaskEntity> findBySubProjectIdAndAssignedEmployeeIdOrderById(long subProjectId, long employeeId);

    boolean existsByIdAndSubProjectId(long id, long subProjectId);
}
