package com.ifscxxe.relatorios_offline.api.controller;

import com.ifscxxe.relatorios_offline.core.providers.JWTprovider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JWTprovider jwtProvider;

    public AuthController(JWTprovider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "username é obrigatório"));
        }

        Collection<? extends GrantedAuthority> authorities;
        if ("admin".equalsIgnoreCase(request.username())) {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        UserDetails user = User.withUsername(request.username())
                .password("")
                .authorities(authorities)
                .build();

        String token = jwtProvider.generateToken(user);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Authorization Bearer token ausente"));
        }
        String token = authHeader.substring(7);
        if (!jwtProvider.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }
        String username = jwtProvider.getUsernameFromToken(token);
        var roles = jwtProvider.getAuthorities(token).stream().map(GrantedAuthority::getAuthority).toList();
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "username", username,
                "roles", roles
        ));
    }

    public record LoginRequest(String username, String password) {}
    public record LoginResponse(String token) {}
}
