package com.ifscxxe.relatorios_offline.coordenadoria.repository;

import com.ifscxxe.relatorios_offline.coordenadoria.model.CoordenadoriaMunicipal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoordenadoriaMunicipalRepository extends JpaRepository<CoordenadoriaMunicipal, Long> {
    List<CoordenadoriaMunicipal> findByRegionalId(Long regionalId);
}

