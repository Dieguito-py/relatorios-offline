package com.ifscxxe.relatorios_offline.relatorio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import com.ifscxxe.relatorios_offline.relatorio.dto.RelatorioTemplateForm;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioCampoTipo;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioTemplate;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioTemplateCampo;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioTemplateRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/regional/relatorios/templates")
@PreAuthorize("hasRole('REGIONAL')")
public class RelatorioTemplateController {

    private static final String TIPOS_COM_OPCOES = "SELECAO|MULTIPLA_SELECAO";

    private final RelatorioTemplateRepository templateRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public RelatorioTemplateController(RelatorioTemplateRepository templateRepository,
                                       UsuarioRepository usuarioRepository,
                                       ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        Regional regional = obterRegional(authentication);
        List<RelatorioTemplate> templates = templateRepository.findByRegionalIdOrderByNomeAsc(regional.getId());
        model.addAttribute("templates", templates);
        model.addAttribute("pageTitle", "Templates de relatorio");
        return "relatorio/template/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        RelatorioTemplateForm form = new RelatorioTemplateForm();
        if (form.getCampos().isEmpty()) {
            form.getCampos().add(new RelatorioTemplateForm.RelatorioTemplateCampoForm());
        }
        prepararFormulario(model, form, "Novo template de relatorio");
        return "relatorio/template/form";
    }

    @PostMapping
    public String criar(@ModelAttribute("templateForm") RelatorioTemplateForm form,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        Regional regional = obterRegional(authentication);
        List<String> erros = validar(form, regional.getId(), null);
        if (!erros.isEmpty()) {
            model.addAttribute("error", String.join(" ", erros));
            prepararFormulario(model, form, "Novo template de relatorio");
            return "relatorio/template/form";
        }

        RelatorioTemplate template = new RelatorioTemplate();
        preencherTemplate(template, form, regional);
        templateRepository.save(template);
        redirectAttributes.addFlashAttribute("message", "Template criado com sucesso.");
        return "redirect:/regional/relatorios/templates";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Authentication authentication, Model model) {
        Regional regional = obterRegional(authentication);
        RelatorioTemplate template = templateRepository.findByIdAndRegionalId(id, regional.getId())
                .orElse(null);
        if (template == null) {
            return "redirect:/regional/relatorios/templates?invalidTemplate";
        }
        RelatorioTemplateForm form = toForm(template);
        prepararFormulario(model, form, "Editar template de relatorio");
        return "relatorio/template/form";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @ModelAttribute("templateForm") RelatorioTemplateForm form,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Regional regional = obterRegional(authentication);
        RelatorioTemplate template = templateRepository.findByIdAndRegionalId(id, regional.getId())
                .orElse(null);
        if (template == null) {
            return "redirect:/regional/relatorios/templates?invalidTemplate";
        }

        List<String> erros = validar(form, regional.getId(), id);
        if (!erros.isEmpty()) {
            model.addAttribute("error", String.join(" ", erros));
            prepararFormulario(model, form, "Editar template de relatorio");
            return "relatorio/template/form";
        }

        preencherTemplate(template, form, regional);
        templateRepository.save(template);
        redirectAttributes.addFlashAttribute("message", "Template atualizado com sucesso.");
        return "redirect:/regional/relatorios/templates";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        Regional regional = obterRegional(authentication);
        RelatorioTemplate template = templateRepository.findByIdAndRegionalId(id, regional.getId())
                .orElse(null);
        if (template == null) {
            return "redirect:/regional/relatorios/templates?invalidTemplate";
        }
        templateRepository.delete(template);
        redirectAttributes.addFlashAttribute("message", "Template removido com sucesso.");
        return "redirect:/regional/relatorios/templates";
    }

    private void prepararFormulario(Model model, RelatorioTemplateForm form, String pageTitle) {
        if (form.getCampos() == null || form.getCampos().isEmpty()) {
            form.setCampos(new ArrayList<>());
            form.getCampos().add(new RelatorioTemplateForm.RelatorioTemplateCampoForm());
        }
        model.addAttribute("templateForm", form);
        model.addAttribute("tiposCampo", RelatorioCampoTipo.values());
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("tiposComOpcoes", TIPOS_COM_OPCOES);
    }

    private List<String> validar(RelatorioTemplateForm form, Long regionalId, Long templateId) {
        List<String> erros = new ArrayList<>();
        if (form == null || !StringUtils.hasText(form.getNome())) {
            erros.add("Informe um nome valido para o template.");
            return erros;
        }

        String nome = form.getNome().trim();
        Optional<RelatorioTemplate> existente = templateRepository.findByRegionalIdAndNomeIgnoreCase(regionalId, nome);
        if (existente.isPresent() && (templateId == null || !existente.get().getId().equals(templateId))) {
            erros.add("Ja existe um template com esse nome na regional.");
        }

        List<RelatorioTemplateForm.RelatorioTemplateCampoForm> campos = filtrarCamposValidos(form.getCampos());
        if (campos.isEmpty()) {
            erros.add("Adicione pelo menos um campo.");
            return erros;
        }

        Set<String> chaves = new HashSet<>();
        int idx = 0;
        for (RelatorioTemplateForm.RelatorioTemplateCampoForm campo : campos) {
            idx++;
            if (!StringUtils.hasText(campo.getChave())) {
                erros.add("Campo " + idx + ": informe a chave.");
                continue;
            }
            String chave = campo.getChave().trim();
            if (!chave.matches("[A-Za-z0-9_]+")) {
                erros.add("Campo " + idx + ": chave deve conter apenas letras, numeros ou _.");
            }
            if (!chaves.add(chave.toLowerCase())) {
                erros.add("Campo " + idx + ": chave duplicada.");
            }
            if (!StringUtils.hasText(campo.getRotulo())) {
                erros.add("Campo " + idx + ": informe o rotulo.");
            }
            if (!StringUtils.hasText(campo.getTipo())) {
                erros.add("Campo " + idx + ": selecione o tipo.");
            } else if (campo.getTipo().matches(TIPOS_COM_OPCOES) && !StringUtils.hasText(campo.getOpcoes())) {
                erros.add("Campo " + idx + ": informe as opcoes separadas por virgula.");
            }
        }

        return erros;
    }

    private List<RelatorioTemplateForm.RelatorioTemplateCampoForm> filtrarCamposValidos(
            List<RelatorioTemplateForm.RelatorioTemplateCampoForm> campos) {
        if (campos == null) {
            return List.of();
        }
        List<RelatorioTemplateForm.RelatorioTemplateCampoForm> filtrados = new ArrayList<>();
        for (RelatorioTemplateForm.RelatorioTemplateCampoForm campo : campos) {
            if (campo == null) {
                continue;
            }
            if (StringUtils.hasText(campo.getChave())
                    || StringUtils.hasText(campo.getRotulo())
                    || StringUtils.hasText(campo.getTipo())
                    || StringUtils.hasText(campo.getOpcoes())) {
                filtrados.add(campo);
            }
        }
        return filtrados;
    }

    private void preencherTemplate(RelatorioTemplate template, RelatorioTemplateForm form, Regional regional) {
        template.setNome(form.getNome().trim());
        template.setDescricao(StringUtils.hasText(form.getDescricao()) ? form.getDescricao().trim() : null);
        template.setAtivo(Boolean.TRUE.equals(form.getAtivo()));
        template.setRegional(regional);

        template.clearCampos();
        List<RelatorioTemplateForm.RelatorioTemplateCampoForm> campos = filtrarCamposValidos(form.getCampos());
        int ordem = 0;
        for (RelatorioTemplateForm.RelatorioTemplateCampoForm campoForm : campos) {
            RelatorioTemplateCampo campo = new RelatorioTemplateCampo();
            campo.setChave(campoForm.getChave().trim());
            campo.setRotulo(campoForm.getRotulo().trim());
            campo.setTipo(RelatorioCampoTipo.valueOf(campoForm.getTipo()));
            campo.setObrigatorio(Boolean.TRUE.equals(campoForm.getObrigatorio()));
            campo.setOrdem(campoForm.getOrdem() != null ? campoForm.getOrdem() : ++ordem);
            campo.setOpcoesJson(serializarOpcoes(campoForm));
            template.addCampo(campo);
        }
    }

    private String serializarOpcoes(RelatorioTemplateForm.RelatorioTemplateCampoForm campoForm) {
        if (!StringUtils.hasText(campoForm.getTipo())) {
            return null;
        }
        if (!campoForm.getTipo().matches(TIPOS_COM_OPCOES)) {
            return null;
        }
        if (!StringUtils.hasText(campoForm.getOpcoes())) {
            return null;
        }
        List<String> opcoes = new ArrayList<>();
        for (String opcao : campoForm.getOpcoes().split(",")) {
            String value = opcao.trim();
            if (!value.isEmpty()) {
                opcoes.add(value);
            }
        }
        if (opcoes.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(opcoes);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private RelatorioTemplateForm toForm(RelatorioTemplate template) {
        RelatorioTemplateForm form = new RelatorioTemplateForm();
        form.setId(template.getId());
        form.setNome(template.getNome());
        form.setDescricao(template.getDescricao());
        form.setAtivo(Boolean.TRUE.equals(template.getAtivo()));
        List<RelatorioTemplateForm.RelatorioTemplateCampoForm> campos = new ArrayList<>();
        template.getCampos().stream()
                .sorted(Comparator.comparing(RelatorioTemplateCampo::getOrdem, Comparator.nullsLast(Integer::compareTo)))
                .forEach(campo -> {
                    RelatorioTemplateForm.RelatorioTemplateCampoForm campoForm = new RelatorioTemplateForm.RelatorioTemplateCampoForm();
                    campoForm.setChave(campo.getChave());
                    campoForm.setRotulo(campo.getRotulo());
                    campoForm.setTipo(campo.getTipo() != null ? campo.getTipo().name() : null);
                    campoForm.setObrigatorio(Boolean.TRUE.equals(campo.getObrigatorio()));
                    campoForm.setOrdem(campo.getOrdem());
                    campoForm.setOpcoes(parseOpcoes(campo.getOpcoesJson()));
                    campos.add(campoForm);
                });
        form.setCampos(campos);
        return form;
    }

    private String parseOpcoes(String opcoesJson) {
        if (!StringUtils.hasText(opcoesJson)) {
            return null;
        }
        try {
            List<String> opcoes = objectMapper.readValue(opcoesJson, new TypeReference<List<String>>() {});
            return String.join(", ", opcoes);
        } catch (Exception ex) {
            return null;
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


