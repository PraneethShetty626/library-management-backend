package com.library.app.auth.config;

import com.library.app.auth.model.LibraryUser;
import com.library.app.auth.model.LibraryUserRoles;
import com.library.app.auth.model.RegisterRequest;
import com.library.app.auth.repository.LibraryUserRepository;
import com.library.app.auth.service.LibraryUserService;
import org.hibernate.annotations.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Profile("!test")
public class AdminInitializer implements CommandLineRunner {
    @Autowired
    private LibraryUserService userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            RegisterRequest user = new RegisterRequest();
            user.setUsername("admin");
            user.setPassword("admin123");

            Set<LibraryUserRoles> roles = new HashSet<>();

            roles.add(LibraryUserRoles.ROLE_ADMIN);
            roles.add(LibraryUserRoles.ROLE_USER);

            user.setRoles(roles);

            userRepository.register(user);
        }
    }
}
