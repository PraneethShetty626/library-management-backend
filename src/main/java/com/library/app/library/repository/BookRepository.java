package com.library.app.library.repository;

import com.library.app.library.model.Book;
import com.library.app.auth.model.LibraryUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Find all books by title (titles can repeat)
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Find all books by author
    Page<Book> findByAuthorContainingIgnoreCase(String author, Pageable pageable);

    Page<Book> findByAvailableFalseAndBorrower(LibraryUser borrower, Pageable pageable);

    // Find all available books
    Page<Book> findByAvailableTrue(Pageable pageable);

    // Find all borrowed books
    Page<Book> findByAvailableFalse(Pageable pageable);

    // Find all books borrowed by a specific user
    Page<Book> findByBorrower(LibraryUser borrower,Pageable pageable);

    Optional<Book> findByIsbn(String isbn);
}
