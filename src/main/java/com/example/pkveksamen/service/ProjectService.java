package com.example.pkveksamen.service;

import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.Project;
import com.example.pkveksamen.model.SubProject;
import com.example.pkveksamen.model.Task;
import com.example.pkveksamen.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void createProject(String projectTitle, String projectDescription, LocalDate projectStartDate,
                              LocalDate projectEndDate, String projectCustomer, Integer employeeId) {
        projectRepository.createProject(projectTitle, projectDescription, projectStartDate, projectEndDate, projectCustomer, employeeId);
    }

    public List<Project> showProjectsByEmployeeId(int employeeId) {
        return projectRepository.showProjectsByEmployeeId(employeeId);
    }

    public Employee getProjectOwner(long projectId) {
        return projectRepository.getProjectOwner(projectId);
    }

    public List<SubProject> showSubProjectsByProjectId(long projectID) {
        return projectRepository.showSubProjectsByProjectId(projectID);
    }

    public boolean canEmployeeAccessProject(int employeeId, long projectId) {
        return projectRepository.canEmployeeAccessProject(employeeId, projectId);
    }

    public boolean subProjectBelongsToProject(long subProjectId, long projectId) {
        return projectRepository.subProjectBelongsToProject(subProjectId, projectId);
    }

    public void saveProject(Project projectModel, int employeeId) {
        projectRepository.saveProject(projectModel, employeeId);
    }

    public void saveSubProject(SubProject subProject, long projectID) {
        projectRepository.saveSubProject(subProject, projectID);
    }

    public void deleteProject(long projectID) {
        projectRepository.deleteProject(projectID);
    }

    public void editProject(Project project) {
        projectRepository.editProject(project);
    }

    public Project getProjectById(long projectId) {
        return projectRepository.getProjectById(projectId);
    }

    public SubProject getSubProjectBySubProjectID(long subProjectID) {
       return projectRepository.getSubProjectBySubProjectID(subProjectID);
    }

    public void editSubProject(SubProject subProject) {
        projectRepository.editSubProject(subProject);
    }
    public void deleteSubProject(long subProjectId) {
        projectRepository.deleteSubProject(subProjectId);
    }

    public List<Employee> getProjectMembers(long projectId) {
        return projectRepository.getProjectMembers(projectId);
    }

    public List<Employee> getAvailableEmployeesToAdd(long projectId) {
        return projectRepository.getAvailableEmployeesToAdd(projectId);
    }

    public void addEmployeeToProject(int employeeId, long projectId) {
        projectRepository.addEmployeeToProject(employeeId, projectId);
    }

    public void removeEmployeeFromProject(int employeeId, long projectId) {
        projectRepository.removeEmployeeFromProject(employeeId, projectId);
    }
}
