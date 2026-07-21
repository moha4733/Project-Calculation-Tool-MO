package com.example.pkveksamen.controller.api;

import com.example.pkveksamen.dto.ErrorResponse;
import com.example.pkveksamen.dto.EmployeeResponse;
import com.example.pkveksamen.dto.ProjectRequest;
import com.example.pkveksamen.dto.ProjectResponse;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.Project;
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
@RequestMapping("/api/projects")
public class ProjectApiController {

    private final ProjectService projectService;
    private final EmployeeService employeeService;
    private final ApiAuthorizationService apiAuthorizationService;

    public ProjectApiController(ProjectService projectService,
                                EmployeeService employeeService,
                                ApiAuthorizationService apiAuthorizationService) {
        this.projectService = projectService;
        this.employeeService = employeeService;
        this.apiAuthorizationService = apiAuthorizationService;
    }

    @GetMapping
    public List<ProjectResponse> getMyProjects(Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        return projectService.showProjectsByEmployeeId(employee.getEmployeeId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProject(@PathVariable long projectId, Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        if (!apiAuthorizationService.canAccessProject(employee, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You do not have access to this project"));
        }
        return ResponseEntity.ok(toResponse(projectService.getProjectById(projectId)));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<?> getProjectMembers(@PathVariable long projectId, Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        if (!apiAuthorizationService.canAccessProject(employee, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You do not have access to this project"));
        }

        List<EmployeeResponse> members = projectService.getProjectMembers(projectId)
                .stream()
                .map(EmployeeResponse::from)
                .toList();
        return ResponseEntity.ok(members);
    }

    @GetMapping("/{projectId}/available-employees")
    public ResponseEntity<?> getAvailableEmployees(@PathVariable long projectId, Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerAccess(
                employee,
                projectId,
                "Only project managers can manage project members"
        );
        if (accessError != null) {
            return accessError;
        }

        List<EmployeeResponse> availableEmployees = projectService.getAvailableEmployeesToAdd(projectId)
                .stream()
                .map(EmployeeResponse::from)
                .toList();
        return ResponseEntity.ok(availableEmployees);
    }

    @PostMapping("/{projectId}/members/{employeeId}")
    public ResponseEntity<?> addProjectMember(@PathVariable long projectId,
                                              @PathVariable int employeeId,
                                              Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerAccess(
                employee,
                projectId,
                "Only project managers can manage project members"
        );
        if (accessError != null) {
            return accessError;
        }
        if (apiAuthorizationService.isProjectMember(projectId, employeeId)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Employee is already a project member"));
        }

        projectService.addEmployeeToProject(employeeId, projectId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EmployeeResponse.from(employeeService.getEmployeeById(employeeId)));
    }

    @DeleteMapping("/{projectId}/members/{employeeId}")
    public ResponseEntity<?> removeProjectMember(@PathVariable long projectId,
                                                 @PathVariable int employeeId,
                                                 Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerAccess(
                employee,
                projectId,
                "Only project managers can manage project members"
        );
        if (accessError != null) {
            return accessError;
        }
        if (projectService.getProjectOwner(projectId).getEmployeeId() == employeeId) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Project owner cannot be removed"));
        }

        projectService.removeEmployeeFromProject(employeeId, projectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody ProjectRequest request, Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        if (employee.getRole() != EmployeeRole.PROJECT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Only project managers can create projects"));
        }
        if (request.projectName() == null || request.projectName().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Project name is required"));
        }
        if (request.projectStartDate() != null
                && request.projectDeadline() != null
                && request.projectDeadline().isBefore(request.projectStartDate())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Project deadline cannot be before start date"));
        }

        Project project = new Project();
        project.setProjectName(request.projectName());
        project.setProjectDescription(request.projectDescription());
        project.setProjectStartDate(request.projectStartDate());
        project.setProjectDeadline(request.projectDeadline());
        project.setProjectCustomer(request.projectCustomer());
        project.recalculateDuration();

        projectService.saveProject(project, employee.getEmployeeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(project));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<?> updateProject(@PathVariable long projectId,
                                           @RequestBody ProjectRequest request,
                                           Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerAccess(
                employee,
                projectId,
                "Only project managers can update projects"
        );
        if (accessError != null) {
            return accessError;
        }
        if (request.projectName() == null || request.projectName().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Project name is required"));
        }
        if (request.projectStartDate() != null
                && request.projectDeadline() != null
                && request.projectDeadline().isBefore(request.projectStartDate())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Project deadline cannot be before start date"));
        }

        Project project = new Project();
        project.setProjectID(projectId);
        project.setProjectName(request.projectName());
        project.setProjectDescription(request.projectDescription());
        project.setProjectStartDate(request.projectStartDate());
        project.setProjectDeadline(request.projectDeadline());
        project.setProjectCustomer(request.projectCustomer());
        project.recalculateDuration();

        projectService.editProject(project);
        return ResponseEntity.ok(toResponse(projectService.getProjectById(projectId)));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable long projectId, Authentication authentication) {
        Employee employee = currentEmployee(authentication);
        ResponseEntity<ErrorResponse> accessError = validateProjectManagerAccess(
                employee,
                projectId,
                "Only project managers can delete projects"
        );
        if (accessError != null) {
            return accessError;
        }

        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ErrorResponse> validateProjectManagerAccess(Employee employee, long projectId, String roleErrorMessage) {
        if (employee.getRole() != EmployeeRole.PROJECT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(roleErrorMessage));
        }
        if (!apiAuthorizationService.canAccessProject(employee, projectId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("You do not have access to this project"));
        }
        return null;
    }

    private ProjectResponse toResponse(Project project) {
        Employee owner = projectService.getProjectOwner(project.getProjectID());
        return ProjectResponse.from(project, owner);
    }

    private Employee currentEmployee(Authentication authentication) {
        return employeeService.getEmployeeByUsername(authentication.getName());
    }
}
