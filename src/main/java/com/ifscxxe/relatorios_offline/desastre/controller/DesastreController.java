package com.ifscxxe.relatorios_offline.desastre.controller;

import com.ifscxxe.relatorios_offline.desastre.model.Desastre;
import com.ifscxxe.relatorios_offline.desastre.model.TipoDesastre;
import com.ifscxxe.relatorios_offline.desastre.repository.DesastreRepository;
import com.ifscxxe.relatorios_offline.relatorio.model.CadastroFamilia;
import com.ifscxxe.relatorios_offline.relatorio.repository.CadastroFamiliaRepository;
import com.ifscxxe.relatorios_offline.relatorio.service.JasperReportService;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Controller
@RequestMapping("/desastres")
@PreAuthorize("hasAnyRole('MUNICIPAL','REGIONAL','MASTER')")
public class DesastreController {

    private final DesastreRepository desastreRepository;
    private final CadastroFamiliaRepository cadastroFamiliaRepository;
    private final UsuarioRepository usuarioRepository;
    private final JasperReportService jasperReportService;

    public DesastreController(DesastreRepository desastreRepository,
                              CadastroFamiliaRepository cadastroFamiliaRepository,
                              UsuarioRepository usuarioRepository,
                              JasperReportService jasperReportService) {
        this.desastreRepository = desastreRepository;
        this.cadastroFamiliaRepository = cadastroFamiliaRepository;
        this.usuarioRepository = usuarioRepository;
        this.jasperReportService = jasperReportService;
    }

    @GetMapping
    public String listar(Model model) {
        List<Desastre> desastres = desastreRepository.findAllByOrderByDataDesastreDescIdDesc();
        model.addAttribute("desastres", desastres);
        model.addAttribute("pageTitle", "Cadastro de Desastres");
        return "desastre/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("desastre", new Desastre());
        model.addAttribute("tiposDesastre", TipoDesastre.values());
        model.addAttribute("formAction", "/desastres/novo");
        model.addAttribute("pageTitle", "Novo Desastre");
        return "desastre/form";
    }

    @PostMapping("/novo")
    public String criar(@ModelAttribute("desastre") Desastre desastre, Model model) {
        if (temDadosInvalidos(desastre, model)) {
            model.addAttribute("tiposDesastre", TipoDesastre.values());
            model.addAttribute("formAction", "/desastres/novo");
            model.addAttribute("pageTitle", "Novo Desastre");
            return "desastre/form";
        }

        desastreRepository.save(desastre);
        return "redirect:/desastres?created";
    }

    @GetMapping("/{id}")
    public String detalhar(@PathVariable Long id, Authentication authentication, Model model) {
        Desastre desastre = desastreRepository.findById(id).orElse(null);
        if (desastre == null) {
            return "redirect:/desastres?invalidDesastre";
        }

        List<CadastroFamilia> relatoriosVinculados = obterRelatoriosVinculados(id, authentication);

        model.addAttribute("desastre", desastre);
        model.addAttribute("relatoriosVinculados", relatoriosVinculados);
        model.addAttribute("pageTitle", "Detalhes do Desastre");
        return "desastre/detalhe";
    }

    @GetMapping("/{id}/exportar-familias")
    public void exportarFamiliasPorDesastre(@PathVariable Long id,
                                            Authentication authentication,
                                            HttpServletResponse response) throws IOException {
        Desastre desastre = desastreRepository.findById(id).orElse(null);
        if (desastre == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Desastre não encontrado.");
        }

        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não autenticado.");
        }

        Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
        String usuarioGerador = resolveUsuarioGerador(authentication, usuario);
        List<CadastroFamilia> relatorios;
        Long regionalId;
        String templateRelatorioFamilias;

        if (hasAuthority(authentication, "ROLE_MUNICIPAL")) {
            if (usuario == null || usuario.getMunicipal() == null || usuario.getMunicipal().getRegional() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível identificar a regional do usuário municipal.");
            }
            regionalId = usuario.getMunicipal().getRegional().getId();
            templateRelatorioFamilias = usuario.getMunicipal().getRegional().getTemplateRelatorioFamilias();
            relatorios = cadastroFamiliaRepository.findByDesastreIdAndMunicipalIdOrderByDataDesastreDesc(id, usuario.getMunicipal().getId());
        } else if (hasAuthority(authentication, "ROLE_REGIONAL")) {
            if (usuario == null || usuario.getRegional() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível identificar a regional do usuário.");
            }
            regionalId = usuario.getRegional().getId();
            templateRelatorioFamilias = usuario.getRegional().getTemplateRelatorioFamilias();
            relatorios = cadastroFamiliaRepository.findByDesastreIdAndRegionalIdOrderByDataDesastreDesc(id, regionalId);
        } else {
            relatorios = cadastroFamiliaRepository.findByDesastreIdOrderByDataDesastreDesc(id);
            if (relatorios.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não há relatórios vinculados a este desastre para exportar.");
            }

            Set<Long> regionaisEncontradas = new TreeSet<>();
            for (CadastroFamilia relatorio : relatorios) {
                if (relatorio.getRegional() != null && relatorio.getRegional().getId() != null) {
                    regionaisEncontradas.add(relatorio.getRegional().getId());
                }
            }

            if (regionaisEncontradas.size() != 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Para exportação MASTER, os relatórios vinculados devem pertencer a uma única regional."
                );
            }

            regionalId = regionaisEncontradas.iterator().next();
            templateRelatorioFamilias = null;
            for (CadastroFamilia relatorio : relatorios) {
                if (relatorio.getRegional() != null
                        && regionalId.equals(relatorio.getRegional().getId())) {
                    templateRelatorioFamilias = relatorio.getRegional().getTemplateRelatorioFamilias();
                    break;
                }
            }
        }

        if (relatorios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não há relatórios vinculados a este desastre para exportar.");
        }

        try {
            Map<String, Object> parametrosAdicionais = new HashMap<>();
            parametrosAdicionais.put("DESASTRE_ID", desastre.getId());
            parametrosAdicionais.put("DESASTRE_DESCRICAO", desastre.getDescricao());
            parametrosAdicionais.put("DESASTRE_TIPO", desastre.getTipoDesastre() != null ? desastre.getTipoDesastre().getDescricao() : null);
            parametrosAdicionais.put("DESASTRE_DATA", desastre.getDataDesastre());
            parametrosAdicionais.put("FILTRO_INICIO", desastre.getDataDesastre().toLocalDate());
            parametrosAdicionais.put("FILTRO_FIM", desastre.getDataDesastre().toLocalDate());
            parametrosAdicionais.put("USUARIO_GERADOR", usuarioGerador);

            byte[] pdf = jasperReportService.gerarRelatorioPdf(
                    regionalId,
                    templateRelatorioFamilias,
                    relatorios,
                    parametrosAdicionais
            );

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=familias_atingidas_desastre_" + id + ".pdf");
            response.getOutputStream().write(pdf);
            response.getOutputStream().flush();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao gerar PDF do relatório do desastre. Detalhe: " + ex.getMessage(),
                    ex
            );
        }
    }

    @GetMapping("/{id}/vinculos")
    public String gerenciarVinculos(@PathVariable Long id,
                                    Authentication authentication,
                                    @RequestParam(value = "q", required = false) String q,
                                    @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                    @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                    @RequestParam(value = "page", defaultValue = "0") int page,
                                    Model model) {
        Desastre desastre = desastreRepository.findById(id).orElse(null);
        if (desastre == null) {
            return "redirect:/desastres?invalidDesastre";
        }

        List<CadastroFamilia> relatoriosVinculados = obterRelatoriosVinculados(id, authentication);
        Pageable pageable = PageRequest.of(Math.max(page, 0), 10, Sort.by(Sort.Direction.DESC, "dataDesastre", "id"));
        Page<CadastroFamilia> relatoriosDisponiveisPage = obterRelatoriosDisponiveis(authentication, q, inicio, fim, pageable);

        model.addAttribute("desastre", desastre);
        model.addAttribute("relatoriosVinculados", relatoriosVinculados);
        model.addAttribute("relatoriosDisponiveisPage", relatoriosDisponiveisPage);
        model.addAttribute("q", q);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("pageTitle", "Gerenciar Vínculos de Relatórios");
        return "desastre/vinculos";
    }

    @PostMapping("/{id}/vincular")
    public String vincularRelatorio(@PathVariable Long id,
                                    @RequestParam(name = "relatorioIds", required = false) List<Long> relatorioIds,
                                    Authentication authentication) {
        Desastre desastre = desastreRepository.findById(id).orElse(null);
        if (desastre == null) {
            return "redirect:/desastres?invalidDesastre";
        }

        if (relatorioIds == null || relatorioIds.isEmpty()) {
            return "redirect:/desastres/" + id + "/vinculos?noneSelected";
        }

        int vinculados = 0;
        for (Long relatorioId : relatorioIds) {
            Optional<CadastroFamilia> relatorioOpt = obterRelatorioPorEscopo(relatorioId, authentication);
            if (relatorioOpt.isEmpty()) {
                continue;
            }
            CadastroFamilia relatorio = relatorioOpt.get();
            relatorio.setDesastre(desastre);
            cadastroFamiliaRepository.save(relatorio);
            vinculados++;
        }

        if (vinculados == 0) {
            return "redirect:/desastres/" + id + "/vinculos?relatorioInvalido";
        }
        return "redirect:/desastres/" + id + "/vinculos?linked";
    }

    @PostMapping("/{id}/desvincular")
    public String desvincularRelatorio(@PathVariable Long id,
                                       @RequestParam("relatorioId") Long relatorioId,
                                       Authentication authentication) {
        Optional<CadastroFamilia> relatorioOpt = obterRelatorioPorEscopo(relatorioId, authentication);
        if (relatorioOpt.isEmpty()) {
            return "redirect:/desastres/" + id + "/vinculos?relatorioInvalido";
        }

        CadastroFamilia relatorio = relatorioOpt.get();
        if (relatorio.getDesastre() == null || !id.equals(relatorio.getDesastre().getId())) {
            return "redirect:/desastres/" + id + "/vinculos?relatorioInvalido";
        }

        relatorio.setDesastre(null);
        cadastroFamiliaRepository.save(relatorio);
        return "redirect:/desastres/" + id + "/vinculos?unlinked";
    }

    @PostMapping("/{id}/desvincular-lote")
    public String desvincularRelatorios(@PathVariable Long id,
                                        @RequestParam(name = "relatorioIds", required = false) List<Long> relatorioIds,
                                        Authentication authentication) {
        if (relatorioIds == null || relatorioIds.isEmpty()) {
            return "redirect:/desastres/" + id + "/vinculos?noneSelectedUnlink";
        }

        int desvinculados = 0;
        for (Long relatorioId : relatorioIds) {
            Optional<CadastroFamilia> relatorioOpt = obterRelatorioPorEscopo(relatorioId, authentication);
            if (relatorioOpt.isEmpty()) {
                continue;
            }

            CadastroFamilia relatorio = relatorioOpt.get();
            if (relatorio.getDesastre() != null && id.equals(relatorio.getDesastre().getId())) {
                relatorio.setDesastre(null);
                cadastroFamiliaRepository.save(relatorio);
                desvinculados++;
            }
        }

        if (desvinculados == 0) {
            return "redirect:/desastres/" + id + "/vinculos?relatorioInvalido";
        }

        return "redirect:/desastres/" + id + "/vinculos?unlinked";
    }

    @GetMapping("/{id}/editar")
    public String editarForm(@PathVariable Long id, Authentication authentication, Model model) {
        Desastre desastre = desastreRepository.findById(id).orElse(null);
        if (desastre == null) {
            return "redirect:/desastres?invalidDesastre";
        }

        List<CadastroFamilia> relatoriosVinculados = obterRelatoriosVinculados(id, authentication);

        model.addAttribute("desastre", desastre);
        model.addAttribute("tiposDesastre", TipoDesastre.values());
        model.addAttribute("relatoriosVinculados", relatoriosVinculados);
        model.addAttribute("formAction", "/desastres/" + id + "/editar");
        model.addAttribute("pageTitle", "Editar Desastre");
        return "desastre/form";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id, @ModelAttribute("desastre") Desastre form, Authentication authentication, Model model) {
        Desastre desastre = desastreRepository.findById(id).orElse(null);
        if (desastre == null) {
            return "redirect:/desastres?invalidDesastre";
        }

        if (temDadosInvalidos(form, model)) {
            model.addAttribute("tiposDesastre", TipoDesastre.values());
            model.addAttribute("relatoriosVinculados", obterRelatoriosVinculados(id, authentication));
            model.addAttribute("formAction", "/desastres/" + id + "/editar");
            model.addAttribute("pageTitle", "Editar Desastre");
            return "desastre/form";
        }

        desastre.setTipoDesastre(form.getTipoDesastre());
        desastre.setDescricao(form.getDescricao().trim());
        desastre.setDataDesastre(form.getDataDesastre());
        desastreRepository.save(desastre);

        return "redirect:/desastres?updated";
    }

    private boolean temDadosInvalidos(Desastre desastre, Model model) {
        if (desastre.getTipoDesastre() == null) {
            model.addAttribute("error", "Selecione o tipo de desastre.");
            return true;
        }
        if (!StringUtils.hasText(desastre.getDescricao())) {
            model.addAttribute("error", "Informe a descrição do desastre.");
            return true;
        }
        if (desastre.getDataDesastre() == null) {
            model.addAttribute("error", "Informe a data e hora do desastre.");
            return true;
        }
        return false;
    }

    private List<CadastroFamilia> obterRelatoriosVinculados(Long desastreId, Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }

        if (hasAuthority(authentication, "ROLE_MUNICIPAL")) {
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
            if (usuario != null && usuario.getMunicipal() != null) {
                return cadastroFamiliaRepository.findByDesastreIdAndMunicipalIdOrderByDataDesastreDesc(
                        desastreId,
                        usuario.getMunicipal().getId()
                );
            }
            return List.of();
        }
        return cadastroFamiliaRepository.findByDesastreIdOrderByDataDesastreDesc(desastreId);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private String resolveUsuarioGerador(Authentication authentication, Usuario usuario) {
        if (usuario != null && StringUtils.hasText(usuario.getNome())) {
            return usuario.getNome().trim();
        }
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName();
        }
        return "sistema";
    }

    private Page<CadastroFamilia> obterRelatoriosDisponiveis(Authentication authentication,
                                                              String q,
                                                              LocalDate inicio,
                                                              LocalDate fim,
                                                              Pageable pageable) {
        if (authentication == null) {
            return Page.empty(pageable);
        }

        String filtro = StringUtils.hasText(q) ? q.trim() : "";
        LocalDateTime inicioDateTime = inicio != null
                ? inicio.atStartOfDay()
                : LocalDate.of(1970, 1, 1).atStartOfDay();
        LocalDateTime fimDateTime = fim != null
                ? fim.atTime(LocalTime.MAX)
                : LocalDate.of(9999, 12, 31).atTime(LocalTime.MAX);

        if (hasAuthority(authentication, "ROLE_MUNICIPAL")) {
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
            if (usuario != null && usuario.getMunicipal() != null) {
                return cadastroFamiliaRepository.buscarDisponiveisMunicipal(
                        usuario.getMunicipal().getId(),
                        filtro,
                        inicioDateTime,
                        fimDateTime,
                        pageable
                );
            }
            return Page.empty(pageable);
        }

        if (hasAuthority(authentication, "ROLE_REGIONAL")) {
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
            if (usuario != null && usuario.getRegional() != null) {
                return cadastroFamiliaRepository.buscarDisponiveisRegional(
                        usuario.getRegional().getId(),
                        filtro,
                        inicioDateTime,
                        fimDateTime,
                        pageable
                );
            }
            return Page.empty(pageable);
        }

        return cadastroFamiliaRepository.buscarDisponiveisTodos(filtro, inicioDateTime, fimDateTime, pageable);
    }

    private Optional<CadastroFamilia> obterRelatorioPorEscopo(Long relatorioId, Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }

        if (hasAuthority(authentication, "ROLE_MUNICIPAL")) {
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
            if (usuario != null && usuario.getMunicipal() != null) {
                return cadastroFamiliaRepository.findByIdAndMunicipalId(relatorioId, usuario.getMunicipal().getId());
            }
            return Optional.empty();
        }

        if (hasAuthority(authentication, "ROLE_REGIONAL")) {
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
            if (usuario != null && usuario.getRegional() != null) {
                return cadastroFamiliaRepository.findByIdAndRegionalId(relatorioId, usuario.getRegional().getId());
            }
            return Optional.empty();
        }

        return cadastroFamiliaRepository.findById(relatorioId);
    }
}
