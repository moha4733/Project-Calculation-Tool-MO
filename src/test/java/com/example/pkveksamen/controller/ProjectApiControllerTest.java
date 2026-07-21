package com.example.pkveksamen.controller;

import com.example.pkveksamen.dto.AuthResponse;
import com.example.pkveksamen.dto.ProjectRequest;
import com.example.pkveksamen.dto.RegisterEmployeeRequest;
import com.example.pkveksamen.model.AlphaRole;
import com.example.pkveksamen.model.EmployeeRole;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectApiControllerTest {

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
    void projects_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectManager_canCreateProject() throws Exception {
        AuthResponse auth = register("pm", "pm@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);

        ProjectRequest request = new ProjectRequest(
                "API Migration",
                "Move project flow to REST",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "Alpha Solutions"
        );

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.projectName").value("API Migration"))
                .andExpect(jsonPath("$.projectDescription").value("Move project flow to REST"))
                .andExpect(jsonPath("$.projectCustomer").value("Alpha Solutions"))
                .andExpect(jsonPath("$.owner.employeeId").value(auth.employee().employeeId()));
    }

    @Test
    void teamMember_cannotCreateProject() throws Exception {
        AuthResponse auth = register("tm", "tm@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);

        ProjectRequest request = new ProjectRequest(
                "Forbidden project",
                "Team member should not create this",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "Alpha Solutions"
        );

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectsList_containsOwnedAndAssignedProjects() throws Exception {
        AuthResponse manager = register("manager", "manager@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse member = register("member", "member@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);

        projectRepository.createProject(
                "Owned project",
                "Visible because manager owns it",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                "Alpha",
                manager.employee().employeeId()
        );
        projectRepository.createProject(
                "Assigned project",
                "Visible because member is assigned",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                "Beta",
                manager.employee().employeeId()
        );
        long assignedProjectId = projectRepository.showProjectsByEmployeeId(manager.employee().employeeId()).get(1).getProjectID();
        projectRepository.addEmployeeToProject(member.employee().employeeId(), assignedProjectId);

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].projectName").value("Assigned project"))
                .andExpect(jsonPath("$[0].owner.password").doesNotExist());
    }

    @Test
    void getProjectById_returnsProjectWhenAuthenticated() throws Exception {
        AuthResponse manager = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        projectRepository.createProject(
                "Single project",
                "Details",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                "Gamma",
                manager.employee().employeeId()
        );

        mockMvc.perform(get("/api/projects/1")
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.projectName").value("Single project"))
                .andExpect(jsonPath("$.owner.username").value("owner"));
    }

    @Test
    void projectMembers_returnsOnlyMembersForAccessibleProject() throws Exception {
        AuthResponse manager = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse member = register("member", "member@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        long projectId = createProject(manager.employee().employeeId());
        projectRepository.addEmployeeToProject(member.employee().employeeId(), projectId);

        mockMvc.perform(get("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("owner"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[1].username").value("member"));
    }

    @Test
    void projectMembers_rejectsOutsider() throws Exception {
        AuthResponse manager = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse outsider = register("outsider", "outsider@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        long projectId = createProject(manager.employee().employeeId());

        mockMvc.perform(get("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void availableEmployees_returnsEmployeesNotAlreadyOnProject() throws Exception {
        AuthResponse manager = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse member = register("member", "member@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        AuthResponse available = register("available", "available@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Tester);
        long projectId = createProject(manager.employee().employeeId());
        projectRepository.addEmployeeToProject(member.employee().employeeId(), projectId);

        mockMvc.perform(get("/api/projects/" + projectId + "/available-employees")
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId").value(available.employee().employeeId()))
                .andExpect(jsonPath("$[0].username").value("available"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void availableEmployees_rejectsOutsider() throws Exception {
        AuthResponse manager = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse outsider = register("outsider", "outsider@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        register("available", "available@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        long projectId = createProject(manager.employee().employeeId());

        mockMvc.perform(get("/api/projects/" + projectId + "/available-employees")
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectManager_canAddMemberToProject() throws Exception {
        AuthResponse manager = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse member = register("member", "member@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        long projectId = createProject(manager.employee().employeeId());

        mockMvc.perform(post("/api/projects/" + projectId + "/members/" + member.employee().employeeId())
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(member.employee().employeeId()))
                .andExpect(jsonPath("$.username").value("member"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].username").value("member"));
    }

    @Test
    void teamMember_cannotAddMemberToProject() throws Exception {
        AuthResponse manager = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse member = register("member", "member@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        AuthResponse available = register("available", "available@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Tester);
        long projectId = createProject(manager.employee().employeeId());
        projectRepository.addEmployeeToProject(member.employee().employeeId(), projectId);

        mockMvc.perform(post("/api/projects/" + projectId + "/members/" + available.employee().employeeId())
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectManager_canRemoveMemberFromProject() throws Exception {
        AuthResponse manager = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        AuthResponse member = register("member", "member@alpha.dk", EmployeeRole.TEAM_MEMBER, AlphaRole.Developer);
        long projectId = createProject(manager.employee().employeeId());
        projectRepository.addEmployeeToProject(member.employee().employeeId(), projectId);

        mockMvc.perform(delete("/api/projects/" + projectId + "/members/" + member.employee().employeeId())
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("owner"));
    }

    @Test
    void projectManager_cannotRemoveProjectOwner() throws Exception {
        AuthResponse manager = register("owner", "owner@alpha.dk", EmployeeRole.PROJECT_MANAGER, AlphaRole.ProjectManager);
        long projectId = createProject(manager.employee().employeeId());

        mockMvc.perform(delete("/api/projects/" + projectId + "/members/" + manager.employee().employeeId())
                        .header("Authorization", "Bearer " + manager.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Project owner cannot be removed"));
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
}
