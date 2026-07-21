package com.example.pkveksamen.controller;

import com.example.pkveksamen.dto.AuthResponse;
import com.example.pkveksamen.dto.RegisterEmployeeRequest;
import com.example.pkveksamen.dto.SubTaskRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubTaskApiControllerTest {

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
        taskId = createTask(member.employee().employeeId());
    }

    @Test
    void subTasks_requireAuthentication() throws Exception {
        mockMvc.perform(get(subTasksUrl()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectManager_canCreateSubTask() throws Exception {
        SubTaskRequest request = new SubTaskRequest(
                "Write controller test",
                "Cover subtask API contract",
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 10),
                Status.NOT_STARTED,
                Priority.HIGH,
                "TDD first"
        );

        mockMvc.perform(post(subTasksUrl())
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subTaskId").value(1))
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.subTaskName").value("Write controller test"))
                .andExpect(jsonPath("$.subTaskStatus").value("NOT_STARTED"))
                .andExpect(jsonPath("$.subTaskPriority").value("HIGH"));
    }

    @Test
    void teamMember_cannotCreateSubTask() throws Exception {
        SubTaskRequest request = validRequest("Should fail");

        mockMvc.perform(post(subTasksUrl())
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listSubTasks_returnsSubTasksForTask() throws Exception {
        createSubTask("Repository migration", Priority.HIGH);
        createSubTask("API response mapping", Priority.MEDIUM);

        mockMvc.perform(get(subTasksUrl())
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].subTaskName").value("Repository migration"))
                .andExpect(jsonPath("$[1].subTaskName").value("API response mapping"));
    }

    @Test
    void assignedTeamMember_canListSubTasksForOwnTask() throws Exception {
        createSubTask("Mine", Priority.HIGH);

        mockMvc.perform(get(subTasksUrl())
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].subTaskName").value("Mine"));
    }

    @Test
    void unassignedTeamMember_cannotListSubTasksForOtherTask() throws Exception {
        AuthResponse otherMember = register("other", "other@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Tester);
        projectRepository.addEmployeeToProject(otherMember.employee().employeeId(), projectId);

        mockMvc.perform(get(subTasksUrl())
                        .header("Authorization", "Bearer " + otherMember.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSubTask_rejectsDatesOutsideTaskPeriod() throws Exception {
        SubTaskRequest request = new SubTaskRequest(
                "Too late",
                "Deadline exceeds parent task",
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 18),
                Status.NOT_STARTED,
                Priority.HIGH,
                ""
        );

        mockMvc.perform(post(subTasksUrl())
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Subtask deadline must be within task period"));
    }

    @Test
    void assignedTeamMember_canUpdateSubTaskStatusForOwnTask() throws Exception {
        createSubTask("Mine", Priority.HIGH);

        mockMvc.perform(patch(subTasksUrl() + "/1/status")
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subTaskStatus").value("COMPLETED"));
    }

    @Test
    void assignedTeamMember_canUpdateSubTaskNoteForOwnTask() throws Exception {
        createSubTask("Mine", Priority.HIGH);

        mockMvc.perform(patch(subTasksUrl() + "/1/note")
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Blocked by dependency\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subTaskNote").value("Blocked by dependency"));
    }

    @Test
    void unassignedTeamMember_cannotUpdateSubTaskStatusForOtherTask() throws Exception {
        AuthResponse otherMember = register("other", "other@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Tester);
        projectRepository.addEmployeeToProject(otherMember.employee().employeeId(), projectId);
        createSubTask("Not mine", Priority.HIGH);

        mockMvc.perform(patch(subTasksUrl() + "/1/status")
                        .header("Authorization", "Bearer " + otherMember.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
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

    private void createSubTask(String name, Priority priority) throws Exception {
        mockMvc.perform(post(subTasksUrl())
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(name, priority))))
                .andExpect(status().isCreated());
    }

    private SubTaskRequest validRequest(String name) {
        return validRequest(name, Priority.MEDIUM);
    }

    private SubTaskRequest validRequest(String name, Priority priority) {
        return new SubTaskRequest(
                name,
                "Subtask description",
                LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 10),
                Status.NOT_STARTED,
                priority,
                ""
        );
    }

    private String subTasksUrl() {
        return "/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks/" + taskId + "/subtasks";
    }
}
