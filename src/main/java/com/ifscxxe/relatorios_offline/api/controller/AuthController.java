package com.ifscxxe.relatorios_offline.api.controller;

import com.ifscxxe.relatorios_offline.core.providers.JWTprovider;
import com.ifscxxe.relatorios_offline.usuario.model.Role;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JWTprovider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;

    public AuthController(JWTprovider jwtProvider, PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository) {
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "username é obrigatório"));
        }
        if (request.password() == null || request.password().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "password é obrigatório"));
        }

        return usuarioRepository.findByUsername(request.username())
                .map(usuario -> {
                    if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "credenciais inválidas"));
                    }
                    List<SimpleGrantedAuthority> authorities = usuario.getRoles().stream()
                            .map(Role::asAuthority)
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    UserDetails userDetails = User.withUsername(usuario.getUsername())
                            .password(usuario.getPassword())
                            .authorities(authorities)
                            .build();

                    String token = jwtProvider.generateToken(userDetails);
                    return ResponseEntity.ok(new LoginResponse(token));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "credenciais inválidas")));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "username é obrigatório"));
        }
        if (request.password() == null || request.password().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "password é obrigatório"));
        }
        if (request.roles() == null || request.roles().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "roles é obrigatório"));
        }

        if (usuarioRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "usuário já existe"));
        }

        // Map roles coming from frontend to enum values, ignoring blanks and duplicates
        Set<Role> roleSet = request.roles().stream()
                .filter(r -> r != null && !r.isBlank())
                .map(Role::fromString)
                .filter(r -> r != null)
                .collect(Collectors.toCollection(HashSet::new));

        if (roleSet.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "roles inválidas"));
        }

        String encoded = passwordEncoder.encode(request.password());
        Usuario usuario = new Usuario(request.username(), encoded, roleSet);
        Usuario saved = usuarioRepository.save(usuario);

        List<String> normalizedRoles = saved.getRoles().stream()
                .map(Role::asAuthority)
                .sorted()
                .toList();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(saved.getUsername(), normalizedRoles));
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
    public record RegisterRequest(String username, String password, List<String> roles) {}
    public record RegisterResponse(String username, List<String> roles) {}
}
