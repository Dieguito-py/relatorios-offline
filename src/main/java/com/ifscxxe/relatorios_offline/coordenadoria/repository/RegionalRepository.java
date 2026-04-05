package com.ifscxxe.relatorios_offline.coordenadoria.repository;

import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionalRepository extends JpaRepository<Regional, Long> {
	Page<Regional> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}

