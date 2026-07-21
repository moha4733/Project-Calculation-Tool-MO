package com.example.pkveksamen.end2end;

import com.example.pkveksamen.dto.AuthResponse;
import com.example.pkveksamen.dto.ProjectRequest;
import com.example.pkveksamen.dto.RegisterEmployeeRequest;
import com.example.pkveksamen.dto.SubProjectRequest;
import com.example.pkveksamen.dto.TaskNoteRequest;
import com.example.pkveksamen.dto.TaskRequest;
import com.example.pkveksamen.dto.TaskStatusRequest;
import com.example.pkveksamen.model.AlphaRole;
import com.example.pkveksamen.model.EmployeeRole;
import com.example.pkveksamen.model.Priority;
import com.example.pkveksamen.model.Status;
import com.example.pkveksamen.repository.ProjectRepository;
import com.example.pkveksamen.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End test:
 * - Starter hele Spring Boot app'en (webEnvironment=RANDOM_PORT)
 * - Kalder rigtige endpoints via HTTP
 * - Bruger H2-profilen (samme schema-h2.sql), så testen er isoleret og reproducerbar
 *
 * Forretningsregler der testes:
 * - Kun PROJECT_MANAGER må oprette projekt / subproject / task
 * - Task.employee_id = "tildelt medarbejder"
 * - TEAM_MEMBER må opdatere status/note på tildelte tasks
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("h2")
class ProjectFlowE2ETest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    /**
     * Constructor injection i test (bedste praksis)
     */
    @Autowired
    public ProjectFlowE2ETest(TestRestTemplate restTemplate,
                              JdbcTemplate jdbcTemplate,
                              ProjectRepository projectRepository,
                              TaskRepository taskRepository) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @BeforeEach
    void cleanDb() {
        // Rydder alle tabeller så testen starter fra et kendt nulpunkt
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
    }

    @Test
    void projectFlow_E2E_PM_creates_everything_teamMember_works_on_task() {

        AuthResponse manager = register("allan", "allan@mail.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse member = register("mohamed", "mohamed@mail.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.UXDesigner);
        int pmId = manager.employee().employeeId();
        int tmId = member.employee().employeeId();

        ProjectRequest projectRequest = new ProjectRequest(
                "KEA Exam Project",
                "E2E test project",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 10),
                "KEA"
        );

        ResponseEntity<String> createProjectResp = restTemplate.exchange(
                baseUrl("/api/projects"),
                HttpMethod.POST,
                new HttpEntity<>(projectRequest, jsonHeaders(manager.token())),
                String.class
        );

        assertTrue(createProjectResp.getStatusCode().is2xxSuccessful(), "Expected created project");

        List<com.example.pkveksamen.model.Project> pmProjects =
                projectRepository.showProjectsByEmployeeId(pmId);

        assertEquals(1, pmProjects.size());
        long projectId = pmProjects.get(0).getProjectID();

        ResponseEntity<String> addMemberResp = restTemplate.exchange(
                baseUrl("/api/projects/" + projectId + "/members/" + tmId),
                HttpMethod.POST,
                new HttpEntity<>(jsonHeaders(manager.token())),
                String.class
        );
        assertTrue(addMemberResp.getStatusCode().is2xxSuccessful(), "Expected member added");

        SubProjectRequest subProjectRequest = new SubProjectRequest(
                "Backend",
                "REST API",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 5)
        );

        ResponseEntity<String> createSubProjectResp = restTemplate.exchange(
                baseUrl("/api/projects/" + projectId + "/subprojects"),
                HttpMethod.POST,
                new HttpEntity<>(subProjectRequest, jsonHeaders(manager.token())),
                String.class
        );

        assertTrue(createSubProjectResp.getStatusCode().is2xxSuccessful(), "Expected created subproject");

        List<com.example.pkveksamen.model.SubProject> subProjects = projectRepository.showSubProjectsByProjectId(projectId);
        assertEquals(1, subProjects.size());
        long subProjectId = subProjects.get(0).getSubProjectID();

        TaskRequest taskRequest = new TaskRequest(
                "Implement Login API",
                "JWT login endpoint",
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 4),
                Status.NOT_STARTED,
                Priority.HIGH,
                "",
                tmId
        );

        ResponseEntity<String> createTaskResp = restTemplate.exchange(
                baseUrl("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks"),
                HttpMethod.POST,
                new HttpEntity<>(taskRequest, jsonHeaders(manager.token())),
                String.class
        );

        assertTrue(createTaskResp.getStatusCode().is2xxSuccessful(), "Expected created task");

        List<com.example.pkveksamen.model.Task> tasksForTM =
                taskRepository.showTaskByEmployeeId(tmId);

        assertEquals(1, tasksForTM.size());
        long taskId = tasksForTM.get(0).getTaskID();

        ResponseEntity<String> updateStatusResp = restTemplate.exchange(
                baseUrl("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks/" + taskId + "/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(new TaskStatusRequest(Status.IN_PROGRESS), jsonHeaders(member.token())),
                String.class
        );

        assertTrue(updateStatusResp.getStatusCode().is2xxSuccessful(), "Expected updated status");

        com.example.pkveksamen.model.Task updatedTask = taskRepository.getTaskById(taskId);
        assertEquals("IN_PROGRESS", updatedTask.getTaskStatus().name());

        ResponseEntity<String> saveNoteResp = restTemplate.exchange(
                baseUrl("/api/projects/" + projectId + "/subprojects/" + subProjectId + "/tasks/" + taskId + "/note"),
                HttpMethod.PATCH,
                new HttpEntity<>(new TaskNoteRequest("API done - needs tests"), jsonHeaders(member.token())),
                String.class
        );

        assertTrue(saveNoteResp.getStatusCode().is2xxSuccessful(), "Expected saved note");

        updatedTask = taskRepository.getTaskById(taskId);
        assertEquals("API done - needs tests", updatedTask.getTaskNote());

        // ---------------------------------------------------------
        // 8) Afslut: simple sanity checks på relationer
        // ---------------------------------------------------------
        assertEquals(1, projectRepository.showProjectsByEmployeeId(pmId).size());
        assertEquals(1, projectRepository.showSubProjectsByProjectId(projectId).size());
        assertEquals(1, taskRepository.showTasksBySubProjectId(subProjectId).size());
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private AuthResponse register(String username, String email, EmployeeRole role, AlphaRole alphaRole) {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest(username, "password123", email, role, alphaRole);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                baseUrl("/api/auth/register"),
                new HttpEntity<>(request, jsonHeaders(null)),
                AuthResponse.class
        );
        assertTrue(response.getStatusCode().is2xxSuccessful(), "Expected registered user");
        assertNotNull(response.getBody());
        return response.getBody();
    }

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }
}
