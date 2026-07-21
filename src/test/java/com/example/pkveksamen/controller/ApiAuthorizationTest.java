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
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiAuthorizationTest {

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

    private AuthResponse owner;
    private AuthResponse member;
    private AuthResponse outsiderManager;
    private long projectId;
    private long subProjectId;
    private long taskId;

    @BeforeEach
    void cleanDatabase() throws Exception {
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

        owner = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        member = register("member", "member@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        outsiderManager = register("outsider", "outsider@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);

        projectId = createProject(owner.employee().employeeId());
        projectRepository.addEmployeeToProject(member.employee().employeeId(), projectId);
        subProjectId = createSubProject(projectId);
        taskId = createTask(member.employee().employeeId());
    }

    @Test
    void outsiderCannotReadProjectById() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + outsiderManager.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this project"));
    }

    @Test
    void outsiderCannotReadSubProjectsByKnownProjectId() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/subprojects")
                        .header("Authorization", "Bearer " + outsiderManager.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this project"));
    }

    @Test
    void outsiderManagerCannotCreateSubProjectInAnotherManagersProject() throws Exception {
        SubProjectRequest request = new SubProjectRequest(
                "Unauthorized subproject",
                "Should be blocked",
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 10)
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects")
                        .header("Authorization", "Bearer " + outsiderManager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this project"));
    }

    @Test
    void outsiderManagerCannotCreateTaskInAnotherManagersProject() throws Exception {
        TaskRequest request = new TaskRequest(
                "Unauthorized task",
                "Should be blocked",
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 10),
                Status.NOT_STARTED,
                Priority.HIGH,
                "",
                member.employee().employeeId()
        );

        mockMvc.perform(post(tasksUrl())
                        .header("Authorization", "Bearer " + outsiderManager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this project"));
    }

    @Test
    void projectManagerCannotAssignTaskToEmployeeOutsideProject() throws Exception {
        TaskRequest request = new TaskRequest(
                "Wrong assignee",
                "Assignee is not a project member",
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 10),
                Status.NOT_STARTED,
                Priority.HIGH,
                "",
                outsiderManager.employee().employeeId()
        );

        mockMvc.perform(post(tasksUrl())
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Assigned employee must be a project member"));
    }

    @Test
    void outsiderCannotReadSubTasksByKnownTaskId() throws Exception {
        mockMvc.perform(get(subTasksUrl())
                        .header("Authorization", "Bearer " + outsiderManager.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have access to this project"));
    }

    @Test
    void taskPathMustMatchParentProjectAndSubProject() throws Exception {
        long otherProjectId = createProject(outsiderManager.employee().employeeId());
        long otherSubProjectId = createSubProject(otherProjectId);

        mockMvc.perform(get("/api/projects/" + otherProjectId + "/subprojects/" + otherSubProjectId + "/tasks/" + taskId + "/subtasks")
                        .header("Authorization", "Bearer " + outsiderManager.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Task does not belong to this subproject"));
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

    private String tasksUrl() {
        return "/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks";
    }

    private String subTasksUrl() {
        return tasksUrl() + "/" + taskId + "/subtasks";
    }
}
