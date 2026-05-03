package com.ifscxxe.relatorios_offline.coordenadoria.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Regional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @OneToMany(mappedBy = "regional", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Municipal> municipais;
}
