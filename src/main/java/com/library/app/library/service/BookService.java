package com.library.app.library.service;

import com.library.app.auth.service.LibraryUserService;
import com.library.app.library.model.Book;
import com.library.app.auth.model.LibraryUser;
import com.library.app.library.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class BookService {

    @Autowired
    private  BookRepository bookRepository;


    // Save or update a book
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }

    // Get all books (paged)
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    // Get book by ID
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    // Get books by title
    public Page<Book> getBooksByTitle(String title, Pageable pageable) {
        return bookRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    // Get books by author
    public Page<Book> getBooksByAuthor(String author, Pageable pageable) {
        return bookRepository.findByAuthorContainingIgnoreCase(author, pageable);
    }

    // Get available books
    public Page<Book> getAvailableBooks(Pageable pageable) {
        return bookRepository.findByAvailableTrue(pageable);
    }

    // Get borrowed books
    public Page<Book> getBorrowedBooks(Pageable pageable) {
        return bookRepository.findByAvailableFalse(pageable);
    }

    public Page<Book> getBorrowedBooksByUser(Pageable pageable, LibraryUser user) {
        return bookRepository.findByAvailableFalseAndBorrower(user,pageable);
    }

    // Get books borrowed by a user
    public Page<Book> getBooksByBorrower(LibraryUser borrower, Pageable pageable) {
        return bookRepository.findByBorrower(borrower, pageable);
    }

    // Borrow a book
    public Optional<Book> borrowBook(Long bookId, LibraryUser user) {
        return bookRepository.findById(bookId).map(book -> {
            if (!book.isAvailable()) {
                throw new IllegalStateException("Book is already borrowed");
            }


            book.setAvailable(false);
            book.setBorrower(user);

            return bookRepository.save(book);
        });
    }

    // Return a book
    public Optional<Book> returnBook(Long bookId) {
        return bookRepository.findById(bookId).map(book -> {
            if (book.isAvailable()) {
                throw new IllegalStateException("Book is not currently borrowed");
            }
            book.setAvailable(true);
            book.setBorrower(null);
            return bookRepository.save(book);
        });
    }

    // Delete a book
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }
}
