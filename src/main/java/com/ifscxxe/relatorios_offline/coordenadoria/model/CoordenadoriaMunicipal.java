package com.ifscxxe.relatorios_offline.coordenadoria.model;

import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class CoordenadoriaMunicipal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @ManyToOne
    @JoinColumn(name = "regional_id")
    private Regional regional;

    @OneToMany(mappedBy = "coordenadoriaMunicipal")
    private List<Usuario> usuarios;
}
