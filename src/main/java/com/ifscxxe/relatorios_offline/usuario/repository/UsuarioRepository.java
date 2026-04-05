package com.ifscxxe.relatorios_offline.usuario.repository;

import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    List<Usuario> findByRegionalId(Long regionalId);
    boolean existsByRegionalId(Long regionalId);
    boolean existsByMunicipalId(Long municipalId);
}
