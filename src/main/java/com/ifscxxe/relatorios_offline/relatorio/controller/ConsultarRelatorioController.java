package com.ifscxxe.relatorios_offline.relatorio.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioDinamico;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioFoto;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioDinamicoRepository;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioFotoRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Municipal;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Regional;
import com.ifscxxe.relatorios_offline.coordenadoria.repository.MunicipalRepository;
import com.ifscxxe.relatorios_offline.coordenadoria.repository.RegionalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/relatorios")
public class ConsultarRelatorioController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RelatorioDinamicoRepository relatorioDinamicoRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MunicipalRepository municipalRepository;
    @Autowired
    private RegionalRepository regionalRepository;
    @Autowired
    private RelatorioFotoRepository relatorioFotoRepository;

    @GetMapping("/consultarRelatorios")
    public String consultar(
            Authentication authentication,
            Model model,
            @RequestParam(value = "inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(value = "cidade", required = false) String cidade,
            @RequestParam(value = "municipalId", required = false) Long municipalId,
            @RequestParam(value = "regionalId", required = false) Long regionalId
    ) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            LocalDate temp = inicio;
            inicio = fim;
            fim = temp;
        }

        String cidadeFiltro = cidade != null ? cidade.trim() : "";

        List<RelatorioDinamico> relatorios = Collections.emptyList();
        List<Municipal> municipais = Collections.emptyList();
        List<Regional> regionais = Collections.emptyList();
        if (authentication != null) {
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
            boolean isMunicipal = isMunicipal(authentication);

            if (usuario != null && isMunicipal && usuario.getMunicipal() != null) {
                Long municipalUsuarioId = usuario.getMunicipal().getId();
                municipalId = municipalUsuarioId;
                if (usuario.getMunicipal().getRegional() != null) {
                    regionalId = usuario.getMunicipal().getRegional().getId();
                    regionais = List.of(usuario.getMunicipal().getRegional());
                }
                municipais = List.of(usuario.getMunicipal());
                LocalDateTime inicioDateTime = inicio != null
                        ? inicio.atStartOfDay()
                        : LocalDate.of(1970, Month.JANUARY, 1).atStartOfDay();
                LocalDateTime fimDateTime = fim != null
                        ? fim.atTime(LocalTime.MAX)
                        : LocalDate.of(9999, Month.DECEMBER, 31).atTime(LocalTime.MAX);
                relatorios = relatorioDinamicoRepository.buscarPorMunicipalFiltros(
                        municipalUsuarioId,
                        cidadeFiltro,
                        inicioDateTime,
                        fimDateTime
                );
            } else if (usuario != null && usuario.getRegional() != null) {
                Long regionalUsuarioId = usuario.getRegional().getId();
                regionalId = regionalUsuarioId;
                regionais = List.of(usuario.getRegional());
                municipais = municipalRepository.findByRegionalId(regionalUsuarioId)
                        .stream()
                        .sorted(Comparator.comparing(Municipal::getNome, Comparator.nullsLast(String::compareToIgnoreCase)))
                        .toList();
                LocalDateTime inicioDateTime = inicio != null
                        ? inicio.atStartOfDay()
                        : LocalDate.of(1970, Month.JANUARY, 1).atStartOfDay();
                LocalDateTime fimDateTime = fim != null
                        ? fim.atTime(LocalTime.MAX)
                        : LocalDate.of(9999, Month.DECEMBER, 31).atTime(LocalTime.MAX);
                relatorios = relatorioDinamicoRepository.buscarPorFiltros(
                        regionalUsuarioId,
                        municipalId,
                        cidadeFiltro,
                        inicioDateTime,
                        fimDateTime
                );
            } else if (hasAuthority(authentication, "ROLE_MASTER")) {
                regionais = regionalRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
                municipais = municipalRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
                LocalDateTime inicioDateTime = inicio != null
                        ? inicio.atStartOfDay()
                        : LocalDate.of(1970, Month.JANUARY, 1).atStartOfDay();
                LocalDateTime fimDateTime = fim != null
                        ? fim.atTime(LocalTime.MAX)
                        : LocalDate.of(9999, Month.DECEMBER, 31).atTime(LocalTime.MAX);
                relatorios = relatorioDinamicoRepository.buscarPorFiltros(
                        regionalId,
                        municipalId,
                        cidadeFiltro,
                        inicioDateTime,
                        fimDateTime
                );
            }
        }
        model.addAttribute("relatorios", relatorios);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("cidade", cidadeFiltro);
        model.addAttribute("municipalId", municipalId);
        model.addAttribute("regionalId", regionalId);
        model.addAttribute("municipais", municipais);
        model.addAttribute("regionais", regionais);
        return "relatorio/consultarRelatorios";
    }

    @GetMapping("/{id:\\d+}")
    @Transactional(readOnly = true)
    public String detalhar(@PathVariable Long id, Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(authentication.getName());
        if (usuarioOpt.isEmpty()) {
            model.addAttribute("error", "Usuário não encontrado.");
            return "redirect:/relatorios/consultarRelatorios";
        }

        Usuario usuario = usuarioOpt.get();
        Optional<RelatorioDinamico> relatorioOpt = buscarRelatorioNoEscopo(id, usuario, authentication);

        if (relatorioOpt.isEmpty()) {
            model.addAttribute("error", "Relatório não encontrado ou indisponível.");
            return "redirect:/relatorios/consultarRelatorios";
        }
        RelatorioDinamico relatorio = relatorioOpt.get();
        model.addAttribute("relatorio", relatorio);

        Map<String, Object> dados = parseDados(relatorio.getDadosJson());
        List<FieldView> campos = relatorio.getTemplate() == null
                ? Collections.emptyList()
                : relatorio.getTemplate().getCampos().stream()
                .map(campo -> new FieldView(
                        campo.getRotulo(),
                        formatarValor(dados.get(campo.getChave())),
                        campo.getTipo(),
                        campo.getChave()
                ))
                .toList();

        model.addAttribute("camposRenderizados", campos);
        model.addAttribute("fotos", relatorioFotoRepository.findByRelatorioId(id));
        model.addAttribute("pageTitle", "Detalhes do Relatório");
        return "relatorio/detalheRelatorio";
    }

    @PostMapping("/{id:\\d+}/excluir")
    public String excluir(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(authentication.getName());
        if (usuarioOpt.isEmpty()) {
            return "redirect:/relatorios/consultarRelatorios?invalidRelatorio";
        }

        Optional<RelatorioDinamico> relatorioOpt = buscarRelatorioNoEscopo(id, usuarioOpt.get(), authentication);
        if (relatorioOpt.isEmpty()) {
            return "redirect:/relatorios/consultarRelatorios?invalidRelatorio";
        }

        relatorioDinamicoRepository.delete(relatorioOpt.get());
        return "redirect:/relatorios/consultarRelatorios?deleted";
    }

    private boolean isMunicipal(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> "ROLE_MUNICIPAL".equals(granted.getAuthority()));
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private Optional<RelatorioDinamico> buscarRelatorioNoEscopo(Long id, Usuario usuario, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_MUNICIPAL")) {
            if (usuario.getMunicipal() == null) {
                return Optional.empty();
            }
            return relatorioDinamicoRepository.findByIdAndMunicipalId(id, usuario.getMunicipal().getId());
        }
        if (hasAuthority(authentication, "ROLE_REGIONAL")) {
            if (usuario.getRegional() == null) {
                return Optional.empty();
            }
            return relatorioDinamicoRepository.findByIdAndRegionalId(id, usuario.getRegional().getId());
        }
        if (hasAuthority(authentication, "ROLE_MASTER")) {
            return relatorioDinamicoRepository.findById(id);
        }
        return Optional.empty();
    }

    private Map<String, Object> parseDados(String dadosJson) {
        if (!StringUtils.hasText(dadosJson)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(dadosJson, new TypeReference<>() {});
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private String formatarValor(Object valor) {
        if (valor == null) {
            return "-";
        }
        if (valor instanceof Boolean bool) {
            return bool ? "Sim" : "Não";
        }
        if (valor instanceof List<?> lista) {
            return lista.stream()
                    .map(item -> item == null ? "" : item.toString())
                    .filter(StringUtils::hasText)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("-");
        }
        return valor.toString();
    }

    private record FieldView(String rotulo, String valor, com.ifscxxe.relatorios_offline.relatorio.model.RelatorioCampoTipo tipo, String chave) {
    }
}
