package com.vectoredu.backend.service.integration;

import com.jayway.jsonpath.JsonPath;
import com.vectoredu.backend.service.config.AbstractIntegrationTest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
@Tag(name = "Аутентификация", description = "Операции, связанные с аутентификацией")
public class AuthenticationControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void clearDatabase() {
        jdbcTemplate.execute("""
            DELETE FROM password_reset;
            DELETE FROM users;
        """);
    }

    @BeforeEach
    public void setup() throws Exception {
        String json = """
            {
                "email": "test@example.com", 
                "password": "Password123", 
                "firstName": "Данил", 
                "lastName": "Богомолов"
            }
        """;

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Данил"))
                .andExpect(jsonPath("$.lastName").value("Богомолов"));
    }

    @Test
    public void testVerifyUser() throws Exception {
        String email = "test@example.com";

        String verificationCode = jdbcTemplate.queryForObject("""
            SELECT verification_code 
            FROM users 
            WHERE email = ?
        """, new Object[]{email}, String.class);

        String json = String.format("""
            {
                "email": "%s", 
                "verificationCode": "%s"
            }
        """, email, verificationCode);

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Аккаунт успешно подтвержден"));
    }

    @Test
    public void testSuccessLoginUser() throws Exception {
        String email = "test@example.com";
        String password = "Password123";

        String verificationCode = jdbcTemplate.queryForObject("""
            SELECT verification_code 
            FROM users 
            WHERE email = ?
        """, new Object[]{email}, String.class);

        String json1 = String.format("""
            {
                "email": "%s", 
                "verificationCode": "%s"
            }
        """, email, verificationCode);

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Аккаунт успешно подтвержден"));

        String json2 = String.format("""
            {
                "email": "%s", 
                "password": "%s"
            }
        """, email, password);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json2))
                .andExpect(status().isOk());
    }

    @Test
    public void testResendVerificationCode() throws Exception {
        mockMvc.perform(post("/auth/resend")
                        .param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Код для подтверждения отправлен"));
    }

    @Test
    public void testRefreshAccessToken() throws Exception {
        String email = "test@example.com";
        String password = "Password123";

        String verificationCode = jdbcTemplate.queryForObject("""
            SELECT verification_code 
            FROM users 
            WHERE email = ?
        """, new Object[]{email}, String.class);

        String verifyJson = String.format("""
            {
                "email": "%s", 
                "verificationCode": "%s"
            }
        """, email, verificationCode);

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Аккаунт успешно подтвержден"));

        String loginJson = String.format("""
            {
                "email": "%s", 
                "password": "%s"
            }
        """, email, password);

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.refreshToken");

        String refreshJson = String.format("""
            {
                "token": "%s"
            }
        """, refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isString());
    }

    @Test
    public void testRegisterUserWithInvalidEmail() throws Exception {
        String json = """
            {
                "email": "invalid-email", 
                "password": "Password123", 
                "firstName": "Test", 
                "lastName": "User"
            }
        """;

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Не верный формат email"));
    }

    @Test
    public void testLoginWithInvalidCredentials() throws Exception {
        String json = """
            {
                "email": "test@example.com", 
                "password": "WrongPassword"
            }
        """;

        String email = "test@example.com";

        jdbcTemplate.update("""
            UPDATE users SET enabled = TRUE 
            WHERE email = ?
        """, email);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Данные введены некорректно"));
    }

    @Test
    public void testVerifyUserWithInvalidCode() throws Exception {
        String email = "test@example.com";
        String invalidVerificationCode = "000000";

        String json = String.format("""
            {
                "email": "%s", 
                "verificationCode": "%s"
            }
        """, email, invalidVerificationCode);

        mockMvc.perform(post("/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный код подтверждения"));
    }

    @Test
    public void testResendVerificationCodeForVerifiedUser() throws Exception {
        String email = "test@example.com";

        jdbcTemplate.update("""
            UPDATE users SET enabled = TRUE 
            WHERE email = ?
        """, email);

        mockMvc.perform(post("/auth/resend")
                        .param("email", email))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Аккаунт уже подтвержден"));
    }
}