package com.ifscxxe.relatorios_offline.relatorio.repository;

import com.ifscxxe.relatorios_offline.relatorio.model.CadastroFamilia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CadastroFamiliaRepository extends JpaRepository<CadastroFamilia, Long> {
    List<CadastroFamilia> findByRegionalIdOrderByIdDesc(Long regionalId);
    List<CadastroFamilia> findByRegionalIdAndDataDesastreBetweenOrderByDataDesastreDesc(Long regionalId, LocalDateTime inicio, LocalDateTime fim);
    @EntityGraph(attributePaths = "fotosResidencia")
    Optional<CadastroFamilia> findByIdAndRegionalId(Long id, Long regionalId);
    List<CadastroFamilia> findByUsuarioIdOrderByDataDesastreDesc(Long usuarioId);
    boolean existsByRegionalId(Long regionalId);
    boolean existsByMunicipalId(Long municipalId);
}

