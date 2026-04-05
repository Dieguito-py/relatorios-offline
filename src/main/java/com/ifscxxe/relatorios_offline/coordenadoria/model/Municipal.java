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

    @ManyToOne
    @JoinColumn(name = "regional_id")
    private Regional regional;

    @OneToMany(mappedBy = "municipal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Usuario> usuarios;
}


