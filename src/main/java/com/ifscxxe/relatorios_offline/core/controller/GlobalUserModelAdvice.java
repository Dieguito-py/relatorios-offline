package com.ifscxxe.relatorios_offline.core.controller;

import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(
        annotations = Controller.class,
        basePackages = {
                "com.ifscxxe.relatorios_offline.core.controller",
                "com.ifscxxe.relatorios_offline.desastre.controller",
                "com.ifscxxe.relatorios_offline.relatorio.controller",
                "com.ifscxxe.relatorios_offline.usuario.controller",
                "com.ifscxxe.relatorios_offline.coordenadoria.controller"
        }
)
public class GlobalUserModelAdvice {

    private final UsuarioRepository usuarioRepository;

    public GlobalUserModelAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("nome")
    public String populateNome(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        if (usuario != null && StringUtils.hasText(usuario.getNome())) {
            return usuario.getNome().trim();
        }

        return username;
    }
}

