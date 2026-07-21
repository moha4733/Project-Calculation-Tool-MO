package com.example.pkveksamen.repository;

import com.example.pkveksamen.entity.SubTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubTaskJpaRepository extends JpaRepository<SubTaskEntity, Long> {
    List<SubTaskEntity> findByTaskIdOrderById(long taskId);

    boolean existsByIdAndTaskId(long id, long taskId);
}
