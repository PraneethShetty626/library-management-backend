package com.library.app.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.app.auth.model.LibraryUser;
import com.library.app.auth.model.LibraryUserRoles;
import com.library.app.auth.model.LoginRequest;
import com.library.app.auth.model.RegisterRequest;
import com.library.app.auth.repository.LibraryUserRepository;
import com.library.app.auth.service.LibraryUserService;
import com.library.app.library.model.Book;
import com.library.app.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LibraryUserService libraryUserService;

    @Autowired
    private LibraryUserRepository libraryUserRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;
    private Book sampleBook;

    @BeforeEach
    void setUpUsers() throws Exception {
        bookRepository.deleteAll();
        libraryUserRepository.deleteAll();

        // Create Admin
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setUsername("admin");
        adminReq.setPassword("admin123");
        adminReq.setRoles(Set.of(LibraryUserRoles.ROLE_ADMIN, LibraryUserRoles.ROLE_USER));
        libraryUserService.register(adminReq);

        // Create Normal User
        RegisterRequest userReq = new RegisterRequest();
        userReq.setUsername("john");
        userReq.setPassword("john123");
        userReq.setRoles(Set.of(LibraryUserRoles.ROLE_USER));
        libraryUserService.register(userReq);

        // Login as Admin
        LoginRequest adminLogin = new LoginRequest("admin", "admin123");
        MvcResult adminRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = adminRes.getResponse().getContentAsString();

        // Login as User
        LoginRequest userLogin = new LoginRequest("john", "john123");
        MvcResult userRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userLogin)))
                .andExpect(status().isOk())
                .andReturn();
        userToken = userRes.getResponse().getContentAsString();

        // Create one book by admin
        sampleBook = new Book();
        sampleBook.setTitle("Effective Java");
        sampleBook.setAuthor("Joshua Bloch");
        sampleBook.setIsbn("0134685991");
        sampleBook.setAvailable(true);

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleBook)))
                .andExpect(status().isCreated());
    }

    /**
     * ✅ Test: Book registration by admin (success and duplicate failure)
     */
    @Test
    void testBookRegistration() throws Exception {
        Book newBook = new Book();
        newBook.setTitle("Clean Code");
        newBook.setAuthor("Robert C. Martin");
        newBook.setIsbn("0132350882");
        newBook.setAvailable(true);

        // Successful create
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBook)))
                .andExpect(status().isCreated());

        // Duplicate ISBN or same title → Should fail with 500
        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newBook)))
                .andExpect(status().isInternalServerError());
    }

    /**
     * ❌ Test: Normal user cannot register a book
     */
    @Test
    void testBookRegistrationByUserShouldFail() throws Exception {
        Book anotherBook = new Book();
        anotherBook.setTitle("The Pragmatic Programmer");
        anotherBook.setAuthor("Andy Hunt");
        anotherBook.setIsbn("020161622X");

        mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anotherBook)))
                .andExpect(status().isForbidden());
    }

    /**
     * ✅ Test: Borrow a book by user
     */
    @Test
    void testBorrowBook() throws Exception {
        Long bookId = bookRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/books/" + bookId + "/borrow")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        Optional<Book> borrowed = bookRepository.findById(bookId);
        assertThat(borrowed).isPresent();
        assertThat(borrowed.get().isAvailable()).isFalse();
    }

    /**
     * ✅ Test: Return borrowed book by same user
     */
    @Test
    void testReturnBook() throws Exception {
        Long bookId = bookRepository.findAll().get(0).getId();

        // Borrow first
        mockMvc.perform(post("/api/books/" + bookId + "/borrow")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Return it
        mockMvc.perform(post("/api/books/" + bookId + "/return")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        Optional<Book> returned = bookRepository.findById(bookId);
        assertThat(returned).isPresent();
        assertThat(returned.get().isAvailable()).isTrue();
    }

    /**
     * ❌ Test: Unauthorized return attempt (wrong user)
     */
    @Test
    void testReturnBookUnauthorized() throws Exception {
        Long bookId = bookRepository.findAll().get(0).getId();

        // Borrow with john
        mockMvc.perform(post("/api/books/" + bookId + "/borrow")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Register another user
        RegisterRequest user2Req = new RegisterRequest();
        user2Req.setUsername("alice");
        user2Req.setPassword("alice123");
        user2Req.setRoles(Set.of(LibraryUserRoles.ROLE_USER));
        libraryUserService.register(user2Req);

        // Login as alice
        LoginRequest aliceLogin = new LoginRequest("alice", "alice123");
        MvcResult aliceRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aliceLogin)))
                .andExpect(status().isOk())
                .andReturn();
        String aliceToken = aliceRes.getResponse().getContentAsString();

        // Alice tries to return john's book
        mockMvc.perform(post("/api/books/" + bookId + "/return")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNotAcceptable());
    }

    /**
     * ✅ Test: Get all books (no pagination params)
     */
    @Test
    void testGetAllBooks() throws Exception {
        mockMvc.perform(get("/api/books")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    /**
     * ✅ Test: Delete book (admin only)
     */
    @Test
    void testDeleteBookByAdmin() throws Exception {
        Long bookId = bookRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/books/" + bookId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    /**
     * ❌ Test: Delete book by normal user should fail
     */
    @Test
    void testDeleteBookByUserShouldFail() throws Exception {
        Long bookId = bookRepository.findAll().get(0).getId();

        mockMvc.perform(delete("/api/books/" + bookId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
