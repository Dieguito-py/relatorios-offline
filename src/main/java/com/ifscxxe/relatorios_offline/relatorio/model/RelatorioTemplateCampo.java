package com.ifscxxe.relatorios_offline.relatorio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "relatorio_template_campo")
public class RelatorioTemplateCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RelatorioTemplate template;

    @Column(nullable = false, length = 100)
    private String chave;

    @Column(nullable = false, length = 150)
    private String rotulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RelatorioCampoTipo tipo;

    @Column(nullable = false)
    private Boolean obrigatorio = false;

    @Column(nullable = false)
    private Integer ordem = 0;

    @Column(name = "opcoes_json", columnDefinition = "text")
    private String opcoesJson;
}

