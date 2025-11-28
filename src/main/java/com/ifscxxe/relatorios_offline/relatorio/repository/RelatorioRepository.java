package com.ifscxxe.relatorios_offline.relatorio.repository;

import com.ifscxxe.relatorios_offline.relatorio.model.Relatorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RelatorioRepository extends JpaRepository<Relatorio, Long> {
    List<Relatorio> findByCoordenadoriaMunicipalIdOrderByIdDesc(Long coordenadoriaId);
    List<Relatorio> findByCoordenadoriaMunicipalIdAndDataDesastreBetweenOrderByDataDesastreDesc(Long coordenadoriaId, LocalDateTime inicio, LocalDateTime fim);
}
