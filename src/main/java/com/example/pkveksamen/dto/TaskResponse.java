package com.example.pkveksamen.dto;

import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Task;
import com.example.pkveksamen.model.Status;

import java.time.LocalDate;

public record TaskResponse(
        long taskId,
        long subProjectId,
        String taskName,
        String taskDescription,
        LocalDate taskStartDate,
        LocalDate taskDeadline,
        int taskDuration,
        Status taskStatus,
        Priority taskPriority,
        String taskNote,
        EmployeeResponse assignedEmployee
) {
    public static TaskResponse from(Task task, long subProjectId) {
        return new TaskResponse(
                task.getTaskID(),
                subProjectId,
                task.getTaskName(),
                task.getTaskDescription(),
                task.getTaskStartDate(),
                task.getTaskDeadline(),
                task.getTaskDuration(),
                task.getTaskStatus(),
                task.getTaskPriority(),
                task.getTaskNote(),
                task.getAssignedEmployee() != null ? EmployeeResponse.from(task.getAssignedEmployee()) : null
        );
    }
}
