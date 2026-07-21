package com.example.pkveksamen.dto;

import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.Project;

import java.time.LocalDate;

public record ProjectResponse(
        long projectId,
        String projectName,
        String projectDescription,
        LocalDate projectStartDate,
        LocalDate projectDeadline,
        String projectCustomer,
        int projectDuration,
        EmployeeResponse owner
) {
    public static ProjectResponse from(Project project, Employee owner) {
        return new ProjectResponse(
                project.getProjectID(),
                project.getProjectName(),
                project.getProjectDescription(),
                project.getProjectStartDate(),
                project.getProjectDeadline(),
                project.getProjectCustomer(),
                project.getProjectDuration(),
                EmployeeResponse.from(owner)
        );
    }
}
