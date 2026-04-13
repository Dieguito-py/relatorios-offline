package com.ifscxxe.relatorios_offline.coordenadoria.controller;

import com.ifscxxe.relatorios_offline.coordenadoria.model.Municipal;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import com.ifscxxe.relatorios_offline.coordenadoria.repository.MunicipalRepository;
import com.ifscxxe.relatorios_offline.coordenadoria.repository.RegionalRepository;
import com.ifscxxe.relatorios_offline.relatorio.repository.CadastroFamiliaRepository;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/master/regionais")
@PreAuthorize("hasRole('MASTER')")
public class MasterRegionalController {

    private static final String DEFAULT_TEMPLATE_RELATORIO_FAMILIAS = "relatorioFamilias.jrxml";

    private final RegionalRepository regionalRepository;
    private final MunicipalRepository municipalRepository;
    private final UsuarioRepository usuarioRepository;
    private final CadastroFamiliaRepository cadastroFamiliaRepository;
    private final Path reportsBaseDir;

    public MasterRegionalController(RegionalRepository regionalRepository,
                                    MunicipalRepository municipalRepository,
                                    UsuarioRepository usuarioRepository,
                                    CadastroFamiliaRepository cadastroFamiliaRepository,
                                    @Value("${app.reports.base-dir}") String reportsBaseDir) {
        this.regionalRepository = regionalRepository;
        this.municipalRepository = municipalRepository;
        this.usuarioRepository = usuarioRepository;
        this.cadastroFamiliaRepository = cadastroFamiliaRepository;
        this.reportsBaseDir = Paths.get(reportsBaseDir).toAbsolutePath().normalize();
    }

    @GetMapping
    public String listar(
            @RequestParam(value = "qRegional", required = false) String qRegional,
            @RequestParam(value = "qMunicipal", required = false) String qMunicipal,
            @RequestParam(value = "municipalRegionalId", required = false) Long municipalRegionalId,
            @RequestParam(value = "regionalPage", defaultValue = "0") int regionalPage,
            @RequestParam(value = "municipalPage", defaultValue = "0") int municipalPage,
            Model model
    ) {
        String buscaRegional = qRegional != null ? qRegional.trim() : "";
        String buscaMunicipal = qMunicipal != null ? qMunicipal.trim() : "";

        Pageable regionalPageable = PageRequest.of(Math.max(0, regionalPage), 8, Sort.by("nome").ascending());
        Pageable municipalPageable = PageRequest.of(Math.max(0, municipalPage), 10, Sort.by("nome").ascending());

        Page<Regional> regionaisPage = regionalRepository.findByNomeContainingIgnoreCase(buscaRegional, regionalPageable);
        Page<Municipal> municipaisPage = municipalRegionalId != null
                ? municipalRepository.findByRegionalIdAndNomeContainingIgnoreCase(municipalRegionalId, buscaMunicipal, municipalPageable)
                : municipalRepository.findByNomeContainingIgnoreCase(buscaMunicipal, municipalPageable);

        List<Regional> regionaisParaSelect = regionalRepository.findAll();
        regionaisParaSelect.sort(Comparator.comparing(Regional::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        model.addAttribute("regionaisPage", regionaisPage);
        model.addAttribute("municipaisPage", municipaisPage);
        model.addAttribute("regionais", regionaisParaSelect);
        model.addAttribute("pageTitle", "Gerenciar Regionais");

        model.addAttribute("qRegional", buscaRegional);
        model.addAttribute("qMunicipal", buscaMunicipal);
        model.addAttribute("municipalRegionalId", municipalRegionalId);

        return "superadmin/regionais/lista";
    }

    @PostMapping
    public String criarRegional(@RequestParam("nome") String nome,
                                @RequestParam(value = "templateRelatorioFamilias", required = false) String templateRelatorioFamilias,
                                RedirectAttributes redirectAttributes) {
        if (nome == null || nome.trim().isEmpty()) {
            return "redirect:/master/regionais?invalidName";
        }

        Regional regional = new Regional();
        regional.setNome(nome.trim());
        try {
            regional.setTemplateRelatorioFamilias(normalizeTemplateRelatorioFamilias(templateRelatorioFamilias));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/master/regionais";
        }
        regionalRepository.save(regional);

        try {
            ensureRegionalReportsDirectory(regional.getId());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("warning", ex.getMessage());
        }

        return "redirect:/master/regionais?created";
    }

    @GetMapping("/{id}/editar")
    public String editarRegionalForm(@PathVariable Long id, Model model) {
        Regional regional = regionalRepository.findById(id)
                .orElse(null);
        if (regional == null) {
            return "redirect:/master/regionais?invalidRegional";
        }

        model.addAttribute("regional", regional);
        model.addAttribute("pageTitle", "Editar Regional");
        return "superadmin/regionais/editarRegional";
    }

    @PostMapping("/{id}/editar")
    public String editarRegional(@PathVariable Long id,
                                 @RequestParam("nome") String nome,
                                 @RequestParam(value = "templateRelatorioFamilias", required = false) String templateRelatorioFamilias,
                                 RedirectAttributes redirectAttributes) {
        Regional regional = regionalRepository.findById(id)
                .orElse(null);
        if (regional == null) {
            return "redirect:/master/regionais?invalidRegional";
        }
        if (nome == null || nome.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Informe um nome válido para a regional.");
            return "redirect:/master/regionais/" + id + "/editar";
        }

        regional.setNome(nome.trim());
        try {
            regional.setTemplateRelatorioFamilias(normalizeTemplateRelatorioFamilias(templateRelatorioFamilias));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/master/regionais/" + id + "/editar";
        }
        regionalRepository.save(regional);


        return "redirect:/master/regionais?updated";
    }

    @PostMapping("/municipais")
    public String criarMunicipal(@RequestParam("nome") String nome,
                                 @RequestParam("regionalId") Long regionalId) {
        if (nome == null || nome.trim().isEmpty()) {
            return "redirect:/master/regionais?invalidMunicipalName";
        }
        if (regionalId == null) {
            return "redirect:/master/regionais?invalidRegional";
        }

        Regional regional = regionalRepository.findById(regionalId)
                .orElse(null);
        if (regional == null) {
            return "redirect:/master/regionais?invalidRegional";
        }

        Municipal municipal = new Municipal();
        municipal.setNome(nome.trim());
        municipal.setRegional(regional);
        municipalRepository.save(municipal);

        return "redirect:/master/regionais?municipalCreated";
    }

    @GetMapping("/municipais/{id}/editar")
    public String editarMunicipalForm(@PathVariable Long id, Model model) {
        Municipal municipal = municipalRepository.findById(id)
                .orElse(null);
        if (municipal == null) {
            return "redirect:/master/regionais?invalidMunicipal";
        }

        List<Regional> regionais = regionalRepository.findAll();
        regionais.sort(Comparator.comparing(Regional::getNome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        model.addAttribute("municipal", municipal);
        model.addAttribute("regionais", regionais);
        model.addAttribute("pageTitle", "Editar Municipal");
        return "superadmin/regionais/editarMunicipal";
    }

    @PostMapping("/municipais/{id}/editar")
    public String editarMunicipal(@PathVariable Long id,
                                  @RequestParam("nome") String nome,
                                  @RequestParam("regionalId") Long regionalId,
                                  RedirectAttributes redirectAttributes) {
        Municipal municipal = municipalRepository.findById(id)
                .orElse(null);
        if (municipal == null) {
            return "redirect:/master/regionais?invalidMunicipal";
        }
        if (nome == null || nome.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Informe um nome válido para o municipal.");
            return "redirect:/master/regionais/municipais/" + id + "/editar";
        }

        Regional regional = regionalRepository.findById(regionalId)
                .orElse(null);
        if (regional == null) {
            redirectAttributes.addFlashAttribute("error", "Selecione uma regional válida.");
            return "redirect:/master/regionais/municipais/" + id + "/editar";
        }

        municipal.setNome(nome.trim());
        municipal.setRegional(regional);
        municipalRepository.save(municipal);

        return "redirect:/master/regionais?municipalUpdated";
    }

    @PostMapping("/municipais/{id}/excluir")
    public String excluirMunicipal(@PathVariable Long id) {
        Municipal municipal = municipalRepository.findById(id)
                .orElse(null);
        if (municipal == null) {
            return "redirect:/master/regionais?invalidMunicipal";
        }

        if (usuarioRepository.existsByMunicipalId(id) || cadastroFamiliaRepository.existsByMunicipalId(id)) {
            return "redirect:/master/regionais?municipalHasLinks";
        }

        municipalRepository.delete(municipal);
        return "redirect:/master/regionais?municipalDeleted";
    }

    @PostMapping("/{id}/excluir")
    public String excluirRegional(@PathVariable Long id) {
        Regional regional = regionalRepository.findById(id)
                .orElse(null);
        if (regional == null) {
            return "redirect:/master/regionais?invalidRegional";
        }

        if (municipalRepository.existsByRegionalId(id)
                || usuarioRepository.existsByRegionalId(id)
                || cadastroFamiliaRepository.existsByRegionalId(id)) {
            return "redirect:/master/regionais?regionalHasLinks";
        }

        regionalRepository.delete(regional);
        return "redirect:/master/regionais?regionalDeleted";
    }

    private String normalizeTemplateRelatorioFamilias(String rawPath) {
        String value = (rawPath == null || rawPath.trim().isEmpty())
                ? DEFAULT_TEMPLATE_RELATORIO_FAMILIAS
                : rawPath.trim();

        value = value.replace('\\', '/');
        if (value.startsWith("/") || value.matches("^[a-zA-Z]:.*")) {
            throw new IllegalArgumentException("Informe apenas caminho relativo para o template da regional.");
        }

        Path relative = Paths.get(value).normalize();
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("Informe apenas caminho relativo para o template da regional.");
        }

        for (Path part : relative) {
            if ("..".equals(part.toString())) {
                throw new IllegalArgumentException("O caminho do template da regional não pode conter '..'.");
            }
        }

        String normalized = relative.toString().replace('\\', '/');
        if (!normalized.toLowerCase(Locale.ROOT).endsWith(".jrxml")) {
            throw new IllegalArgumentException("O template da regional deve ter extensão .jrxml.");
        }

        return normalized;
    }

    private void ensureRegionalReportsDirectory(Long regionalId) {
        if (regionalId == null) {
            throw new IllegalStateException("Regional criada, mas não foi possível identificar o ID para criar pasta Jasper.");
        }

        Path regionalReportsDir = reportsBaseDir.resolve(String.valueOf(regionalId)).normalize();
        try {
            Files.createDirectories(regionalReportsDir);
        } catch (IOException | SecurityException ex) {
            throw new IllegalStateException(
                    "Regional criada, mas não foi possível criar a pasta Jasper em: " + regionalReportsDir,
                    ex
            );
        }
    }
}


