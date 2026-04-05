package com.ifscxxe.relatorios_offline.core.controller;

import com.ifscxxe.relatorios_offline.usuario.model.Role;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Objects;

@Controller
public class homeController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            System.out.println("Usuário logado via web: " + authentication.getName());
        }
        if (authentication != null) {
            String username = authentication.getName();
            model.addAttribute("username", username);
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            model.addAttribute("nome", usuario != null ? usuario.getNome() : username);
            if (usuario != null) {
                String municipal = usuario.getMunicipal() != null
                        ? usuario.getMunicipal().getNome()
                        : null;
                String regional = usuario.getRegional() != null
                        ? usuario.getRegional().getNome()
                        : (usuario.getMunicipal() != null && usuario.getMunicipal().getRegional() != null
                        ? usuario.getMunicipal().getRegional().getNome()
                        : null);
                model.addAttribute("municipal", municipal);
                model.addAttribute("regional", regional);
            }
            model.addAttribute("roles", authentication.getAuthorities());
            model.addAttribute("roleNames", authentication.getAuthorities().stream()
                    .map(granted -> Role.fromString(granted.getAuthority()))
                    .filter(Objects::nonNull)
                    .map(Role::name)
                    .toList());
        }
        return "core/home";
    }

    @GetMapping("/home")
    public String homeAlias(Authentication authentication, Model model) {
        return home(authentication, model);
    }
}
