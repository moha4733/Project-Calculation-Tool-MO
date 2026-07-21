package com.example.pkveksamen.dto;

import com.example.pkveksamen.model.SubProject;

import java.time.LocalDate;

public record SubProjectResponse(
        long subProjectId,
        long projectId,
        String subProjectName,
        String subProjectDescription,
        LocalDate subProjectStartDate,
        LocalDate subProjectDeadline,
        int subProjectDuration
) {
    public static SubProjectResponse from(SubProject subProject, long projectId) {
        return new SubProjectResponse(
                subProject.getSubProjectID(),
                projectId,
                subProject.getSubProjectName(),
                subProject.getSubProjectDescription(),
                subProject.getSubProjectStartDate(),
                subProject.getSubProjectDeadline(),
                subProject.getSubProjectDuration()
        );
    }
}
