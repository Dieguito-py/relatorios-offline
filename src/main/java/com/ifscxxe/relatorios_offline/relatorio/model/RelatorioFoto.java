package com.ifscxxe.relatorios_offline.relatorio.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "relatorio_foto")
public class RelatorioFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chave", length = 120)
    private String chave;

    @Column(name = "nome_original")
    private String nomeOriginal;

    @Column(name = "nome_gerado")
    private String nomeGerado;

    @Column(name = "caminho", nullable = false, columnDefinition = "TEXT")
    private String caminho;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "tamanho")
    private Long tamanho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relatorio_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RelatorioDinamico relatorio;
}


