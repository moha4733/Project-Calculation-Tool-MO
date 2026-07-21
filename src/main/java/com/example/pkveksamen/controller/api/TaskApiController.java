package com.example.pkveksamen.controller.api;

import com.example.pkveksamen.dto.ErrorResponse;
import com.example.pkveksamen.dto.TaskNoteRequest;
import com.example.pkveksamen.dto.TaskRequest;
import com.example.pkveksamen.dto.TaskResponse;
import com.example.pkveksamen.dto.TaskStatusRequest;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;
import com.example.pkveksamen.model.SubProject;
import com.example.pkveksamen.model.Task;
import com.example.pkveksamen.service.ApiAuthorizationService;
import com.example.pkveksamen.service.EmployeeService;
import com.example.pkveksamen.service.ProjectService;
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
@RequestMapping("/api/projects/{projectId}/subprojects/{subProjectId}/tasks")
public class TaskApiController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final EmployeeService employeeService;
    private final ApiAuthorizationService apiAuthorizationService;

    public TaskApiController(TaskService taskService,
                             ProjectService projectService,
                             EmployeeService employeeService,
                             ApiAuthorizationService apiAuthorizationService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.employeeService = employeeService;
        this.apiAuthorizationService = apiAuthorizationService;
    }

    @GetMapping
    public ResponseEntity<?> getTasks(@PathVariable long projectId,
                                      @PathVariable long subProjectId,
                                      Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectAndSubProjectAccess(employee, projectId, subProjectId);
        if (accessError != null) {
            return accessError;
        }

        List<Task> tasks = employee.getRole() == EmployeeRole.PROJECT_MANAGER
                ? taskService.showTasksBySubProjectId(subProjectId)
                : taskService.showTasksBySubProjectIdAndEmployeeId(subProjectId, employee.getEmployeeId());

        List<TaskResponse> response = tasks.stream()
                .map(task -> TaskResponse.from(task, subProjectId))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createTask(@PathVariable long projectId,
                                        @PathVariable long subProjectId,
                                        @RequestBody TaskRequest request,
                                        Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        if (employee.getRole() != EmployeeRole.PROJECT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only project managers can create tasks"));
        }
        ResponseEntity<ErrorResponse> accessError = validateProjectAndSubProjectAccess(employee, projectId, subProjectId);
        if (accessError != null) {
            return accessError;
        }
        if (request.taskName() == null || request.taskName().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Task name is required"));
        }
        if (request.assignedToEmployeeId() != null
                && !apiAuthorizationService.isProjectMember(projectId, request.assignedToEmployeeId())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assigned employee must be a project member"));
        }

        SubProject subProject = projectService.getSubProjectBySubProjectID(subProjectId);
        String validationError = validateSubProjectPeriod(subProject, request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(validationError));
        }

        Task task = new Task();
        task.setTaskName(request.taskName());
        task.setTaskDescription(request.taskDescription());
        task.setTaskStartDate(request.taskStartDate());
        task.setTaskDeadline(request.taskDeadline());
        task.setTaskStatus(request.taskStatus() != null ? request.taskStatus() : Status.NOT_STARTED);
        task.setTaskPriority(request.taskPriority() != null ? request.taskPriority() : Priority.MEDIUM);
        task.setTaskNote(request.taskNote());
        task.recalculateDuration();

        Task createdTask = taskService.createTask(
                request.assignedToEmployeeId(),
                subProjectId,
                task.getTaskName(),
                task.getTaskDescription(),
                task.getTaskStatus(),
                task.getTaskStartDate(),
                task.getTaskDeadline(),
                task.getTaskDuration(),
                task.getTaskPriority(),
                task.getTaskNote()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(createdTask, subProjectId));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable long projectId,
                                        @PathVariable long subProjectId,
                                        @PathVariable long taskId,
                                        @RequestBody TaskRequest request,
                                        Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerSubProjectAndTaskAccess(employee, projectId, subProjectId, taskId, "update");
        if (accessError != null) {
            return accessError;
        }
        if (request.taskName() == null || request.taskName().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Task name is required"));
        }
        if (request.assignedToEmployeeId() != null
                && !apiAuthorizationService.isProjectMember(projectId, request.assignedToEmployeeId())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assigned employee must be a project member"));
        }

        SubProject subProject = projectService.getSubProjectBySubProjectID(subProjectId);
        String validationError = validateSubProjectPeriod(subProject, request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(validationError));
        }

        Task task = new Task();
        task.setTaskID((int) taskId);
        task.setTaskName(request.taskName());
        task.setTaskDescription(request.taskDescription());
        task.setTaskStartDate(request.taskStartDate());
        task.setTaskDeadline(request.taskDeadline());
        task.setTaskStatus(request.taskStatus() != null ? request.taskStatus() : Status.NOT_STARTED);
        task.setTaskPriority(request.taskPriority() != null ? request.taskPriority() : Priority.MEDIUM);
        task.setTaskNote(request.taskNote());
        if (request.assignedToEmployeeId() != null) {
            Employee assignedEmployee = new Employee();
            assignedEmployee.setEmployeeId(request.assignedToEmployeeId());
            task.setAssignedEmployee(assignedEmployee);
        }
        task.recalculateDuration();

        taskService.editTask(task);
        return ResponseEntity.ok(TaskResponse.from(taskService.getTaskById(taskId), subProjectId));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable long projectId,
                                        @PathVariable long subProjectId,
                                        @PathVariable long taskId,
                                        Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerSubProjectAndTaskAccess(employee, projectId, subProjectId, taskId, "delete");
        if (accessError != null) {
            return accessError;
        }

        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable long projectId,
                                              @PathVariable long subProjectId,
                                              @PathVariable long taskId,
                                              @RequestBody TaskStatusRequest request,
                                              Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateTaskProgressAccess(employee, projectId, subProjectId, taskId);
        if (accessError != null) {
            return accessError;
        }
        if (request.status() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Task status is required"));
        }

        taskService.updateTaskStatus(taskId, request.status());
        return ResponseEntity.ok(TaskResponse.from(taskService.getTaskById(taskId), subProjectId));
    }

    @PatchMapping("/{taskId}/note")
    public ResponseEntity<?> updateTaskNote(@PathVariable long projectId,
                                            @PathVariable long subProjectId,
                                            @PathVariable long taskId,
                                            @RequestBody TaskNoteRequest request,
                                            Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateTaskProgressAccess(employee, projectId, subProjectId, taskId);
        if (accessError != null) {
            return accessError;
        }

        taskService.updateTaskNote(taskId, request.note());
        return ResponseEntity.ok(TaskResponse.from(taskService.getTaskById(taskId), subProjectId));
    }

    private String validateSubProjectPeriod(SubProject subProject, TaskRequest request) {
        if (request.taskStartDate() != null
                && subProject.getSubProjectStartDate() != null
                && request.taskStartDate().isBefore(subProject.getSubProjectStartDate())) {
            return "Task start date must be within subproject period";
        }
        if (request.taskDeadline() != null
                && subProject.getSubProjectDeadline() != null
                && request.taskDeadline().isAfter(subProject.getSubProjectDeadline())) {
            return "Task deadline must be within subproject period";
        }
        if (request.taskStartDate() != null
                && request.taskDeadline() != null
                && request.taskDeadline().isBefore(request.taskStartDate())) {
            return "Task deadline cannot be before start date";
        }
        return null;
    }

    private ResponseEntity<ErrorResponse> validateProjectAndSubProjectAccess(Employee employee, long projectId, long subProjectId) {
        if (!apiAuthorizationService.canAccessProject(employee, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You do not have access to this project"));
        }
        if (!apiAuthorizationService.subProjectBelongsToProject(subProjectId, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Subproject does not belong to this project"));
        }
        return null;
    }

    private ResponseEntity<ErrorResponse> validateProjectManagerSubProjectAndTaskAccess(Employee employee,
                                                                                        long projectId,
                                                                                        long subProjectId,
                                                                                        long taskId,
                                                                                        String action) {
        if (employee.getRole() != EmployeeRole.PROJECT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only project managers can " + action + " tasks"));
        }
        ResponseEntity<ErrorResponse> accessError = validateProjectAndSubProjectAccess(employee, projectId, subProjectId);
        if (accessError != null) {
            return accessError;
        }
        if (!apiAuthorizationService.taskBelongsToSubProject(taskId, subProjectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Task does not belong to this subproject"));
        }
        return null;
    }

    private ResponseEntity<ErrorResponse> validateTaskProgressAccess(Employee employee,
                                                                     long projectId,
                                                                     long subProjectId,
                                                                     long taskId) {
        ResponseEntity<ErrorResponse> accessError = validateProjectAndSubProjectAccess(employee, projectId, subProjectId);
        if (accessError != null) {
            return accessError;
        }
        if (!apiAuthorizationService.taskBelongsToSubProject(taskId, subProjectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Task does not belong to this subproject"));
        }

        Task task = taskService.getTaskById(taskId);
        if (!apiAuthorizationService.canReadTask(employee, task)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You can only update your own tasks"));
        }
        return null;
    }

    private Employee currentEmployee(Authentication authentication) {
        return employeeService.getEmployeeByUsername(authentication.getName());
    }
}
