package com.example.pkveksamen.dto;

import java.time.LocalDate;

public record SubProjectRequest(
        String subProjectName,
        String subProjectDescription,
        LocalDate subProjectStartDate,
        LocalDate subProjectDeadline
) {
}
