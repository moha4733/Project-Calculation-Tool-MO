package com.example.pkveksamen.dto;

import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;

import java.time.LocalDate;

public record SubTaskRequest(
        String subTaskName,
        String subTaskDescription,
        LocalDate subTaskStartDate,
        LocalDate subTaskDeadline,
        Status subTaskStatus,
        Priority subTaskPriority,
        String subTaskNote
) {
}
