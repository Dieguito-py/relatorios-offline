package com.ifscxxe.relatorios_offline.coordenadoria.controller;

import com.ifscxxe.relatorios_offline.coordenadoria.model.Municipal;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import com.ifscxxe.relatorios_offline.coordenadoria.repository.MunicipalRepository;
import com.ifscxxe.relatorios_offline.coordenadoria.repository.RegionalRepository;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioDinamicoRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
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

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/master/regionais")
@PreAuthorize("hasRole('MASTER')")
public class MasterRegionalController {

    private final RegionalRepository regionalRepository;
    private final MunicipalRepository municipalRepository;
    private final UsuarioRepository usuarioRepository;
    private final RelatorioDinamicoRepository relatorioDinamicoRepository;

    public MasterRegionalController(RegionalRepository regionalRepository,
                                    MunicipalRepository municipalRepository,
                                    UsuarioRepository usuarioRepository,
                                    RelatorioDinamicoRepository relatorioDinamicoRepository) {
        this.regionalRepository = regionalRepository;
        this.municipalRepository = municipalRepository;
        this.usuarioRepository = usuarioRepository;
        this.relatorioDinamicoRepository = relatorioDinamicoRepository;
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
                                RedirectAttributes redirectAttributes) {
        if (nome == null || nome.trim().isEmpty()) {
            return "redirect:/master/regionais?invalidName";
        }

        Regional regional = new Regional();
        regional.setNome(nome.trim());
        regionalRepository.save(regional);

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
        regionalRepository.save(regional);

        return "redirect:/master/regionais?updated";
    }

    @PostMapping("/municipais")
    public String criarMunicipal(@RequestParam("nome") String nome,
                                 @RequestParam("regionalId") Long regionalId,
                                 @RequestParam(value = "endereco", required = false) String endereco,
                                 @RequestParam(value = "cnpj", required = false) String cnpj,
                                 @RequestParam(value = "emailOrgaoProponente", required = false) String emailOrgaoProponente,
                                 @RequestParam(value = "telefoneOrgaoProponente", required = false) String telefoneOrgaoProponente,
                                 @RequestParam(value = "emailCompdec", required = false) String emailCompdec,
                                 @RequestParam(value = "telefoneCompdec", required = false) String telefoneCompdec,
                                 @RequestParam(value = "celularCompdec", required = false) String celularCompdec) {
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
        municipal.setEndereco(normalizeOptional(endereco));
        municipal.setCnpj(normalizeOptional(cnpj));
        municipal.setEmailOrgaoProponente(normalizeOptional(emailOrgaoProponente));
        municipal.setTelefoneOrgaoProponente(normalizeOptional(telefoneOrgaoProponente));
        municipal.setEmailCompdec(normalizeOptional(emailCompdec));
        municipal.setTelefoneCompdec(normalizeOptional(telefoneCompdec));
        municipal.setCelularCompdec(normalizeOptional(celularCompdec));
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
        List<Usuario> usuariosMunicipais = usuarioRepository.findByMunicipalIdOrderByNomeAscUsernameAsc(id);

        model.addAttribute("municipal", municipal);
        model.addAttribute("regionais", regionais);
        model.addAttribute("usuariosMunicipais", usuariosMunicipais);
        model.addAttribute("pageTitle", "Editar Municipal");
        return "superadmin/regionais/editarMunicipal";
    }

    @PostMapping("/municipais/{id}/editar")
    public String editarMunicipal(@PathVariable Long id,
                                  @RequestParam("nome") String nome,
                                  @RequestParam("regionalId") Long regionalId,
                                  @RequestParam(value = "endereco", required = false) String endereco,
                                  @RequestParam(value = "cnpj", required = false) String cnpj,
                                  @RequestParam(value = "emailOrgaoProponente", required = false) String emailOrgaoProponente,
                                  @RequestParam(value = "telefoneOrgaoProponente", required = false) String telefoneOrgaoProponente,
                                  @RequestParam(value = "emailCompdec", required = false) String emailCompdec,
                                  @RequestParam(value = "telefoneCompdec", required = false) String telefoneCompdec,
                                  @RequestParam(value = "celularCompdec", required = false) String celularCompdec,
                                  @RequestParam(value = "coordenadorCompdecId", required = false) Long coordenadorCompdecId,
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
        municipal.setEndereco(normalizeOptional(endereco));
        municipal.setCnpj(normalizeOptional(cnpj));
        municipal.setEmailOrgaoProponente(normalizeOptional(emailOrgaoProponente));
        municipal.setTelefoneOrgaoProponente(normalizeOptional(telefoneOrgaoProponente));
        municipal.setEmailCompdec(normalizeOptional(emailCompdec));
        municipal.setTelefoneCompdec(normalizeOptional(telefoneCompdec));
        municipal.setCelularCompdec(normalizeOptional(celularCompdec));

        if (coordenadorCompdecId == null) {
            municipal.setCoordenadorCompdec(null);
        } else {
            Usuario coordenador = usuarioRepository.findByIdAndMunicipalId(coordenadorCompdecId, id).orElse(null);
            if (coordenador == null) {
                redirectAttributes.addFlashAttribute("error", "Selecione um coordenador valido da propria COMPDEC.");
                return "redirect:/master/regionais/municipais/" + id + "/editar";
            }
            municipal.setCoordenadorCompdec(coordenador);
        }

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

        if (usuarioRepository.existsByMunicipalId(id) || relatorioDinamicoRepository.existsByMunicipalId(id)) {
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
                || relatorioDinamicoRepository.existsByRegionalId(id)) {
            return "redirect:/master/regionais?regionalHasLinks";
        }

        regionalRepository.delete(regional);
        return "redirect:/master/regionais?regionalDeleted";
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
