package com.cloudFileStorageSystem.security.auth;

import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.UsersRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final PasswordEncoder passwordEncoder;
    private final UsersRepository usersRepository;

    public CustomAuthenticationProvider(PasswordEncoder passwordEncoder, UsersRepository usersRepository) {
        this.passwordEncoder = passwordEncoder;
        this.usersRepository = usersRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String identifier = authentication.getName();
        String password = authentication.getCredentials().toString();

        Users user;

        if (identifier.contains("@")) {

            user = usersRepository.findByEmail(identifier)
                    .orElseThrow(() ->
                            new BadCredentialsException("Invalid credentials"));

        } else if (identifier.matches("\\d{10}")) {

            user = usersRepository.findByPhoneNumber(identifier)
                    .orElseThrow(() ->
                            new BadCredentialsException("Invalid credentials"));

        } else {

            user = usersRepository.findByUsername(identifier)
                    .orElseThrow(() ->
                            new BadCredentialsException("Invalid credentials"));
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),   // or user object
                null,
                List.of()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);
    }
}
