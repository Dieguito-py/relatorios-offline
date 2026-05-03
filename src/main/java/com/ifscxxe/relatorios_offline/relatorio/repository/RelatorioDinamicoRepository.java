package com.ifscxxe.relatorios_offline.relatorio.repository;

import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioDinamico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RelatorioDinamicoRepository extends JpaRepository<RelatorioDinamico, Long> {

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    List<RelatorioDinamico> findByMunicipalIdOrderByIdDesc(Long municipalId);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    List<RelatorioDinamico> findByMunicipalIdAndDataRegistroBetweenOrderByDataRegistroDesc(
            Long municipalId,
            LocalDateTime inicio,
            LocalDateTime fim
    );

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    List<RelatorioDinamico> findByRegionalIdOrderByIdDesc(Long regionalId);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    List<RelatorioDinamico> findByRegionalIdAndDataRegistroBetweenOrderByDataRegistroDesc(
            Long regionalId,
            LocalDateTime inicio,
            LocalDateTime fim
    );

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    List<RelatorioDinamico> findByDataRegistroBetweenOrderByDataRegistroDesc(LocalDateTime inicio, LocalDateTime fim);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    Optional<RelatorioDinamico> findByIdAndMunicipalId(Long id, Long municipalId);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    Optional<RelatorioDinamico> findByIdAndRegionalId(Long id, Long regionalId);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    List<RelatorioDinamico> findByUsuarioIdOrderByDataRegistroDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    List<RelatorioDinamico> findByDesastreIdOrderByDataRegistroDesc(Long desastreId);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    List<RelatorioDinamico> findByDesastreIdAndRegionalIdOrderByDataRegistroDesc(Long desastreId, Long regionalId);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    List<RelatorioDinamico> findByDesastreIdAndMunicipalIdOrderByDataRegistroDesc(Long desastreId, Long municipalId);

    boolean existsByRegionalId(Long regionalId);

    boolean existsByMunicipalId(Long municipalId);

    boolean existsByDesastreId(Long desastreId);

    @Query("""
            select r from RelatorioDinamico r
            where r.desastre is null
              and r.municipal.id = :municipalId
              and (:cidade = '' or lower(r.cidade) like lower(concat('%', :cidade, '%')))
              and r.dataRegistro >= :inicio
              and r.dataRegistro <= :fim
            """)
    Page<RelatorioDinamico> buscarDisponiveisMunicipal(@Param("municipalId") Long municipalId,
                                                     @Param("cidade") String cidade,
                                                     @Param("inicio") LocalDateTime inicio,
                                                     @Param("fim") LocalDateTime fim,
                                                     Pageable pageable);

    @Query("""
            select r from RelatorioDinamico r
            where r.desastre is null
              and r.regional.id = :regionalId
              and (:cidade = '' or lower(r.cidade) like lower(concat('%', :cidade, '%')))
              and r.dataRegistro >= :inicio
              and r.dataRegistro <= :fim
            """)
    Page<RelatorioDinamico> buscarDisponiveisRegional(@Param("regionalId") Long regionalId,
                                                    @Param("cidade") String cidade,
                                                    @Param("inicio") LocalDateTime inicio,
                                                    @Param("fim") LocalDateTime fim,
                                                    Pageable pageable);

    @Query("""
            select r from RelatorioDinamico r
            where r.desastre is null
              and (:cidade = '' or lower(r.cidade) like lower(concat('%', :cidade, '%')))
              and r.dataRegistro >= :inicio
              and r.dataRegistro <= :fim
            """)
    Page<RelatorioDinamico> buscarDisponiveisTodos(@Param("cidade") String cidade,
                                                 @Param("inicio") LocalDateTime inicio,
                                                 @Param("fim") LocalDateTime fim,
                                                 Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    @Query("""
            select r from RelatorioDinamico r
            where r.municipal.id = :municipalId
              and (:cidade = '' or lower(r.cidade) like lower(concat('%', :cidade, '%')))
              and r.dataRegistro >= :inicio
              and r.dataRegistro <= :fim
            order by r.dataRegistro desc
            """)
    List<RelatorioDinamico> buscarPorMunicipalFiltros(@Param("municipalId") Long municipalId,
                                                      @Param("cidade") String cidade,
                                                      @Param("inicio") LocalDateTime inicio,
                                                      @Param("fim") LocalDateTime fim);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    @Query("""
            select r from RelatorioDinamico r
            where r.regional.id = :regionalId
              and (:cidade = '' or lower(r.cidade) like lower(concat('%', :cidade, '%')))
              and r.dataRegistro >= :inicio
              and r.dataRegistro <= :fim
            order by r.dataRegistro desc
            """)
    List<RelatorioDinamico> buscarPorRegionalFiltros(@Param("regionalId") Long regionalId,
                                                     @Param("cidade") String cidade,
                                                     @Param("inicio") LocalDateTime inicio,
                                                     @Param("fim") LocalDateTime fim);

    @EntityGraph(attributePaths = {"usuario", "municipal", "regional", "template", "desastre"})
    @Query("""
            select r from RelatorioDinamico r
            where (:regionalId is null or r.regional.id = :regionalId)
              and (:municipalId is null or r.municipal.id = :municipalId)
              and (:cidade = '' or lower(r.cidade) like lower(concat('%', :cidade, '%')))
              and r.dataRegistro >= :inicio
              and r.dataRegistro <= :fim
            order by r.dataRegistro desc
            """)
    List<RelatorioDinamico> buscarPorFiltros(@Param("regionalId") Long regionalId,
                                             @Param("municipalId") Long municipalId,
                                             @Param("cidade") String cidade,
                                             @Param("inicio") LocalDateTime inicio,
                                             @Param("fim") LocalDateTime fim);
}
