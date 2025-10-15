package com.library.app.auth.repository;

import com.library.app.auth.model.LibraryUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNullApi;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface LibraryUserRepository extends JpaRepository<LibraryUser, Long> {
    Optional<LibraryUser> findByUsername(String username);
    Page<LibraryUser> findAll(Pageable pageable);
    Page<LibraryUser> findByUsernameContainingIgnoreCase(String username,Pageable pageable);
}