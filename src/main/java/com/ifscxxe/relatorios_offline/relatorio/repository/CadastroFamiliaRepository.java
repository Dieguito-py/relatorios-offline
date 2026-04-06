package com.ifscxxe.relatorios_offline.relatorio.repository;

import com.ifscxxe.relatorios_offline.relatorio.model.CadastroFamilia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    List<CadastroFamilia> findByDesastreIdOrderByDataDesastreDesc(Long desastreId);
    boolean existsByRegionalId(Long regionalId);
    boolean existsByMunicipalId(Long municipalId);
    List<CadastroFamilia> findByMunicipalIdOrderByIdDesc(Long municipalId);
    List<CadastroFamilia> findByMunicipalIdAndDataDesastreBetweenOrderByDataDesastreDesc(Long municipalId, LocalDateTime inicio, LocalDateTime fim);
    @EntityGraph(attributePaths = "fotosResidencia")
    Optional<CadastroFamilia> findByIdAndMunicipalId(Long id, Long municipalId);
    List<CadastroFamilia> findByDesastreIdAndMunicipalIdOrderByDataDesastreDesc(Long desastreId, Long municipalId);

    @Query("""
            select c from CadastroFamilia c
            where c.desastre is null
              and c.municipal.id = :municipalId
              and (:q = '' or lower(c.nomeAtingido) like lower(concat('%', :q, '%'))
                   or lower(c.cidadeAtingido) like lower(concat('%', :q, '%')))
              and c.dataDesastre >= :inicio
              and c.dataDesastre <= :fim
            """)
    Page<CadastroFamilia> buscarDisponiveisMunicipal(@Param("municipalId") Long municipalId,
                                                     @Param("q") String q,
                                                     @Param("inicio") LocalDateTime inicio,
                                                     @Param("fim") LocalDateTime fim,
                                                     Pageable pageable);

    @Query("""
            select c from CadastroFamilia c
            where c.desastre is null
              and c.regional.id = :regionalId
              and (:q = '' or lower(c.nomeAtingido) like lower(concat('%', :q, '%'))
                   or lower(c.cidadeAtingido) like lower(concat('%', :q, '%')))
              and c.dataDesastre >= :inicio
              and c.dataDesastre <= :fim
            """)
    Page<CadastroFamilia> buscarDisponiveisRegional(@Param("regionalId") Long regionalId,
                                                    @Param("q") String q,
                                                    @Param("inicio") LocalDateTime inicio,
                                                    @Param("fim") LocalDateTime fim,
                                                    Pageable pageable);

    @Query("""
            select c from CadastroFamilia c
            where c.desastre is null
              and (:q = '' or lower(c.nomeAtingido) like lower(concat('%', :q, '%'))
                   or lower(c.cidadeAtingido) like lower(concat('%', :q, '%')))
              and c.dataDesastre >= :inicio
              and c.dataDesastre <= :fim
            """)
    Page<CadastroFamilia> buscarDisponiveisTodos(@Param("q") String q,
                                                 @Param("inicio") LocalDateTime inicio,
                                                 @Param("fim") LocalDateTime fim,
                                                 Pageable pageable);
}
