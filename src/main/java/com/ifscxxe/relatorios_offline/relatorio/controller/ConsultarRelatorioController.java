package com.ifscxxe.relatorios_offline.relatorio.controller;

import com.ifscxxe.relatorios_offline.relatorio.model.CadastroFamilia;
import com.ifscxxe.relatorios_offline.relatorio.repository.CadastroFamiliaRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/relatorios")
public class ConsultarRelatorioController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CadastroFamiliaRepository cadastroFamiliaRepository;

    @GetMapping("/consultarRelatorios")
    public String consultar(
            Authentication authentication,
            Model model,
            @RequestParam(value = "inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            LocalDate temp = inicio;
            inicio = fim;
            fim = temp;
        }

        List<CadastroFamilia> relatorios = Collections.emptyList();
        if (authentication != null) {
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
            boolean isMunicipal = isMunicipal(authentication);

            if (usuario != null && isMunicipal && usuario.getMunicipal() != null) {
                Long municipalId = usuario.getMunicipal().getId();
                if (inicio == null && fim == null) {
                    relatorios = cadastroFamiliaRepository.findByMunicipalIdOrderByIdDesc(municipalId);
                } else {
                    LocalDateTime inicioDateTime = inicio != null
                            ? inicio.atStartOfDay()
                            : LocalDate.of(1970, Month.JANUARY, 1).atStartOfDay();
                    LocalDateTime fimDateTime = fim != null
                            ? fim.atTime(LocalTime.MAX)
                            : LocalDate.of(9999, Month.DECEMBER, 31).atTime(LocalTime.MAX);
                    relatorios = cadastroFamiliaRepository.findByMunicipalIdAndDataDesastreBetweenOrderByDataDesastreDesc(
                            municipalId,
                            inicioDateTime,
                            fimDateTime
                    );
                }
            } else if (usuario != null && usuario.getRegional() != null) {
                Long regionalId = usuario.getRegional().getId();
                if (inicio == null && fim == null) {
                    relatorios = cadastroFamiliaRepository.findByRegionalIdOrderByIdDesc(regionalId);
                } else {
                    LocalDateTime inicioDateTime = inicio != null
                            ? inicio.atStartOfDay()
                            : LocalDate.of(1970, Month.JANUARY, 1).atStartOfDay();
                    LocalDateTime fimDateTime = fim != null
                            ? fim.atTime(LocalTime.MAX)
                            : LocalDate.of(9999, Month.DECEMBER, 31).atTime(LocalTime.MAX);
                    relatorios = cadastroFamiliaRepository.findByRegionalIdAndDataDesastreBetweenOrderByDataDesastreDesc(
                            regionalId,
                            inicioDateTime,
                            fimDateTime
                    );
                }
            } else if (hasAuthority(authentication, "ROLE_MASTER")) {
                if (inicio == null && fim == null) {
                    relatorios = cadastroFamiliaRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
                } else {
                    LocalDateTime inicioDateTime = inicio != null
                            ? inicio.atStartOfDay()
                            : LocalDate.of(1970, Month.JANUARY, 1).atStartOfDay();
                    LocalDateTime fimDateTime = fim != null
                            ? fim.atTime(LocalTime.MAX)
                            : LocalDate.of(9999, Month.DECEMBER, 31).atTime(LocalTime.MAX);
                    relatorios = cadastroFamiliaRepository.findByDataDesastreBetweenOrderByDataDesastreDesc(
                            inicioDateTime,
                            fimDateTime
                    );
                }
            }
        }
        model.addAttribute("relatorios", relatorios);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        return "relatorio/consultarRelatorios";
    }

    @GetMapping("/{id:\\d+}")
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
        Optional<CadastroFamilia> relatorioOpt = buscarRelatorioNoEscopo(id, usuario, authentication);

        if (relatorioOpt.isEmpty()) {
            model.addAttribute("error", "Relatório não encontrado ou indisponível.");
            return "redirect:/relatorios/consultarRelatorios";
        }
        CadastroFamilia relatorio = relatorioOpt.get();
        model.addAttribute("relatorio", relatorio);

        List<String> fotoResidenciaUrls = relatorio.getFotosResidencia() == null
                ? Collections.emptyList()
                : relatorio.getFotosResidencia().stream()
                .map(foto -> normalizarFotoResidenciaUrl(foto.getCaminho()))
                .filter(StringUtils::hasText)
                .toList();
        model.addAttribute("fotoResidenciaUrls", fotoResidenciaUrls);

        if (StringUtils.hasText(relatorio.getLatitude()) && StringUtils.hasText(relatorio.getLongitude())) {
            model.addAttribute("googleMapsUrl",
                    "https://www.google.com/maps/search/?api=1&query=" + relatorio.getLatitude() + "," + relatorio.getLongitude());
        }

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

        Optional<CadastroFamilia> relatorioOpt = buscarRelatorioNoEscopo(id, usuarioOpt.get(), authentication);
        if (relatorioOpt.isEmpty()) {
            return "redirect:/relatorios/consultarRelatorios?invalidRelatorio";
        }

        cadastroFamiliaRepository.delete(relatorioOpt.get());
        return "redirect:/relatorios/consultarRelatorios?deleted";
    }


    private String normalizarFotoResidenciaUrl(String fotoResidencia) {
        if (!StringUtils.hasText(fotoResidencia)) {
            return null;
        }
        if (fotoResidencia.startsWith("http") || fotoResidencia.startsWith("/")) {
            return fotoResidencia;
        }
        if (fotoResidencia.startsWith("uploads/")) {
            return "/" + fotoResidencia;
        }
        return "data:image/jpeg;base64," + fotoResidencia;
    }

    private boolean isMunicipal(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> "ROLE_MUNICIPAL".equals(granted.getAuthority()));
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private Optional<CadastroFamilia> buscarRelatorioNoEscopo(Long id, Usuario usuario, Authentication authentication) {
        if (hasAuthority(authentication, "ROLE_MUNICIPAL")) {
            if (usuario.getMunicipal() == null) {
                return Optional.empty();
            }
            return cadastroFamiliaRepository.findByIdAndMunicipalId(id, usuario.getMunicipal().getId());
        }
        if (hasAuthority(authentication, "ROLE_REGIONAL")) {
            if (usuario.getRegional() == null) {
                return Optional.empty();
            }
            return cadastroFamiliaRepository.findByIdAndRegionalId(id, usuario.getRegional().getId());
        }
        if (hasAuthority(authentication, "ROLE_MASTER")) {
            return cadastroFamiliaRepository.findById(id);
        }
        return Optional.empty();
    }
}
