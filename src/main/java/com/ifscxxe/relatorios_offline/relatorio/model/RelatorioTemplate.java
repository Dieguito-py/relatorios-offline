package com.ifscxxe.relatorios_offline.relatorio.model;

import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "relatorio_template")
public class RelatorioTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne(optional = false)
    @JoinColumn(name = "regional_id", nullable = false)
    private Regional regional;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC, id ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RelatorioTemplateCampo> campos = new ArrayList<>();

    public void addCampo(RelatorioTemplateCampo campo) {
        campo.setTemplate(this);
        campos.add(campo);
    }

    public void clearCampos() {
        campos.clear();
    }
}

