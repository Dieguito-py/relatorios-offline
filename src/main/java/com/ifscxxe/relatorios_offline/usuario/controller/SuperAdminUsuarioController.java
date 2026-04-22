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
import org.springframework.security.core.Authentication;
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
@PreAuthorize("hasAnyRole('REGIONAL','MASTER')")
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
    public String listarUsuarios(Authentication authentication, Model model) {
        Usuario usuarioLogado = obterUsuarioLogado(authentication);
        boolean isMaster = hasRole(authentication, "ROLE_MASTER");

        List<Usuario> usuarios = isMaster
                ? usuarioRepository.findAll()
                : usuarioRepository.findByRegionalId(usuarioLogado.getRegional() != null ? usuarioLogado.getRegional().getId() : -1L);
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
    public String novoUsuario(Authentication authentication, Model model) {
        Usuario usuario = new Usuario();
        model.addAttribute("usuario", usuario);
        preencherListas(model, authentication);
        model.addAttribute("pageTitle", "Novo Usuário");
        return "superadmin/usuarios/form";
    }

    @PostMapping
    public String salvarUsuario(@Valid @ModelAttribute("usuario") Usuario usuario,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model) {
        Usuario usuarioLogado = obterUsuarioLogado(authentication);
        boolean isMaster = hasRole(authentication, "ROLE_MASTER");

        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            bindingResult.rejectValue("username", "username.exists", "Nome de usuário já utilizado.");
        }

        if (usuario.getMunicipal() == null || usuario.getMunicipal().getId() == null) {
            bindingResult.rejectValue("municipal", "municipal.required", "Selecione um municipal.");
        }

        if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
            bindingResult.rejectValue("roles", "roles.required", "Selecione ao menos uma permissão.");
        }

        List<Role> rolesPermitidas = rolesPermitidasParaPerfil(isMaster);
        if (usuario.getRoles() != null && usuario.getRoles().stream().anyMatch(role -> !rolesPermitidas.contains(role))) {
            bindingResult.rejectValue("roles", "roles.invalid", "Você não pode atribuir uma ou mais permissões selecionadas.");
        }

        if (bindingResult.hasErrors()) {
            preencherListas(model, authentication);
            model.addAttribute("pageTitle", "Novo Usuário");
            return "superadmin/usuarios/form";
        }

        Municipal municipal = municipalRepository.findById(usuario.getMunicipal().getId())
                .orElseThrow(() -> new IllegalArgumentException("Municipal não encontrado"));

        if (!isMaster) {
            Long regionalDoLogado = usuarioLogado.getRegional() != null ? usuarioLogado.getRegional().getId() : null;
            Long regionalDoMunicipal = municipal.getRegional() != null ? municipal.getRegional().getId() : null;
            if (regionalDoLogado == null || !regionalDoLogado.equals(regionalDoMunicipal)) {
                bindingResult.rejectValue("municipal", "municipal.invalid", "Você só pode selecionar município da sua regional.");
                preencherListas(model, authentication);
                model.addAttribute("pageTitle", "Novo Usuário");
                return "superadmin/usuarios/form";
            }
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRoles(EnumSet.copyOf(usuario.getRoles()));
        usuario.setMunicipal(municipal);
        usuario.setRegional(municipal.getRegional());
        usuario.setAtivo(true);

        usuarioRepository.save(usuario);
        return "redirect:/superadmin/usuarios?created";
    }

    @PostMapping("/{id}/desativar")
    public String desativarUsuario(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioAlvo = usuarioRepository.findById(id)
                .orElse(null);
        if (usuarioAlvo == null) {
            return "redirect:/superadmin/usuarios?invalidUser";
        }

        Usuario usuarioLogado = obterUsuarioLogado(authentication);
        boolean isMaster = hasRole(authentication, "ROLE_MASTER");

        if (!isMaster) {
            Long regionalDoLogado = usuarioLogado.getRegional() != null ? usuarioLogado.getRegional().getId() : null;
            Long regionalDoAlvo = usuarioAlvo.getRegional() != null ? usuarioAlvo.getRegional().getId() : null;
            if (regionalDoLogado == null || !regionalDoLogado.equals(regionalDoAlvo)) {
                return "redirect:/superadmin/usuarios?forbidden";
            }
        }

        if (usuarioLogado.getId().equals(usuarioAlvo.getId())) {
            return "redirect:/superadmin/usuarios?cannotDeactivateSelf";
        }

        if (Boolean.FALSE.equals(usuarioAlvo.getAtivo())) {
            return "redirect:/superadmin/usuarios?alreadyInactive";
        }

        usuarioAlvo.setAtivo(false);
        usuarioRepository.save(usuarioAlvo);
        return "redirect:/superadmin/usuarios?deactivated";
    }

    @GetMapping("/{id}/relatorios")
    public String relatoriosPorUsuario(@PathVariable Long id, Authentication authentication, Model model) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        Usuario usuarioLogado = obterUsuarioLogado(authentication);
        boolean isMaster = hasRole(authentication, "ROLE_MASTER");

        if (!isMaster) {
            Long regionalDoLogado = usuarioLogado.getRegional() != null ? usuarioLogado.getRegional().getId() : null;
            Long regionalDoAlvo = usuario.getRegional() != null ? usuario.getRegional().getId() : null;
            if (regionalDoLogado == null || !regionalDoLogado.equals(regionalDoAlvo)) {
                return "redirect:/superadmin/usuarios?forbidden";
            }
        }

        List<CadastroFamilia> relatorios = cadastroFamiliaRepository.findByUsuarioIdOrderByDataDesastreDesc(id);
        model.addAttribute("usuario", usuario);
        model.addAttribute("relatorios", relatorios);
        model.addAttribute("pageTitle", "Relatórios de " + usuario.getNome());
        return "superadmin/usuarios/relatorios";
    }

    private void preencherListas(Model model, Authentication authentication) {
        Usuario usuarioLogado = obterUsuarioLogado(authentication);
        boolean isMaster = hasRole(authentication, "ROLE_MASTER");

        List<Municipal> municipais = isMaster
                ? municipalRepository.findAll()
                : municipalRepository.findByRegionalId(usuarioLogado.getRegional() != null ? usuarioLogado.getRegional().getId() : -1L);

        municipais.sort(Comparator.comparing(Municipal::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        model.addAttribute("municipais", municipais);
        model.addAttribute("rolesDisponiveis", rolesPermitidasParaPerfil(isMaster));
    }

    private Usuario obterUsuarioLogado(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Usuário não autenticado");
        }
        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário autenticado não encontrado"));
    }

    private boolean hasRole(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    private List<Role> rolesPermitidasParaPerfil(boolean isMaster) {
        if (isMaster) {
            return List.of(Role.values());
        }
        return List.of(Role.AGENTECAMPO, Role.MUNICIPAL);
    }
}
