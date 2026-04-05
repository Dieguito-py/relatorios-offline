package com.ifscxxe.relatorios_offline.coordenadoria.repository;

import com.ifscxxe.relatorios_offline.coordenadoria.model.Municipal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MunicipalRepository extends JpaRepository<Municipal, Long> {
    List<Municipal> findByRegionalId(Long regionalId);
}


