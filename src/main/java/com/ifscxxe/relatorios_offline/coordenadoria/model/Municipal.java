package com.ifscxxe.relatorios_offline.coordenadoria.model;

import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "coordenadoria_municipal")
public class Municipal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String endereco;

    @Column(length = 20)
    private String cnpj;

    private String emailOrgaoProponente;

    @Column(length = 30)
    private String telefoneOrgaoProponente;

    private String emailCompdec;

    @Column(length = 30)
    private String telefoneCompdec;

    @Column(length = 30)
    private String celularCompdec;

    @ManyToOne
    private Usuario coordenadorCompdec;

    @ManyToOne
    @JoinColumn(name = "regional_id")
    private Regional regional;

    @OneToMany(mappedBy = "municipal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Usuario> usuarios;
}


