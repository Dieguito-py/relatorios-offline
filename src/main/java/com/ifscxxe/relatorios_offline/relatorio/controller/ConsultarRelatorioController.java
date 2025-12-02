package com.ifscxxe.relatorios_offline.relatorio.controller;

import com.ifscxxe.relatorios_offline.relatorio.model.Relatorio;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private RelatorioRepository relatorioRepository;

    @GetMapping("/consultarRelatorios")
    public String consultar(
            Authentication authentication,
            Model model,
            @RequestParam(value = "inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        List<Relatorio> relatorios = Collections.emptyList();
        if (authentication != null) {
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
            if (usuario != null && usuario.getCoordenadoriaMunicipal() != null) {
                Long coordenadoriaId = usuario.getCoordenadoriaMunicipal().getId();
                if (inicio == null && fim == null) {
                    relatorios = relatorioRepository.findByCoordenadoriaMunicipalIdOrderByIdDesc(coordenadoriaId);
                } else {
                    LocalDateTime inicioDateTime = inicio != null
                            ? inicio.atStartOfDay()
                            : LocalDate.of(1970, Month.JANUARY, 1).atStartOfDay();
                    LocalDateTime fimDateTime = fim != null
                            ? fim.atTime(LocalTime.MAX)
                            : LocalDate.of(9999, Month.DECEMBER, 31).atTime(LocalTime.MAX);
                    relatorios = relatorioRepository.findByCoordenadoriaMunicipalIdAndDataDesastreBetweenOrderByDataDesastreDesc(
                            coordenadoriaId,
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
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getCoordenadoriaMunicipal() == null) {
            model.addAttribute("error", "Usuário sem coordenadoria associada.");
            return "redirect:/relatorios/consultarRelatorios";
        }
        Long coordenadoriaId = usuarioOpt.get().getCoordenadoriaMunicipal().getId();
        Optional<Relatorio> relatorioOpt = relatorioRepository.findByIdAndCoordenadoriaMunicipalId(id, coordenadoriaId);
        if (relatorioOpt.isEmpty()) {
            model.addAttribute("error", "Relatório não encontrado ou indisponível.");
            return "redirect:/relatorios/consultarRelatorios";
        }
        Relatorio relatorio = relatorioOpt.get();
        model.addAttribute("relatorio", relatorio);

        String fotoResidenciaUrl = null;
        if (StringUtils.hasText(relatorio.getFotoResidencia())) {
            fotoResidenciaUrl = relatorio.getFotoResidencia().startsWith("http")
                    ? relatorio.getFotoResidencia()
                    : "data:image/jpeg;base64," + relatorio.getFotoResidencia();
        }
        model.addAttribute("fotoResidenciaUrl", fotoResidenciaUrl);

        if (StringUtils.hasText(relatorio.getLatitude()) && StringUtils.hasText(relatorio.getLongitude())) {
            model.addAttribute("googleMapsUrl",
                    "https://www.google.com/maps/search/?api=1&query=" + relatorio.getLatitude() + "," + relatorio.getLongitude());
        }

        model.addAttribute("pageTitle", "Detalhes do Relatório");
        return "relatorio/detalheRelatorio";
    }
}
