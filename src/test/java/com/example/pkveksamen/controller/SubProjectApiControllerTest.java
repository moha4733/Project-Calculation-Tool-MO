package com.example.pkveksamen.controller;

import com.example.pkveksamen.dto.AuthResponse;
import com.example.pkveksamen.dto.RegisterEmployeeRequest;
import com.example.pkveksamen.dto.SubProjectRequest;
import com.example.pkveksamen.model.AlphaRole;
import com.example.pkveksamen.model.EmployeeRole;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubProjectApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void cleanDatabase() {
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
    void subProjects_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects/1/subprojects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectManager_canCreateSubProject() throws Exception {
        AuthResponse manager = register("pm", "pm@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        long projectId = createProject(manager.employee().employeeId());

        SubProjectRequest request = new SubProjectRequest(
                "Backend API",
                "Build REST endpoints",
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects")
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subProjectId").value(1))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.subProjectName").value("Backend API"))
                .andExpect(jsonPath("$.subProjectDescription").value("Build REST endpoints"));
    }

    @Test
    void teamMember_cannotCreateSubProject() throws Exception {
        AuthResponse manager = register("pm", "pm@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse member = register("tm", "tm@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        long projectId = createProject(manager.employee().employeeId());
        projectRepository.addEmployeeToProject(member.employee().employeeId(), projectId);

        SubProjectRequest request = new SubProjectRequest(
                "Should fail",
                "Team member cannot create subprojects",
                LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects")
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listSubProjects_returnsProjectSubProjects() throws Exception {
        AuthResponse manager = register("pm", "pm@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        long projectId = createProject(manager.employee().employeeId());
        createSubProject(projectId, "Backend API", "REST");
        createSubProject(projectId, "Frontend App", "Vanilla JS");

        mockMvc.perform(get("/api/projects/" + projectId + "/subprojects")
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].subProjectName").value("Backend API"))
                .andExpect(jsonPath("$[1].subProjectName").value("Frontend App"));
    }

    @Test
    void createSubProject_rejectsDatesOutsideProjectPeriod() throws Exception {
        AuthResponse manager = register("pm", "pm@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        long projectId = createProject(manager.employee().employeeId());

        SubProjectRequest request = new SubProjectRequest(
                "Too early",
                "Starts before parent project",
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects")
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Subproject start date must be within project period"));
    }

    @Test
    void createSubProject_rejectsDeadlineAfterProjectPeriod() throws Exception {
        AuthResponse manager = register("pm", "pm@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        long projectId = createProject(manager.employee().employeeId());

        SubProjectRequest request = new SubProjectRequest(
                "Too late",
                "Ends after parent project",
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 9, 1)
        );

        mockMvc.perform(post("/api/projects/" + projectId + "/subprojects")
                        .header("Authorization", "Bearer " + manager.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Subproject deadline must be within project period"));
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
                "Project period used for subproject validation",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "Alpha",
                ownerId
        );
        return projectRepository.showProjectsByEmployeeId(ownerId).get(0).getProjectID();
    }

    private void createSubProject(long projectId, String name, String description) {
        SubProject subProject = new SubProject();
        subProject.setSubProjectName(name);
        subProject.setSubProjectDescription(description);
        subProject.setSubProjectStartDate(LocalDate.of(2026, 8, 5));
        subProject.setSubProjectDeadline(LocalDate.of(2026, 8, 20));
        projectRepository.saveSubProject(subProject, projectId);
    }
}
