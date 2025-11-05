package com.ifscxxe.relatorios_offline.usuario.model;

import com.ifscxxe.relatorios_offline.coordenadoria.model.CoordenadoriaMunicipal;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import com.ifscxxe.relatorios_offline.relatorio.model.Relatorio;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "usuarios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_usuario_username", columnNames = "username")
})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 200)
    private String password;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Set<Role> roles = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "coordenadoria_municipal_id")
    private CoordenadoriaMunicipal coordenadoriaMunicipal;
    @ManyToOne
    @JoinColumn(name = "regional_id")
    private Regional regional;

    @OneToMany(mappedBy = "usuario")
    private List<Relatorio> relatorios;
}
