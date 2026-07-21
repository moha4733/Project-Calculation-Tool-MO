package com.example.pkveksamen.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.example.pkveksamen.entity.EmployeeEntity;
import com.example.pkveksamen.entity.ProjectEntity;
import com.example.pkveksamen.entity.SubProjectEntity;
import com.example.pkveksamen.model.Employee;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.SubProject;
import org.springframework.stereotype.Repository;

import com.example.pkveksamen.model.Project;

@Repository
public class ProjectRepository {

    private final ProjectJpaRepository projectJpaRepository;
    private final SubProjectJpaRepository subProjectJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public ProjectRepository(ProjectJpaRepository projectJpaRepository,
                             SubProjectJpaRepository subProjectJpaRepository,
                             EmployeeJpaRepository employeeJpaRepository) {
        this.projectJpaRepository = projectJpaRepository;
        this.subProjectJpaRepository = subProjectJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    public void createProject(String projectTitle, String projectDescription, LocalDate projectStartDate,
                              LocalDate projectDeadline, String projectCustomer, Integer employeeId) {
        Project project = new Project();
        project.setProjectName(projectTitle);
        project.setProjectDescription(projectDescription);
        project.setProjectStartDate(projectStartDate);
        project.setProjectDeadline(projectDeadline);
        project.setProjectCustomer(projectCustomer);
        saveProject(project, employeeId);
    }

    public List<Project> showProjectsByEmployeeId(int employeeId) {
        return projectJpaRepository.findVisibleProjectsForEmployee(employeeId)
                .stream()
                .map(this::toProjectModel)
                .toList();
    }

    public List<SubProject> showSubProjectsByProjectId(long projectID) {
        return subProjectJpaRepository.findByProjectIdOrderById(projectID)
                .stream()
                .map(this::toSubProjectModel)
                .toList();
    }

    public boolean canEmployeeAccessProject(int employeeId, long projectId) {
        return projectJpaRepository.existsVisibleProjectForEmployee(projectId, employeeId);
    }

    public boolean subProjectBelongsToProject(long subProjectId, long projectId) {
        return subProjectJpaRepository.existsByIdAndProjectId(subProjectId, projectId);
    }


    public void saveProject(Project project, int employeeId) {
        EmployeeEntity owner = employeeJpaRepository.findById((long) employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + employeeId));

        ProjectEntity entity = new ProjectEntity();
        entity.setOwner(owner);
        entity.setTitle(project.getProjectName());
        entity.setDescription(project.getProjectDescription());
        entity.setStartDate(project.getProjectStartDate());
        entity.setDeadline(project.getProjectDeadline());
        entity.setCustomer(project.getProjectCustomer());

        ProjectEntity saved = projectJpaRepository.save(entity);
        project.setProjectID(saved.getId());
    }

    public void saveSubProject(SubProject subProject, long projectID) {
        ProjectEntity project = projectJpaRepository.findById(projectID)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectID));
        subProject.recalculateDuration();

        SubProjectEntity entity = new SubProjectEntity();
        entity.setProject(project);
        entity.setTitle(subProject.getSubProjectName());
        entity.setDescription(subProject.getSubProjectDescription());
        entity.setStartDate(subProject.getSubProjectStartDate());
        entity.setDeadline(subProject.getSubProjectDeadline());
        entity.setDuration(subProject.getSubProjectDuration());

        SubProjectEntity saved = subProjectJpaRepository.save(entity);
        subProject.setSubProjectID(saved.getId());
    }

    /*
    public void deleteProject(long projectID) {
        jdbcTemplate.update("DELETE FROM project WHERE project_id = ?", projectID);
    }
    */

    public void deleteProject(long projectID) {
        projectJpaRepository.deleteById(projectID);
    }

    public void editProject(Project project) {
        ProjectEntity entity = projectJpaRepository.findById(project.getProjectID())
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + project.getProjectID()));

        entity.setTitle(project.getProjectName());
        entity.setDescription(project.getProjectDescription());
        entity.setStartDate(project.getProjectStartDate());
        entity.setDeadline(project.getProjectDeadline());
        entity.setCustomer(project.getProjectCustomer());
        projectJpaRepository.save(entity);
    }
    public void editSubProject(SubProject subProject) {
        SubProjectEntity entity = subProjectJpaRepository.findById(subProject.getSubProjectID())
                .orElseThrow(() -> new NoSuchElementException("Subproject not found: " + subProject.getSubProjectID()));
        subProject.recalculateDuration();

        entity.setTitle(subProject.getSubProjectName());
        entity.setDescription(subProject.getSubProjectDescription());
        entity.setStartDate(subProject.getSubProjectStartDate());
        entity.setDeadline(subProject.getSubProjectDeadline());
        entity.setDuration(subProject.getSubProjectDuration());

        subProjectJpaRepository.save(entity);
    }

    public Project getProjectById(long projectId) {
        return projectJpaRepository.findById(projectId)
                .map(this::toProjectModel)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));
    }


    public SubProject getSubProjectBySubProjectID(long subProjectID) {
        return subProjectJpaRepository.findById(subProjectID)
                .map(this::toSubProjectModel)
                .orElseThrow(() -> new NoSuchElementException("Subproject not found: " + subProjectID));
    }

    public void deleteSubProject(long subProjectId) {
        subProjectJpaRepository.deleteById(subProjectId);
    }

    public List<Employee> getProjectMembers(long projectId) {
        ProjectEntity project = projectJpaRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));

        Map<Long, Employee> employees = new LinkedHashMap<>();
        employees.put(project.getOwner().getId(), toEmployeeModel(project.getOwner()));
        project.getMembers().stream()
                .sorted(Comparator.comparing(EmployeeEntity::getId))
                .forEach(member -> employees.put(member.getId(), toEmployeeModel(member)));
        return new ArrayList<>(employees.values());
    }

    public List<Employee> getAvailableEmployeesToAdd(long projectId) {
        ProjectEntity project = projectJpaRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));

        List<Long> assignedIds = new ArrayList<>();
        assignedIds.add(project.getOwner().getId());
        project.getMembers().forEach(member -> assignedIds.add(member.getId()));

        return employeeJpaRepository.findAll()
                .stream()
                .filter(employee -> !assignedIds.contains(employee.getId()))
                .sorted(Comparator.comparing(EmployeeEntity::getId))
                .map(this::toEmployeeModel)
                .toList();
    }

    public void addEmployeeToProject(int employeeId, long projectId) {
        ProjectEntity project = projectJpaRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));
        EmployeeEntity employee = employeeJpaRepository.findById((long) employeeId)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + employeeId));

        if (!project.getOwner().getId().equals(employee.getId())) {
            project.getMembers().add(employee);
            projectJpaRepository.save(project);
        }
    }


    public void removeEmployeeFromProject(int employeeId, long projectId) {
        ProjectEntity project = projectJpaRepository.findById(projectId)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));
        project.getMembers().removeIf(member -> member.getId() == employeeId);
        projectJpaRepository.save(project);
    }

    public Employee getProjectOwner(long projectId) {
        return projectJpaRepository.findById(projectId)
                .map(ProjectEntity::getOwner)
                .map(this::toEmployeeModel)
                .orElseThrow(() -> new NoSuchElementException("Project not found: " + projectId));
    }

    private Project toProjectModel(ProjectEntity entity) {
        Project project = new Project();
        project.setProjectID(entity.getId());
        project.setProjectName(entity.getTitle());
        project.setProjectDescription(entity.getDescription());
        project.setProjectStartDate(entity.getStartDate());
        project.setProjectDeadline(entity.getDeadline());
        project.setProjectCustomer(entity.getCustomer());
        project.recalculateDuration();
        return project;
    }

    private Employee toEmployeeModel(EmployeeEntity entity) {
        Employee employee = new Employee();
        employee.setEmployeeId(entity.getId().intValue());
        employee.setUsername(entity.getUsername());
        employee.setEmail(entity.getEmail());
        employee.setRole(EmployeeRole.fromDisplayName(entity.getRole()));
        return employee;
    }

    private SubProject toSubProjectModel(SubProjectEntity entity) {
        SubProject subProject = new SubProject();
        subProject.setSubProjectID(entity.getId());
        subProject.setSubProjectName(entity.getTitle());
        subProject.setSubProjectDescription(entity.getDescription());
        subProject.setSubProjectStartDate(entity.getStartDate());
        subProject.setSubProjectDeadline(entity.getDeadline());
        subProject.setSubProjectDuration(entity.getDuration() != null ? entity.getDuration() : 0);
        subProject.recalculateDuration();
        return subProject;
    }
}
