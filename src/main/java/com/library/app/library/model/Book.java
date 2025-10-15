package com.library.app.library.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.library.app.auth.model.LibraryUser;
import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "books")
@Data
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(unique = true, nullable = false)
    private String isbn;

    @Column(nullable = false)
    private boolean available = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", referencedColumnName = "id")
    @JsonIgnore
    private LibraryUser borrower;
}
