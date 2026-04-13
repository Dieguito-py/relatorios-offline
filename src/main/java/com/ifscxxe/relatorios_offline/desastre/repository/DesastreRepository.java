package com.ifscxxe.relatorios_offline.desastre.repository;

import com.ifscxxe.relatorios_offline.desastre.model.Desastre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesastreRepository extends JpaRepository<Desastre, Long> {
    List<Desastre> findAllByOrderByDataDesastreDescIdDesc();
}

