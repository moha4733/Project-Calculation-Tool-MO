package com.example.pkveksamen.controller.api;

import com.example.pkveksamen.dto.ErrorResponse;
import com.example.pkveksamen.dto.SubProjectRequest;
import com.example.pkveksamen.dto.SubProjectResponse;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.Project;
import com.example.pkveksamen.model.SubProject;
import com.example.pkveksamen.service.ApiAuthorizationService;
import com.example.pkveksamen.service.EmployeeService;
import com.example.pkveksamen.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/subprojects")
public class SubProjectApiController {

    private final ProjectService projectService;
    private final EmployeeService employeeService;
    private final ApiAuthorizationService apiAuthorizationService;

    public SubProjectApiController(ProjectService projectService,
                                   EmployeeService employeeService,
                                   ApiAuthorizationService apiAuthorizationService) {
        this.projectService = projectService;
        this.employeeService = employeeService;
        this.apiAuthorizationService = apiAuthorizationService;
    }

    @GetMapping
    public ResponseEntity<?> getSubProjects(@PathVariable long projectId, Authentication authentication) {
        Employee employee = employeeService.getEmployeeByUsername(authentication.getName());
        if (!apiAuthorizationService.canAccessProject(employee, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You do not have access to this project"));
        }

        List<SubProjectResponse> subProjects = projectService.showSubProjectsByProjectId(projectId)
                .stream()
                .map(subProject -> SubProjectResponse.from(subProject, projectId))
                .toList();
        return ResponseEntity.ok(subProjects);
    }

    @PostMapping
    public ResponseEntity<?> createSubProject(@PathVariable long projectId,
                                              @RequestBody SubProjectRequest request,
                                              Authentication authentication) {
        Employee employee = employeeService.getEmployeeByUsername(authentication.getName());
        if (employee.getRole() != EmployeeRole.PROJECT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only project managers can create subprojects"));
        }
        if (!apiAuthorizationService.canAccessProject(employee, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You do not have access to this project"));
        }
        if (request.subProjectName() == null || request.subProjectName().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Subproject name is required"));
        }

        Project project = projectService.getProjectById(projectId);
        String validationError = validateProjectPeriod(project, request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(validationError));
        }

        SubProject subProject = new SubProject();
        subProject.setSubProjectName(request.subProjectName());
        subProject.setSubProjectDescription(request.subProjectDescription());
        subProject.setSubProjectStartDate(request.subProjectStartDate());
        subProject.setSubProjectDeadline(request.subProjectDeadline());
        subProject.recalculateDuration();

        projectService.saveSubProject(subProject, projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(SubProjectResponse.from(subProject, projectId));
    }

    @PutMapping("/{subProjectId}")
    public ResponseEntity<?> updateSubProject(@PathVariable long projectId,
                                              @PathVariable long subProjectId,
                                              @RequestBody SubProjectRequest request,
                                              Authentication authentication) {
        Employee employee = employeeService.getEmployeeByUsername(authentication.getName());
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerAndSubProjectAccess(employee, projectId, subProjectId, "update");
        if (accessError != null) {
            return accessError;
        }
        if (request.subProjectName() == null || request.subProjectName().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Subproject name is required"));
        }

        Project project = projectService.getProjectById(projectId);
        String validationError = validateProjectPeriod(project, request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(validationError));
        }

        SubProject subProject = new SubProject();
        subProject.setSubProjectID(subProjectId);
        subProject.setSubProjectName(request.subProjectName());
        subProject.setSubProjectDescription(request.subProjectDescription());
        subProject.setSubProjectStartDate(request.subProjectStartDate());
        subProject.setSubProjectDeadline(request.subProjectDeadline());
        subProject.recalculateDuration();

        projectService.editSubProject(subProject);
        return ResponseEntity.ok(SubProjectResponse.from(projectService.getSubProjectBySubProjectID(subProjectId), projectId));
    }

    @DeleteMapping("/{subProjectId}")
    public ResponseEntity<?> deleteSubProject(@PathVariable long projectId,
                                              @PathVariable long subProjectId,
                                              Authentication authentication) {
        Employee employee = employeeService.getEmployeeByUsername(authentication.getName());
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerAndSubProjectAccess(employee, projectId, subProjectId, "delete");
        if (accessError != null) {
            return accessError;
        }

        projectService.deleteSubProject(subProjectId);
        return ResponseEntity.noContent().build();
    }

    private String validateProjectPeriod(Project project, SubProjectRequest request) {
        if (request.subProjectStartDate() != null
                && project.getProjectStartDate() != null
                && request.subProjectStartDate().isBefore(project.getProjectStartDate())) {
            return "Subproject start date must be within project period";
        }
        if (request.subProjectDeadline() != null
                && project.getProjectDeadline() != null
                && request.subProjectDeadline().isAfter(project.getProjectDeadline())) {
            return "Subproject deadline must be within project period";
        }
        if (request.subProjectStartDate() != null
                && request.subProjectDeadline() != null
                && request.subProjectDeadline().isBefore(request.subProjectStartDate())) {
            return "Subproject deadline cannot be before start date";
        }
        return null;
    }

    private ResponseEntity<ErrorResponse> validateProjectManagerAndSubProjectAccess(Employee employee,
                                                                                    long projectId,
                                                                                    long subProjectId,
                                                                                    String action) {
        if (employee.getRole() != EmployeeRole.PROJECT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only project managers can " + action + " subprojects"));
        }
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
}
