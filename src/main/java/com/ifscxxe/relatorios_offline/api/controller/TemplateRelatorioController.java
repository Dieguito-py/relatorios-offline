package com.ifscxxe.relatorios_offline.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifscxxe.relatorios_offline.api.dto.relatorio.template.TemplateCampoResponse;
import com.ifscxxe.relatorios_offline.api.dto.relatorio.template.TemplateResponse;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioTemplate;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioTemplateCampo;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioTemplateRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/templates")
public class TemplateRelatorioController {

    private final RelatorioTemplateRepository templateRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public TemplateRelatorioController(RelatorioTemplateRepository templateRepository,
                                       UsuarioRepository usuarioRepository,
                                       ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<?> listar(Authentication authentication) {
        Regional regional = obterRegional(authentication);
        List<TemplateResponse> templates = templateRepository.findByRegionalIdOrderByNomeAsc(regional.getId()).stream()
                .filter(RelatorioTemplate::getAtivo)
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalhar(@PathVariable Long id, Authentication authentication) {
        Regional regional = obterRegional(authentication);
        Optional<RelatorioTemplate> templateOpt = templateRepository.findByIdAndRegionalId(id, regional.getId());
        if (templateOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Template nao encontrado"));
        }
        RelatorioTemplate template = templateOpt.get();
        if (!Boolean.TRUE.equals(template.getAtivo())) {
            return ResponseEntity.status(404).body(Map.of("error", "Template inativo"));
        }
        return ResponseEntity.ok(toResponse(template));
    }

    private TemplateResponse toResponse(RelatorioTemplate template) {
        List<TemplateCampoResponse> campos = template.getCampos().stream()
                .map(this::toCampoResponse)
                .collect(Collectors.toList());
        return new TemplateResponse(
                template.getId(),
                template.getRegional() != null ? template.getRegional().getId() : null,
                template.getNome(),
                template.getDescricao(),
                Boolean.TRUE.equals(template.getAtivo()),
                campos
        );
    }

    private TemplateCampoResponse toCampoResponse(RelatorioTemplateCampo campo) {
        return new TemplateCampoResponse(
                campo.getChave(),
                campo.getRotulo(),
                campo.getTipo() != null ? campo.getTipo().name() : null,
                Boolean.TRUE.equals(campo.getObrigatorio()),
                campo.getOrdem(),
                parseOpcoes(campo.getOpcoesJson())
        );
    }

    private List<String> parseOpcoes(String opcoesJson) {
        if (!StringUtils.hasText(opcoesJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(opcoesJson, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private Regional obterRegional(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Usuario nao autenticado");
        }
        Usuario usuario = usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado nao encontrado"));

        if (usuario.getRegional() != null) {
            return usuario.getRegional();
        }
        if (usuario.getMunicipal() != null && usuario.getMunicipal().getRegional() != null) {
            return usuario.getMunicipal().getRegional();
        }
        throw new IllegalArgumentException("Regional nao encontrada para o usuario autenticado");
    }
}

