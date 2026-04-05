package com.ifscxxe.relatorios_offline.usuario.controller;

import com.ifscxxe.relatorios_offline.coordenadoria.model.Municipal;
import com.ifscxxe.relatorios_offline.coordenadoria.repository.MunicipalRepository;
import com.ifscxxe.relatorios_offline.relatorio.model.CadastroFamilia;
import com.ifscxxe.relatorios_offline.relatorio.repository.CadastroFamiliaRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Role;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Controller
@RequestMapping("/superadmin/usuarios")
@PreAuthorize("hasRole('SUPERADMIN')")
public class SuperAdminUsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final CadastroFamiliaRepository cadastroFamiliaRepository;
    private final MunicipalRepository municipalRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminUsuarioController(UsuarioRepository usuarioRepository,
                                       CadastroFamiliaRepository cadastroFamiliaRepository,
                                       MunicipalRepository municipalRepository,
                                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cadastroFamiliaRepository = cadastroFamiliaRepository;
        this.municipalRepository = municipalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        usuarios.sort(Comparator
                .comparing((Usuario u) -> u.getRegional() != null ? u.getRegional().getNome() : null,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(u -> u.getMunicipal() != null ? u.getMunicipal().getNome() : null,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(u -> u.getNome() != null ? u.getNome() : u.getUsername(),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("pageTitle", "Gerenciar Usuários");
        return "superadmin/usuarios/lista";
    }

    @GetMapping("/novo")
    public String novoUsuario(Model model) {
        Usuario usuario = new Usuario();
        model.addAttribute("usuario", usuario);
        preencherListas(model);
        model.addAttribute("pageTitle", "Novo Usuário");
        return "superadmin/usuarios/form";
    }

    @PostMapping
    public String salvarUsuario(@Valid @ModelAttribute("usuario") Usuario usuario,
                                BindingResult bindingResult,
                                Model model) {
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            bindingResult.rejectValue("username", "username.exists", "Nome de usuário já utilizado.");
        }

        if (usuario.getMunicipal() == null || usuario.getMunicipal().getId() == null) {
            bindingResult.rejectValue("municipal", "municipal.required", "Selecione um municipal.");
        }

        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
            bindingResult.rejectValue("roles", "roles.required", "Selecione ao menos uma permissão.");
        }

        if (bindingResult.hasErrors()) {
            preencherListas(model);
            model.addAttribute("pageTitle", "Novo Usuário");
            return "superadmin/usuarios/form";
        }

        Municipal municipal = municipalRepository.findById(usuario.getMunicipal().getId())
                .orElseThrow(() -> new IllegalArgumentException("Municipal não encontrado"));

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRoles(EnumSet.copyOf(usuario.getRoles()));
        usuario.setMunicipal(municipal);
        usuario.setRegional(municipal.getRegional());

        usuarioRepository.save(usuario);
        return "redirect:/superadmin/usuarios?created";
    }

    @GetMapping("/{id}/relatorios")
    public String relatoriosPorUsuario(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        List<CadastroFamilia> relatorios = cadastroFamiliaRepository.findByUsuarioIdOrderByDataDesastreDesc(id);
        model.addAttribute("usuario", usuario);
        model.addAttribute("relatorios", relatorios);
        model.addAttribute("pageTitle", "Relatórios de " + usuario.getNome());
        return "superadmin/usuarios/relatorios";
    }

    private void preencherListas(Model model) {
        model.addAttribute("municipais", municipalRepository.findAll());
        model.addAttribute("rolesDisponiveis", Role.values());
    }
}
