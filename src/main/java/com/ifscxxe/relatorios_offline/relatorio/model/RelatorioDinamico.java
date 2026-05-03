package com.ifscxxe.relatorios_offline.relatorio.model;

import com.ifscxxe.relatorios_offline.coordenadoria.model.Municipal;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import com.ifscxxe.relatorios_offline.desastre.model.Desastre;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "relatorio_dinamico")
public class RelatorioDinamico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_registro", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @Column(nullable = false, length = 120)
    private String cidade;

    @Column(name = "dados_json", columnDefinition = "text")
    private String dadosJson;

    @ManyToOne(optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private RelatorioTemplate template;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "coordenadoria_municipal_id", nullable = false)
    private Municipal municipal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "regional_id", nullable = false)
    private Regional regional;

    @ManyToOne
    @JoinColumn(name = "desastre_id")
    private Desastre desastre;

    @PrePersist
    protected void onCreate() {
        if (dataRegistro == null) {
            dataRegistro = LocalDateTime.now();
        }
    }
}

