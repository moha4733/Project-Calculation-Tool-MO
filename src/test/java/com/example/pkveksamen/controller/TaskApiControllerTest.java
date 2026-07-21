package com.example.pkveksamen.controller;

import com.example.pkveksamen.dto.AuthResponse;
import com.example.pkveksamen.dto.RegisterEmployeeRequest;
import com.example.pkveksamen.dto.TaskRequest;
import com.example.pkveksamen.model.AlphaRole;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;
import com.example.pkveksamen.model.SubProject;
import com.example.pkveksamen.repository.ProjectRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectRepository projectRepository;

    private AuthResponse manager;
    private AuthResponse member;
    private long projectId;
    private long subProjectId;

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

        manager = register("pm", "pm@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        member = register("tm", "tm@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        projectId = createProject(manager.employee().employeeId());
        projectRepository.addEmployeeToProject(member.employee().employeeId(), projectId);
        subProjectId = createSubProject(projectId);
    }

    @Test
    void tasks_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectManager_canCreateTask() throws Exception {
        TaskRequest request = new TaskRequest(
                "Implement task API",
                "Expose task list and create endpoints",
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 12),
                Status.NOT_STARTED,
                Priority.HIGH,
                "Start with API tests",
                member.employee().employeeId()
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks")
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(1))
                .andExpect(jsonPath("$.subProjectId").value(subProjectId))
                .andExpect(jsonPath("$.taskName").value("Implement task API"))
                .andExpect(jsonPath("$.taskStatus").value("NOT_STARTED"))
                .andExpect(jsonPath("$.taskPriority").value("HIGH"))
                .andExpect(jsonPath("$.assignedEmployee.employeeId").value(member.employee().employeeId()))
                .andExpect(jsonPath("$.assignedEmployee.password").doesNotExist());
    }

    @Test
    void teamMember_cannotCreateTask() throws Exception {
        TaskRequest request = new TaskRequest(
                "Should fail",
                "Team member cannot create tasks",
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 12),
                Status.NOT_STARTED,
                Priority.MEDIUM,
                "",
                member.employee().employeeId()
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks")
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listTasks_returnsTasksForSubProject() throws Exception {
        createTask("Backend task", Priority.HIGH);
        createTask("Frontend task", Priority.MEDIUM);

        mockMvc.perform(get("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks")
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].taskName").value("Backend task"))
                .andExpect(jsonPath("$[1].taskName").value("Frontend task"));
    }

    @Test
    void teamMember_onlySeesAssignedTasksForSubProject() throws Exception {
        AuthResponse otherMember = register("other", "other@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Tester);
        projectRepository.addEmployeeToProject(otherMember.employee().employeeId(), projectId);

        createTask("Mine", Priority.HIGH, member.employee().employeeId());
        createTask("Not mine", Priority.LOW, otherMember.employee().employeeId());

        mockMvc.perform(get("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks")
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].taskName").value("Mine"));
    }

    @Test
    void createTask_rejectsDatesOutsideSubProjectPeriod() throws Exception {
        TaskRequest request = new TaskRequest(
                "Too late",
                "Deadline exceeds subproject",
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 25),
                Status.NOT_STARTED,
                Priority.HIGH,
                "",
                member.employee().employeeId()
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks")
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Task deadline must be within subproject period"));
    }

    @Test
    void createTask_rejectsStartBeforeSubProjectPeriod() throws Exception {
        TaskRequest request = new TaskRequest(
                "Too early",
                "Start is before subproject",
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 12),
                Status.NOT_STARTED,
                Priority.HIGH,
                "",
                member.employee().employeeId()
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks")
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Task start date must be within subproject period"));
    }

    @Test
    void assignedTeamMember_canUpdateOwnTaskStatus() throws Exception {
        createTask("Mine", Priority.HIGH, member.employee().employeeId());

        mockMvc.perform(patch(tasksUrl() + "/1/status")
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskStatus").value("IN_PROGRESS"));
    }

    @Test
    void assignedTeamMember_canUpdateOwnTaskNote() throws Exception {
        createTask("Mine", Priority.HIGH, member.employee().employeeId());

        mockMvc.perform(patch(tasksUrl() + "/1/note")
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Waiting for review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskNote").value("Waiting for review"));
    }

    @Test
    void teamMember_cannotUpdateOtherMembersTaskStatus() throws Exception {
        AuthResponse otherMember = register("other", "other@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Tester);
        projectRepository.addEmployeeToProject(otherMember.employee().employeeId(), projectId);
        createTask("Not mine", Priority.HIGH, otherMember.employee().employeeId());

        mockMvc.perform(patch(tasksUrl() + "/1/status")
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
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

    private void createTask(String name, Priority priority) throws Exception {
        createTask(name, priority, member.employee().employeeId());
    }

    private void createTask(String name, Priority priority, int assignedEmployeeId) throws Exception {
        TaskRequest request = new TaskRequest(
                name,
                "Task description",
                LocalDate.of(2026, 8, 6),
                LocalDate.of(2026, 8, 12),
                Status.NOT_STARTED,
                priority,
                "",
                assignedEmployeeId
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks")
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String tasksUrl() {
        return "/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks";
    }
}
