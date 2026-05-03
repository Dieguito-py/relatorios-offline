package com.ifscxxe.relatorios_offline.desastre.model;

import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioDinamico;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "desastre")
public class Desastre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_desastre", nullable = false, length = 40)
    private TipoDesastre tipoDesastre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(nullable = false)
    private LocalDateTime dataDesastre;

    @OneToMany(mappedBy = "desastre")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RelatorioDinamico> cadastrosFamilia = new ArrayList<>();
}

