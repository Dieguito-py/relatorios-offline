package com.ifscxxe.relatorios_offline.relatorio.model;

import com.ifscxxe.relatorios_offline.coordenadoria.model.Municipal;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import com.ifscxxe.relatorios_offline.core.storage.StoredFileMetadata;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "relatorio")
public class CadastroFamilia {
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

    private String latitude;
    private String longitude;

    @OneToMany(mappedBy = "relatorio", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RelatorioFoto> fotosResidencia = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "coordenadoria_municipal_id")
    private Municipal municipal;

    @ManyToOne
    @JoinColumn(name = "regional_id")
    private Regional regional;

    @PrePersist
    protected void onCreate() {
        this.dataDesastre = LocalDateTime.now();
    }

    public void addFotoResidencia(StoredFileMetadata arquivo) {
        RelatorioFoto foto = new RelatorioFoto();
        foto.setNomeOriginal(arquivo.nomeOriginal());
        foto.setNomeGerado(arquivo.nomeGerado());
        foto.setCaminho(arquivo.caminho());
        foto.setContentType(arquivo.contentType());
        foto.setTamanho(arquivo.tamanho());
        foto.setRelatorio(this);
        fotosResidencia.add(foto);
    }
}



