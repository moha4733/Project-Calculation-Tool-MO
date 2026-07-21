package com.example.pkveksamen.dto;

import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;
import com.example.pkveksamen.model.SubTask;

import java.time.LocalDate;

public record SubTaskResponse(
        long subTaskId,
        long taskId,
        String subTaskName,
        String subTaskDescription,
        LocalDate subTaskStartDate,
        LocalDate subTaskDeadline,
        int subTaskDuration,
        Status subTaskStatus,
        Priority subTaskPriority,
        String subTaskNote
) {
    public static SubTaskResponse from(SubTask subTask, long taskId) {
        return new SubTaskResponse(
                subTask.getSubTaskId(),
                taskId,
                subTask.getSubTaskName(),
                subTask.getSubTaskDescription(),
                subTask.getSubTaskStartDate(),
                subTask.getSubTaskDeadline(),
                subTask.getSubTaskDuration(),
                subTask.getSubTaskStatus(),
                subTask.getSubTaskPriority(),
                subTask.getSubTaskNote()
        );
    }
}
