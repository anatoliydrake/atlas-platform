package com.atlas.platform.service;

import com.atlas.platform.dto.request.LoginRequest;
import com.atlas.platform.dto.request.RegisterRequest;
import com.atlas.platform.dto.response.AuthResponse;
import com.atlas.platform.dto.response.UserResponse;
import com.atlas.platform.entity.User;
import com.atlas.platform.repository.UserRepository;
import com.atlas.platform.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
    }

    public AuthResponse login(LoginRequest request) {
        User user = (User) loadUserByUsername(request.getUsernameOrEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user);

        return new AuthResponse(token, new UserResponse(user));
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (request.getTelegramId() != null && userRepository.existsByTelegramId(request.getTelegramId())) {
            throw new IllegalArgumentException("Telegram account already linked");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash password!
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setTelegramId(request.getTelegramId());

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser);

        return new AuthResponse(token, new UserResponse(savedUser));
    }
}
