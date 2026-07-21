package com.example.pkveksamen.service;

import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.Task;
import org.springframework.stereotype.Service;

@Service
public class ApiAuthorizationService {

    private final ProjectService projectService;
    private final TaskService taskService;

    public ApiAuthorizationService(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    public boolean canAccessProject(Employee employee, long projectId) {
        return employee != null && projectService.canEmployeeAccessProject(employee.getEmployeeId(), projectId);
    }

    public boolean canCreateInsideProject(Employee employee, long projectId) {
        return employee != null
                && employee.getRole() == EmployeeRole.PROJECT_MANAGER
                && canAccessProject(employee, projectId);
    }

    public boolean isProjectMember(long projectId, int employeeId) {
        return projectService.canEmployeeAccessProject(employeeId, projectId);
    }

    public boolean subProjectBelongsToProject(long subProjectId, long projectId) {
        return projectService.subProjectBelongsToProject(subProjectId, projectId);
    }

    public boolean taskBelongsToSubProject(long taskId, long subProjectId) {
        return taskService.taskBelongsToSubProject(taskId, subProjectId);
    }

    public boolean subTaskBelongsToTask(long subTaskId, long taskId) {
        return taskService.subTaskBelongsToTask(subTaskId, taskId);
    }

    public boolean canReadTask(Employee employee, Task task) {
        if (employee == null) {
            return false;
        }
        if (employee.getRole() == EmployeeRole.PROJECT_MANAGER) {
            return true;
        }
        return task.getAssignedEmployee() != null
                && task.getAssignedEmployee().getEmployeeId() == employee.getEmployeeId();
    }
}
