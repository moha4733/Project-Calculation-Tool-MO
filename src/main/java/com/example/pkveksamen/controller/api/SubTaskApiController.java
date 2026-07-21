package com.example.pkveksamen.controller.api;

import com.example.pkveksamen.dto.ErrorResponse;
import com.example.pkveksamen.dto.SubTaskRequest;
import com.example.pkveksamen.dto.SubTaskResponse;
import com.example.pkveksamen.dto.TaskNoteRequest;
import com.example.pkveksamen.dto.TaskStatusRequest;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;
import com.example.pkveksamen.model.SubTask;
import com.example.pkveksamen.model.Task;
import com.example.pkveksamen.service.ApiAuthorizationService;
import com.example.pkveksamen.service.EmployeeService;
import com.example.pkveksamen.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/subprojects/{subProjectId}/tasks/{taskId}/subtasks")
public class SubTaskApiController {

    private final TaskService taskService;
    private final EmployeeService employeeService;
    private final ApiAuthorizationService apiAuthorizationService;

    public SubTaskApiController(TaskService taskService,
                                EmployeeService employeeService,
                                ApiAuthorizationService apiAuthorizationService) {
        this.taskService = taskService;
        this.employeeService = employeeService;
        this.apiAuthorizationService = apiAuthorizationService;
    }

    @GetMapping
    public ResponseEntity<?> getSubTasks(@PathVariable long projectId,
                                         @PathVariable long subProjectId,
                                         @PathVariable long taskId,
                                         Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectSubProjectAndTaskAccess(employee, projectId, subProjectId, taskId);
        if (accessError != null) {
            return accessError;
        }

        Task task = taskService.getTaskById(taskId);
        if (!apiAuthorizationService.canReadTask(employee, task)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You can only view subtasks for your own tasks"));
        }

        List<SubTaskResponse> subTasks = taskService.showSubTasksByTaskId(taskId)
                .stream()
                .map(subTask -> SubTaskResponse.from(subTask, taskId))
                .toList();
        return ResponseEntity.ok(subTasks);
    }

    @PostMapping
    public ResponseEntity<?> createSubTask(@PathVariable long projectId,
                                           @PathVariable long subProjectId,
                                           @PathVariable long taskId,
                                           @RequestBody SubTaskRequest request,
                                           Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        if (employee.getRole() != EmployeeRole.PROJECT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only project managers can create subtasks"));
        }
        ResponseEntity<ErrorResponse> accessError = validateProjectSubProjectAndTaskAccess(employee, projectId, subProjectId, taskId);
        if (accessError != null) {
            return accessError;
        }
        if (request.subTaskName() == null || request.subTaskName().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Subtask name is required"));
        }

        Task task = taskService.getTaskById(taskId);
        String validationError = validateTaskPeriod(task, request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(validationError));
        }

        SubTask subTask = new SubTask();
        subTask.setSubTaskName(request.subTaskName());
        subTask.setSubTaskDescription(request.subTaskDescription());
        subTask.setSubTaskStartDate(request.subTaskStartDate());
        subTask.setSubTaskDeadline(request.subTaskDeadline());
        subTask.setSubTaskStatus(request.subTaskStatus() != null ? request.subTaskStatus() : Status.NOT_STARTED);
        subTask.setSubTaskPriority(request.subTaskPriority() != null ? request.subTaskPriority() : Priority.MEDIUM);
        subTask.setSubTaskNote(request.subTaskNote());
        subTask.recalculateDuration();

        SubTask createdSubTask = taskService.createSubTask(
                taskId,
                subTask.getSubTaskName(),
                subTask.getSubTaskDescription(),
                subTask.getSubTaskStatus().name(),
                subTask.getSubTaskStartDate(),
                subTask.getSubTaskDeadline(),
                subTask.getSubTaskDuration(),
                subTask.getSubTaskPriority().name(),
                subTask.getSubTaskNote()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(SubTaskResponse.from(createdSubTask, taskId));
    }

    @PutMapping("/{subTaskId}")
    public ResponseEntity<?> updateSubTask(@PathVariable long projectId,
                                           @PathVariable long subProjectId,
                                           @PathVariable long taskId,
                                           @PathVariable long subTaskId,
                                           @RequestBody SubTaskRequest request,
                                           Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerTaskAndSubTaskAccess(
                employee,
                projectId,
                subProjectId,
                taskId,
                subTaskId,
                "update"
        );
        if (accessError != null) {
            return accessError;
        }
        if (request.subTaskName() == null || request.subTaskName().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Subtask name is required"));
        }

        Task task = taskService.getTaskById(taskId);
        String validationError = validateTaskPeriod(task, request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(validationError));
        }

        SubTask subTask = new SubTask();
        subTask.setSubTaskId(subTaskId);
        subTask.setSubTaskName(request.subTaskName());
        subTask.setSubTaskDescription(request.subTaskDescription());
        subTask.setSubTaskStartDate(request.subTaskStartDate());
        subTask.setSubTaskDeadline(request.subTaskDeadline());
        subTask.setSubTaskStatus(request.subTaskStatus() != null ? request.subTaskStatus() : Status.NOT_STARTED);
        subTask.setSubTaskPriority(request.subTaskPriority() != null ? request.subTaskPriority() : Priority.MEDIUM);
        subTask.setSubTaskNote(request.subTaskNote());
        subTask.recalculateDuration();

        taskService.editSubTask(subTask);
        return ResponseEntity.ok(SubTaskResponse.from(taskService.getSubTaskById(subTaskId), taskId));
    }

    @DeleteMapping("/{subTaskId}")
    public ResponseEntity<?> deleteSubTask(@PathVariable long projectId,
                                           @PathVariable long subProjectId,
                                           @PathVariable long taskId,
                                           @PathVariable long subTaskId,
                                           Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerTaskAndSubTaskAccess(
                employee,
                projectId,
                subProjectId,
                taskId,
                subTaskId,
                "delete"
        );
        if (accessError != null) {
            return accessError;
        }

        taskService.deleteSubTask(subTaskId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{subTaskId}/status")
    public ResponseEntity<?> updateSubTaskStatus(@PathVariable long projectId,
                                                 @PathVariable long subProjectId,
                                                 @PathVariable long taskId,
                                                 @PathVariable long subTaskId,
                                                 @RequestBody TaskStatusRequest request,
                                                 Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateSubTaskProgressAccess(employee, projectId, subProjectId, taskId, subTaskId);
        if (accessError != null) {
            return accessError;
        }
        if (request.status() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Subtask status is required"));
        }

        taskService.updateSubTaskStatus(subTaskId, request.status());
        return ResponseEntity.ok(SubTaskResponse.from(taskService.getSubTaskById(subTaskId), taskId));
    }

    @PatchMapping("/{subTaskId}/note")
    public ResponseEntity<?> updateSubTaskNote(@PathVariable long projectId,
                                               @PathVariable long subProjectId,
                                               @PathVariable long taskId,
                                               @PathVariable long subTaskId,
                                               @RequestBody TaskNoteRequest request,
                                               Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateSubTaskProgressAccess(employee, projectId, subProjectId, taskId, subTaskId);
        if (accessError != null) {
            return accessError;
        }

        taskService.updateSubTaskNote(subTaskId, request.note());
        return ResponseEntity.ok(SubTaskResponse.from(taskService.getSubTaskById(subTaskId), taskId));
    }

    private String validateTaskPeriod(Task task, SubTaskRequest request) {
        if (request.subTaskStartDate() != null
                && task.getTaskStartDate() != null
                && request.subTaskStartDate().isBefore(task.getTaskStartDate())) {
            return "Subtask start date must be within task period";
        }
        if (request.subTaskDeadline() != null
                && task.getTaskDeadline() != null
                && request.subTaskDeadline().isAfter(task.getTaskDeadline())) {
            return "Subtask deadline must be within task period";
        }
        if (request.subTaskStartDate() != null
                && request.subTaskDeadline() != null
                && request.subTaskDeadline().isBefore(request.subTaskStartDate())) {
            return "Subtask deadline cannot be before start date";
        }
        return null;
    }

    private ResponseEntity<ErrorResponse> validateProjectSubProjectAndTaskAccess(Employee employee,
                                                                                 long projectId,
                                                                                 long subProjectId,
                                                                                 long taskId) {
        if (!apiAuthorizationService.canAccessProject(employee, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You do not have access to this project"));
        }
        if (!apiAuthorizationService.subProjectBelongsToProject(subProjectId, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Subproject does not belong to this project"));
        }
        if (!apiAuthorizationService.taskBelongsToSubProject(taskId, subProjectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Task does not belong to this subproject"));
        }
        return null;
    }

    private ResponseEntity<ErrorResponse> validateProjectManagerTaskAndSubTaskAccess(Employee employee,
                                                                                     long projectId,
                                                                                     long subProjectId,
                                                                                     long taskId,
                                                                                     long subTaskId,
                                                                                     String action) {
        if (employee.getRole() != EmployeeRole.PROJECT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only project managers can " + action + " subtasks"));
        }
        ResponseEntity<ErrorResponse> accessError = validateProjectSubProjectAndTaskAccess(employee, projectId, subProjectId, taskId);
        if (accessError != null) {
            return accessError;
        }
        if (!apiAuthorizationService.subTaskBelongsToTask(subTaskId, taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Subtask does not belong to this task"));
        }
        return null;
    }

    private ResponseEntity<ErrorResponse> validateSubTaskProgressAccess(Employee employee,
                                                                        long projectId,
                                                                        long subProjectId,
                                                                        long taskId,
                                                                        long subTaskId) {
        ResponseEntity<ErrorResponse> accessError = validateProjectSubProjectAndTaskAccess(employee, projectId, subProjectId, taskId);
        if (accessError != null) {
            return accessError;
        }
        if (!apiAuthorizationService.subTaskBelongsToTask(subTaskId, taskId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Subtask does not belong to this task"));
        }

        Task task = taskService.getTaskById(taskId);
        if (!apiAuthorizationService.canReadTask(employee, task)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You can only update subtasks for your own tasks"));
        }
        return null;
    }

    private Employee currentEmployee(Authentication authentication) {
        return employeeService.getEmployeeByUsername(authentication.getName());
    }
}
