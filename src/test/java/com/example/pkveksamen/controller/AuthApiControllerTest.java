package com.example.pkveksamen.controller;

import com.example.pkveksamen.dto.LoginRequest;
import com.example.pkveksamen.dto.RegisterEmployeeRequest;
import com.example.pkveksamen.model.AlphaRole;
import com.example.pkveksamen.model.EmployeeRole;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void register_createsEmployeeAndReturnsTokenWithoutPassword() throws Exception {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest(
                "api.manager",
                "password123",
                "api.manager@alpha.dk",
                EmployeeRole.PROJECT_MANAGER,
                AlphaRole.ProjectManager
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.employee.username").value("api.manager"))
                .andExpect(jsonPath("$.employee.email").value("api.manager@alpha.dk"))
                .andExpect(jsonPath("$.employee.password").doesNotExist());
    }

    @Test
    void register_returnsConflictWhenUsernameOrEmailExists() throws Exception {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest(
                "api.member",
                "password123",
                "api.member@alpha.dk",
                EmployeeRole.TEAM_MEMBER,
                AlphaRole.Developer
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_returnsTokenWhenPasswordIsCorrect() throws Exception {
        register("api.login", "password123", "api.login@alpha.dk");

        LoginRequest request = new LoginRequest("api.login", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())))
                .andExpect(jsonPath("$.employee.username").value("api.login"));
    }

    @Test
    void login_returnsUnauthorizedWhenPasswordIsWrong() throws Exception {
        register("api.wrong", "password123", "api.wrong@alpha.dk");

        LoginRequest request = new LoginRequest("api.wrong", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeesMe_requiresValidToken() throws Exception {
        mockMvc.perform(get("/api/employees/me"))
                .andExpect(status().isUnauthorized());

        String token = register("api.secure", "password123", "api.secure@alpha.dk");

        mockMvc.perform(get("/api/employees/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("api.secure"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void employeesList_requiresValidToken() throws Exception {
        String token = register("api.list", "password123", "api.list@alpha.dk");

        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("api.list"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    private String register(String username, String password, String email) throws Exception {
        RegisterEmployeeRequest request = new RegisterEmployeeRequest(
                username,
                password,
                email,
                EmployeeRole.TEAM_MEMBER,
                AlphaRole.Developer
        );

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }
}
