package com.example.pkveksamen.dto;

import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;

import java.time.LocalDate;

public record TaskRequest(
        String taskName,
        String taskDescription,
        LocalDate taskStartDate,
        LocalDate taskDeadline,
        Status taskStatus,
        Priority taskPriority,
        String taskNote,
        Integer assignedToEmployeeId
) {
}
