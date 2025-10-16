package com.library.app.auth.service;

import com.library.app.auth.config.JwtFilter;
import com.library.app.auth.model.LibraryUser;
import com.library.app.auth.model.LoginRequest;
import com.library.app.auth.model.RegisterRequest;
import com.library.app.auth.repository.LibraryUserRepository;
import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LibraryUserService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryUserService.class);

    @Autowired
    private LibraryUserRepository userRepository;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private AuthenticationManager authManager;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public void register(RegisterRequest user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        LibraryUser newUser = new LibraryUser();

        newUser.setRoles(user.getRoles());
        newUser.setUsername(user.getUsername());
        newUser.setPassword(encoder.encode(user.getPassword()));


        logger.info("Registering user: {}", newUser.toString());

        userRepository.save(newUser);
    }

    public String verify(LoginRequest user) {
        Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        if (authentication.isAuthenticated()) {
            Optional<LibraryUser> _u =  userRepository.findByUsername(user.getUsername());

            if (_u.isEmpty()) {
                throw new SecurityException("User not found");
            }

            return jwtService.generateToken(user.getUsername(), _u.get().getRoles());
        } else {
            throw new SecurityException("Invalid login credentials");
        }
    }

    public long count() {
        return  userRepository.count();
    }

    public Page<LibraryUser> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Optional<LibraryUser> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // ✅ Get user by username
    public Optional<LibraryUser> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<LibraryUser> updateUserName(Long id, String newUserName, String currentUsername) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setUsername(newUserName);
                    return userRepository.save(user);
                });
    }


    public Optional<LibraryUser> disableUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setEnabled(false);
            return userRepository.save(user);
        });
    }

    public Optional<LibraryUser> enableUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setEnabled(true);
            return userRepository.save(user);
        });
    }

    public Optional<LibraryUser> expireUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setExpired(true);
            return userRepository.save(user);
        });
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<LibraryUser> updatePassword(Long id, String newPassword) {
        return userRepository.findById(id).map(user -> {
            user.setPassword(encoder.encode(newPassword));
            return userRepository.save(user);
        });
    }

    public Page<LibraryUser> findByUsername(String name,Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCase(name, pageable);
    }


    private boolean isAdmin(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getRoles().contains("ROLE_ADMIN"))
                .orElse(false);
    }
}
