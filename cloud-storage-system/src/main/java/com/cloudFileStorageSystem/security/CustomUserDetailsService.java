package com.cloudFileStorageSystem.security;
import com.cloudFileStorageSystem.module.Users;
import com.cloudFileStorageSystem.repository.UsersRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository  userRepository;

    public CustomUserDetailsService(UsersRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {

        Users user = userRepository.findByUsernameOrEmailOrPhoneNumber(identifier, identifier,identifier)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + identifier)
                );

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getId().toString())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_"+user.getRole()))
                .build();
    }

    public UserDetails loadUserByUserId(Long userId) {

        Users user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getId().toString())
                .password(user.getPassword())
                .build();
    }
}