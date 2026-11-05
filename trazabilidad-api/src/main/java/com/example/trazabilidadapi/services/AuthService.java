package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.models.AppUser;
import com.example.trazabilidadapi.models.LoginResponse;
import com.example.trazabilidadapi.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtService jwtService) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse authenticate(String username, String password) {
        // 1. Buscar si el usuario existe en la BD
        AppUser user = userRepository.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        }

        // 2. Validar la contraseña contra su hash BCrypt
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos.");
        }

        // 3. Si todo es correcto, devolvemos la respuesta de éxito sin la contraseña por seguridad
        return new LoginResponse(
                user.getUsername(),
                user.getName(),
                user.getRole(),
                "¡Autenticación en MonCorp exitosa!",
                jwtService.issueToken(user)
        );
    }
}