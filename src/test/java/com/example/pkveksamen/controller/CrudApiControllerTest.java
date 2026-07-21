package com.example.pkveksamen.controller;

import com.example.pkveksamen.dto.AuthResponse;
import com.example.pkveksamen.dto.ProjectRequest;
import com.example.pkveksamen.dto.RegisterEmployeeRequest;
import com.example.pkveksamen.dto.SubProjectRequest;
import com.example.pkveksamen.dto.SubTaskRequest;
import com.example.pkveksamen.dto.TaskRequest;
import com.example.pkveksamen.model.AlphaRole;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;
import com.example.pkveksamen.model.SubProject;
import com.example.pkveksamen.model.Task;
import com.example.pkveksamen.repository.ProjectRepository;
import com.example.pkveksamen.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrudApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskService taskService;

    private AuthResponse manager;
    private AuthResponse member;
    private long projectId;
    private long subProjectId;
    private long taskId;
    private long subTaskId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE sub_task");
        jdbcTemplate.execute("TRUNCATE TABLE task");
        jdbcTemplate.execute("TRUNCATE TABLE sub_project");
        jdbcTemplate.execute("TRUNCATE TABLE project_employee");
        jdbcTemplate.execute("TRUNCATE TABLE project");
        jdbcTemplate.execute("TRUNCATE TABLE employee_role");
        jdbcTemplate.execute("TRUNCATE TABLE employee");
        jdbcTemplate.execute("TRUNCATE TABLE role");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        jdbcTemplate.execute("ALTER TABLE employee ALTER COLUMN employee_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE role ALTER COLUMN role_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE project ALTER COLUMN project_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE sub_project ALTER COLUMN sub_project_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE task ALTER COLUMN task_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE sub_task ALTER COLUMN sub_task_id RESTART WITH 1");

        manager = register("pm", "pm@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        member = register("tm", "tm@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        projectId = createProject(manager.employee().employeeId());
        projectRepository.addEmployeeToProject(member.employee().employeeId(), projectId);
        subProjectId = createSubProject(projectId);
        taskId = createTask(member.employee().employeeId());
        subTaskId = createSubTask();
    }

    @Test
    void projectManager_canUpdateProject() throws Exception {
        ProjectRequest request = new ProjectRequest(
                "Updated project",
                "Updated project description",
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 30),
                "Updated customer"
        );

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.projectName").value("Updated project"))
                .andExpect(jsonPath("$.projectCustomer").value("Updated customer"));
    }

    @Test
    void teamMember_cannotUpdateProject() throws Exception {
        ProjectRequest request = new ProjectRequest(
                "Should fail",
                "Team members cannot update projects",
                LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 30),
                "Alpha"
        );

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectManager_canDeleteProject() throws Exception {
        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void projectManager_canUpdateSubProject() throws Exception {
        SubProjectRequest request = new SubProjectRequest(
                "Updated subproject",
                "Updated subproject description",
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 19)
        );

        mockMvc.perform(put(subProjectsUrl() + "/" + subProjectId)
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subProjectId").value(subProjectId))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.subProjectName").value("Updated subproject"));
    }

    @Test
    void projectManager_canDeleteSubProject() throws Exception {
        mockMvc.perform(delete(subProjectsUrl() + "/" + subProjectId)
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(subProjectsUrl())
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void projectManager_canUpdateTask() throws Exception {
        TaskRequest request = new TaskRequest(
                "Updated task",
                "Updated task description",
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 11),
                Status.IN_PROGRESS,
                Priority.LOW,
                "Updated task note",
                member.employee().employeeId()
        );

        mockMvc.perform(put(tasksUrl() + "/" + taskId)
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.subProjectId").value(subProjectId))
                .andExpect(jsonPath("$.taskName").value("Updated task"))
                .andExpect(jsonPath("$.taskStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.taskPriority").value("LOW"));
    }

    @Test
    void teamMember_cannotDeleteTask() throws Exception {
        mockMvc.perform(delete(tasksUrl() + "/" + taskId)
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectManager_canDeleteTask() throws Exception {
        mockMvc.perform(delete(tasksUrl() + "/" + taskId)
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(tasksUrl())
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void projectManager_canUpdateSubTask() throws Exception {
        SubTaskRequest request = new SubTaskRequest(
                "Updated subtask",
                "Updated subtask description",
                LocalDate.of(2026, 8, 8),
                LocalDate.of(2026, 8, 10),
                Status.COMPLETED,
                Priority.MEDIUM,
                "Updated subtask note"
        );

        mockMvc.perform(put(subTasksUrl() + "/" + subTaskId)
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subTaskId").value(subTaskId))
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.subTaskName").value("Updated subtask"))
                .andExpect(jsonPath("$.subTaskStatus").value("COMPLETED"));
    }

    @Test
    void projectManager_canDeleteSubTask() throws Exception {
        mockMvc.perform(delete(subTasksUrl() + "/" + subTaskId)
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(subTasksUrl())
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private AuthResponse register(String username, String email, EmployeeRole role, AlphaRole alphaRole) throws Exception {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest(username, "password123", email, role, alphaRole);

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthResponse.class);
    }

    private long createProject(int ownerId) {
        projectRepository.createProject(
                "Parent project",
                "Project period",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "Alpha",
                ownerId
        );
        return projectRepository.showProjectsByEmployeeId(ownerId).get(0).getProjectID();
    }

    private long createSubProject(long parentProjectId) {
        SubProject subProject = new SubProject();
        subProject.setSubProjectName("Backend");
        subProject.setSubProjectDescription("API migration");
        subProject.setSubProjectStartDate(LocalDate.of(2026, 8, 5));
        subProject.setSubProjectDeadline(LocalDate.of(2026, 8, 20));
        projectRepository.saveSubProject(subProject, parentProjectId);
        return subProject.getSubProjectID();
    }

    private long createTask(int assignedEmployeeId) {
        Task task = taskService.createTask(
                assignedEmployeeId,
                subProjectId,
                "Parent task",
                "Task period",
                Status.NOT_STARTED,
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 12),
                0,
                Priority.HIGH,
                ""
        );
        return task.getTaskID();
    }

    private long createSubTask() {
        return taskService.createSubTask(
                taskId,
                "Parent subtask",
                "Subtask period",
                Status.NOT_STARTED.name(),
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 10),
                0,
                Priority.HIGH.name(),
                ""
        ).getSubTaskId();
    }

    private String subProjectsUrl() {
        return "/api/projects/" + projectId + "/subprojects";
    }

    private String tasksUrl() {
        return subProjectsUrl() + "/" + subProjectId + "/tasks";
    }

    private String subTasksUrl() {
        return tasksUrl() + "/" + taskId + "/subtasks";
    }
}
