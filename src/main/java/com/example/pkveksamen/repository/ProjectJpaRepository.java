package com.example.pkveksamen.repository;

import com.example.pkveksamen.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectJpaRepository extends JpaRepository<ProjectEntity, Long> {

    @Query("""
            SELECT DISTINCT p
            FROM ProjectEntity p
            LEFT JOIN p.members m
            WHERE p.owner.id = :employeeId OR m.id = :employeeId
            ORDER BY p.id
            """)
    List<ProjectEntity> findVisibleProjectsForEmployee(@Param("employeeId") long employeeId);

    @Query("""
            SELECT COUNT(p) > 0
            FROM ProjectEntity p
            LEFT JOIN p.members m
            WHERE p.id = :projectId
            AND (p.owner.id = :employeeId OR m.id = :employeeId)
            """)
    boolean existsVisibleProjectForEmployee(@Param("projectId") long projectId, @Param("employeeId") long employeeId);
}
