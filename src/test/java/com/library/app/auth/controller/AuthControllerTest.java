package com.library.app.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.app.auth.model.LibraryUserRoles;
import com.library.app.auth.model.LoginRequest;
import com.library.app.auth.model.RegisterRequest;
import com.library.app.auth.repository.LibraryUserRepository;
import com.library.app.auth.service.LibraryUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryUserService libraryUserService;

    @Autowired
    private LibraryUserRepository libraryUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        // Clean DB
        libraryUserRepository.deleteAll();

        // Create admin user
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setUsername("admin");
        adminReq.setPassword("admin123");
        adminReq.setRoles(Set.of(LibraryUserRoles.ROLE_ADMIN, LibraryUserRoles.ROLE_USER));

        libraryUserService.register(adminReq);

        // Login as admin to get token
        LoginRequest adminLogin = new LoginRequest("admin", "admin123");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        adminToken = result.getResponse().getContentAsString();
    }



    @Test
    void testAdminTokenIsGenerated() {
        assert adminToken != null && !adminToken.isBlank();
    }


    @Test
    void testRegisterUser_AsAdmin_Success() throws Exception {
        RegisterRequest newUser = getRegisterRequest();

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isConflict());

    }


    @Test
    void testRegisterUser_WithoutToken_Forbidden() throws Exception {
        RegisterRequest newUser = getRegisterRequest();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_Success_ForRegisteredUser() throws Exception {
        // First register the user
        RegisterRequest newUser = getRegisterRequest();

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest login = new LoginRequest(newUser.getUsername(), newUser.getPassword());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk());
    }

    @Test
    void testLogin_Fails_WrongPassword() throws Exception {
        LoginRequest login = new LoginRequest("admin", "wrongpass");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testLogin_Fails_UserNotFound() throws Exception {
        LoginRequest login = new LoginRequest("ghost", "nope");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRegisterUser_AsUser_Fail() throws Exception {
        RegisterRequest newUser = getRegisterRequest();

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(newUser.getUsername(), newUser.getPassword());

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String token = result.getResponse().getContentAsString();

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isForbidden());

    }

    private RegisterRequest getRegisterRequest() {
        RegisterRequest newUser = new RegisterRequest();
        newUser.setUsername("john");
        newUser.setPassword("password123");
        newUser.setRoles(Set.of(LibraryUserRoles.ROLE_USER));
        return newUser;
    }


}
