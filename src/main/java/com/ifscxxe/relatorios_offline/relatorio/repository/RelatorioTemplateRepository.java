package com.ifscxxe.relatorios_offline.relatorio.repository;

import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelatorioTemplateRepository extends JpaRepository<RelatorioTemplate, Long> {
    List<RelatorioTemplate> findByRegionalIdOrderByNomeAsc(Long regionalId);

    Optional<RelatorioTemplate> findByIdAndRegionalId(Long id, Long regionalId);

    boolean existsByRegionalIdAndNomeIgnoreCase(Long regionalId, String nome);

    Optional<RelatorioTemplate> findByRegionalIdAndNomeIgnoreCase(Long regionalId, String nome);
}


