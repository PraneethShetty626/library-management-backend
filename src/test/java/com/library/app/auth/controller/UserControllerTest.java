package com.library.app.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.app.auth.model.LibraryUser;
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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryUserService libraryUserService;

    @Autowired
    private LibraryUserRepository libraryUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;
    private Long userId;

    @BeforeEach
    void setup() throws Exception {
        libraryUserRepository.deleteAll();


        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setUsername("admin");
        adminReq.setPassword("admin123");
        adminReq.setRoles(Set.of(LibraryUserRoles.ROLE_ADMIN, LibraryUserRoles.ROLE_USER));
        libraryUserService.register(adminReq);


        RegisterRequest userReq = new RegisterRequest();
        userReq.setUsername("john");
        userReq.setPassword("john123");
        userReq.setRoles(Set.of(LibraryUserRoles.ROLE_USER));
        libraryUserService.register(userReq);

        Optional<LibraryUser> user = libraryUserRepository.findByUsername("john");
        userId = user.map(LibraryUser::getId).orElseThrow();


        LoginRequest adminLogin = new LoginRequest("admin", "admin123");
        MvcResult adminRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = adminRes.getResponse().getContentAsString();

        LoginRequest userLogin = new LoginRequest("john", "john123");
        MvcResult userRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn();
        userToken = userRes.getResponse().getContentAsString();
    }


    @Test
    void testGetAllUsers_AsAdmin_ShouldReturnUsers() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testGetAllUsers_AsUser_ShouldFailForbidden() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetUser_ByUsername_AsAdmin() throws Exception {
        mockMvc.perform(get("/users/user")
                        .param("username", "john")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void testGetUser_ById_NotFound() throws Exception {
        mockMvc.perform(get("/users/user")
                        .param("id", "9999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateUserName_AsSelf_ShouldWork() throws Exception {
        mockMvc.perform(put("/users/" + userId + "/updateName")
                        .param("username", "johnny")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johnny"));
    }

    @Test
    void testUpdateUserName_AsOtherUser_ShouldFailForbidden() throws Exception {
        mockMvc.perform(put("/users/" + 1 + "/updateName")
                        .param("username", "johnny")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDisableAndEnableUser_AsAdmin() throws Exception {
        // disable
        mockMvc.perform(patch("/users/" + userId + "/disable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // enable
        mockMvc.perform(patch("/users/" + userId + "/enable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testExpireUser_AsAdmin() throws Exception {
        mockMvc.perform(patch("/users/" + userId + "/expire")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdatePassword_AsSelf_ShouldSucceed() throws Exception {
        mockMvc.perform(patch("/users/" + userId + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newpass")))
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Password updated successfully."));
    }

    @Test
    void testUpdatePassword_AsAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(patch("/users/" + userId + "/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "resetpass")))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchUsersByName_AsAdmin_ShouldReturnResults() throws Exception {
        mockMvc.perform(get("/users/search")
                        .param("name", "john")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").exists());
    }

    @Test
    void testDeleteUser_AsAdmin_ShouldSucceed() throws Exception {
        mockMvc.perform(delete("/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(libraryUserRepository.findById(userId)).isEmpty();
    }

    @Test
    void testDeleteUser_AsUser_ShouldFailForbidden() throws Exception {
        // Create another user to try deleting
        RegisterRequest newUserReq = new RegisterRequest();
        newUserReq.setUsername("mike");
        newUserReq.setPassword("mike123");
        newUserReq.setRoles(Set.of(LibraryUserRoles.ROLE_USER));
        libraryUserService.register(newUserReq);

        Optional<LibraryUser> mike = libraryUserRepository.findByUsername("mike");
        Long mikeId = mike.map(LibraryUser::getId).orElseThrow();

        mockMvc.perform(delete("/users/" + mikeId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}