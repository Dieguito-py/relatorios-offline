package com.ifscxxe.relatorios_offline.desastre.model;

import com.ifscxxe.relatorios_offline.relatorio.model.CadastroFamilia;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
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

    @Column(name = "data_desastre_de", nullable = false)
    private LocalDate dataDesastreDe;

    @Column(name = "data_desastre_ate", nullable = false)
    private LocalDate dataDesastreAte;

    @OneToMany(mappedBy = "desastre")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CadastroFamilia> cadastrosFamilia = new ArrayList<>();
}

