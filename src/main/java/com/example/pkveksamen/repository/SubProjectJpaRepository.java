package com.example.pkveksamen.repository;

import com.example.pkveksamen.entity.SubProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubProjectJpaRepository extends JpaRepository<SubProjectEntity, Long> {
    List<SubProjectEntity> findByProjectIdOrderById(long projectId);

    boolean existsByIdAndProjectId(long id, long projectId);
}
