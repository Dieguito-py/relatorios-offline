package com.ifscxxe.relatorios_offline.api.controller;

import com.ifscxxe.relatorios_offline.api.dto.auth.request.LoginRequest;
import com.ifscxxe.relatorios_offline.api.dto.auth.request.RegisterRequest;
import com.ifscxxe.relatorios_offline.api.dto.auth.response.LoginResponse;
import com.ifscxxe.relatorios_offline.api.dto.auth.response.RegisterResponse;
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
                    if (Boolean.FALSE.equals(usuario.getAtivo())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "usuário desativado"));
                    }
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
                    System.out.println("Usuário logado via API: " + usuario.getUsername());
                    Long municipalId = usuario.getMunicipal() != null ? usuario.getMunicipal().getId() : null;
                    String municipalNome = usuario.getMunicipal() != null ? usuario.getMunicipal().getNome() : null;
                    return ResponseEntity.ok(new LoginResponse(token, usuario.getNome(), municipalId, municipalNome));
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

        Set<Role> roleSet = request.roles().stream()
                .filter(r -> r != null && !r.isBlank())
                .map(Role::fromString)
                .filter(r -> r != null)
                .collect(Collectors.toCollection(HashSet::new));

        if (roleSet.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "roles inválidas"));
        }

        String encoded = passwordEncoder.encode(request.password());
        Usuario usuario = new Usuario();
        usuario.setUsername(request.username());
        usuario.setPassword(encoded);
        usuario.setRoles(roleSet);
        usuario.setAtivo(true);
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
        boolean ativo = usuarioRepository.findByUsername(username)
                .map(usuario -> !Boolean.FALSE.equals(usuario.getAtivo()))
                .orElse(false);
        if (!ativo) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }
        var roles = jwtProvider.getAuthorities(token).stream().map(GrantedAuthority::getAuthority).toList();
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "username", username,
                "roles", roles
        ));
    }

}
