package com.library.app.auth.service;

import com.library.app.auth.model.LibraryUser;
import com.library.app.auth.model.UserPrincipal;
import com.library.app.auth.repository.LibraryUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthLibraryUserService implements UserDetailsService {
    @Autowired
    private LibraryUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<LibraryUser> user = userRepository.findByUsername(username);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return new UserPrincipal(user.get());
    }

}
