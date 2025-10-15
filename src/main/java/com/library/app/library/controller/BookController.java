package com.library.app.library.controller;

import com.library.app.auth.model.LibraryUser;
import com.library.app.auth.service.LibraryUserService;
import com.library.app.library.model.Book;
import com.library.app.library.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;


    @Autowired
    private LibraryUserService userDetailsService;


    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // Utility to validate and build pageable
    private Pageable getPageable(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Invalid pagination parameters.");
        }
        return PageRequest.of(page, size, Sort.by("title").ascending());
    }

    /**
     * Get all books (paginated)
     */
    @GetMapping
    public ResponseEntity<?> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = getPageable(page, size);
            Page<Book> books = bookService.getAllBooks(pageable);
            return books.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                    : ResponseEntity.ok(books);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get a book by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Book not found with ID: " + id));
    }

    /**
     * Get books by title (paginated)
     */
    @GetMapping("/title/{title}")
    public ResponseEntity<?> getBooksByTitle(
            @PathVariable String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = getPageable(page, size);
            Page<Book> books = bookService.getBooksByTitle(title, pageable);
            return books.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                    : ResponseEntity.ok(books);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get books by author (paginated)
     */
    @GetMapping("/author/{author}")
    public ResponseEntity<?> getBooksByAuthor(
            @PathVariable String author,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = getPageable(page, size);
            Page<Book> books = bookService.getBooksByAuthor(author, pageable);
            return books.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                    : ResponseEntity.ok(books);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get all available books (paginated)
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = getPageable(page, size);
            Page<Book> books = bookService.getAvailableBooks(pageable);
            return books.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                    : ResponseEntity.ok(books);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get all borrowed books (paginated)
     */
    @GetMapping("/borrowed")
    public ResponseEntity<?> getAllBorrowedBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = getPageable(page, size);
            Page<Book> books = bookService.getBorrowedBooks(pageable);
            return books.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                    : ResponseEntity.ok(books);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/borrowed/{id}")
    public ResponseEntity<?> getBorrowedBooksByUserlear
            (
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable Long id,
            Principal principal
    ) {
        try {
            Optional<LibraryUser> currentUser = userDetailsService.getUserById(id);

            if (currentUser.isEmpty() ||
                    (!currentUser.get().getUsername().equals(principal.getName())
                            && !currentUser.get().getRoles().contains("ROLE_ADMIN"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not allowed to update this password.");
            }

            Pageable pageable = getPageable(page, size);
            Page<Book> books = bookService.getBorrowedBooksByUser(pageable,currentUser.get());
            return books.isEmpty()
                    ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
                    : ResponseEntity.ok(books);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Create a new book
     */
    @PostMapping
    public ResponseEntity<?> createBook(@RequestBody Book book) {
        try {
            if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Book title cannot be empty.");
            }
            Book saved = bookService.saveBook(book);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating book: " + e.getMessage());
        }
    }

    /**
     * Update an existing book
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id, @RequestBody Book updatedBook) {
        return bookService.getBookById(id)
                .<ResponseEntity<?>>map(existing -> {
                    existing.setTitle(updatedBook.getTitle());
                    existing.setAuthor(updatedBook.getAuthor());
                    existing.setIsbn(updatedBook.getIsbn());
                    existing.setAvailable(updatedBook.isAvailable());
                    existing.setBorrower(updatedBook.getBorrower());
                    return ResponseEntity.ok(bookService.saveBook(existing));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Book not found with ID: " + id));
    }

    /**
     * Borrow a book
     */
    @PostMapping("/{id}/borrow")
    public ResponseEntity<?> borrowBook(
            @PathVariable Long id,
            Principal principal
    ) {
        try {
            Optional<LibraryUser> _u = userDetailsService.getUserByUsername(principal.getName());

            if (_u.isEmpty()) {
                return  ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found to borrow this book.");
            }

            Optional<Book> borrowed = bookService.borrowBook(id, _u.get());
            return borrowed.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Book not found or not available for borrowing."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error borrowing book: " + e.getMessage());
        }
    }

    /**
     * Return a borrowed book
     */
    @PostMapping("/{id}/return")
    public ResponseEntity<?> returnBook(@PathVariable Long id, Principal principal) {
        try {
            Optional<LibraryUser> _u = userDetailsService.getUserByUsername(principal.getName());
            Optional<Book> _b = bookService.getBookById(id);

            if (_u.isEmpty() || _b.isEmpty() || _b.get().getBorrower() == null || !Objects.equals(_u.get().getId(), _b.get().getBorrower().getId())) {
                return  ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("You are not authorized to return this book.");
            }

            Optional<Book> returned = bookService.returnBook(id);
            return returned.<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Book not found or not currently borrowed."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error returning book: " + e.getMessage());
        }
    }

    /**
     * Delete a book
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        try {
            Optional<Book> existing = bookService.getBookById(id);
            if (existing.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Book not found with ID: " + id);
            }
            bookService.deleteBook(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting book: " + e.getMessage());
        }
    }
}
