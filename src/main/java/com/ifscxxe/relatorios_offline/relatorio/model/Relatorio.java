package com.ifscxxe.relatorios_offline.relatorio.model;

import com.ifscxxe.relatorios_offline.coordenadoria.model.CoordenadoriaMunicipal;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Relatorio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataDesastre;

    private String nomeAtingido;
    private String cpfAtingido;
    private String rgAtingido;
    private LocalDateTime dataNascimentoAtingido;
    private String enderecoAtingido;
    private String bairroAtingido;
    private String cidadeAtingido;
    private String complementoAtingido;

    private String localizacao;
    private String moradia;
    private String danoResidencia;
    private Double estimativaDanoMoveis;
    private Double estimativaDanoEdificacao;
    private String ocupacao;
    private String tipoConstrucao;
    private String alternativaMoradia;
    private String observacaoImovel;

    private Integer numeroTotalPessoas;
    private Integer menores0a12;
    private Integer menores13a17;
    private Integer maiores18a59;
    private Integer idosos60mais;
    private Boolean possuiNecessidadesEspeciais;
    private Integer quantidadeNecessidadesEspeciais;
    private String observacaoNecessidades;
    private Boolean usoMedicamentoContinuo;
    private String medicamento;
    private Boolean possuiDesaparecidos;
    private Integer quantidadeDesaparecidos;
    private Integer quantidadeFeridos;
    private Integer quantidadeObitos;

    private Integer qtdAguaPotavel5L;
    private Integer qtdColchoesSolteiro;
    private Integer qtdColchoesCasal;
    private Integer qtdCestasBasicas;
    private Integer qtdKitHigienePessoal;
    private Integer qtdKitLimpeza;
    private Integer qtdMoveis;
    private Integer qtdTelhas6mm;
    private Integer qtdTelhas4mm;
    private Integer qtdRoupas;
    private String outrasNecessidades;
    private String observacaoAssistencia;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "coordenadoria_municipal_id")
    private CoordenadoriaMunicipal coordenadoriaMunicipal;

    @PrePersist
    protected void onCreate() {
        this.dataDesastre = LocalDateTime.now();
    }
}
