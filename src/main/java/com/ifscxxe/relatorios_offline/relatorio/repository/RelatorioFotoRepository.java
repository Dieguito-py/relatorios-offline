package com.ifscxxe.relatorios_offline.relatorio.repository;

import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelatorioFotoRepository extends JpaRepository<RelatorioFoto, Long> {
    List<RelatorioFoto> findByRelatorioId(Long relatorioId);
}
