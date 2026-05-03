package com.ifscxxe.relatorios_offline.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifscxxe.relatorios_offline.api.dto.relatorio.request.RelatorioDinamicoRequest;
import com.ifscxxe.relatorios_offline.api.dto.relatorio.response.RelatorioResponse;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Municipal;
import com.ifscxxe.relatorios_offline.coordenadoria.repository.MunicipalRepository;
import com.ifscxxe.relatorios_offline.core.storage.FileStorageService;
import com.ifscxxe.relatorios_offline.core.storage.StoredFileMetadata;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioDinamico;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioFoto;
import com.ifscxxe.relatorios_offline.relatorio.model.RelatorioTemplate;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioDinamicoRepository;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioFotoRepository;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioTemplateRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioDinamicoRepository relatorioDinamicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MunicipalRepository municipalRepository;
    private final RelatorioTemplateRepository templateRepository;
    private final RelatorioFotoRepository fotoRepository;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    public RelatorioController(RelatorioDinamicoRepository relatorioDinamicoRepository,
                               UsuarioRepository usuarioRepository,
                               MunicipalRepository municipalRepository,
                               RelatorioTemplateRepository templateRepository,
                               RelatorioFotoRepository fotoRepository,
                               ObjectMapper objectMapper,
                               FileStorageService fileStorageService) {
        this.relatorioDinamicoRepository = relatorioDinamicoRepository;
        this.usuarioRepository = usuarioRepository;
        this.municipalRepository = municipalRepository;
        this.templateRepository = templateRepository;
        this.fotoRepository = fotoRepository;
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(path = "/criar", consumes = "multipart/form-data")
    public ResponseEntity<?> criarRelatorio(
            @RequestParam("request") String requestJson,
            HttpServletRequest httpRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuário não autenticado"));
        }

        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));

        try {
            RelatorioDinamicoRequest request = objectMapper.readValue(requestJson, RelatorioDinamicoRequest.class);

            if (request.templateId() == null) {
                throw new IllegalArgumentException("Informe o templateId do relatório.");
            }
            if (request.cidade() == null || request.cidade().trim().isEmpty()) {
                throw new IllegalArgumentException("Informe a cidade do relatório.");
            }

            Municipal municipal;
            if (request.municipalId() != null) {
                municipal = municipalRepository.findById(request.municipalId())
                        .orElseThrow(() -> new IllegalArgumentException("Municipal não encontrado"));
            } else {
                municipal = usuario.getMunicipal();
                if (municipal == null) {
                    throw new IllegalArgumentException("Informe municipalId para criar o relatório.");
                }
            }

            RelatorioTemplate template = templateRepository.findById(request.templateId())
                    .orElseThrow(() -> new IllegalArgumentException("Template não encontrado"));

            if (!Boolean.TRUE.equals(template.getAtivo())) {
                throw new IllegalArgumentException("Template está inativo");
            }

            if (template.getRegional() == null || template.getRegional().getId() == null) {
                throw new IllegalArgumentException("Template não possui regional configurada");
            }
            if (municipal.getRegional() == null || municipal.getRegional().getId() == null) {
                throw new IllegalArgumentException("Municipal sem regional vinculada");
            }
            if (!template.getRegional().getId().equals(municipal.getRegional().getId())) {
                throw new IllegalArgumentException("Template não pertence à regional do municipal informado");
            }

            RelatorioDinamico relatorio = new RelatorioDinamico();
            relatorio.setTemplate(template);
            relatorio.setCidade(request.cidade().trim());
            relatorio.setDadosJson(request.dados() == null ? null : objectMapper.writeValueAsString(request.dados()));
            relatorio.setUsuario(usuario);
            relatorio.setMunicipal(municipal);
            relatorio.setRegional(municipal.getRegional());

            RelatorioDinamico savedRelatorio = relatorioDinamicoRepository.save(relatorio);

            Long regionalId = municipal.getRegional().getId();
            if (httpRequest instanceof MultipartHttpServletRequest multipartRequest) {
                for (Map.Entry<String, List<MultipartFile>> entry : multipartRequest.getMultiFileMap().entrySet()) {
                    for (MultipartFile file : entry.getValue()) {
                        if (!file.isEmpty()) {
                            salvarFoto(savedRelatorio, entry.getKey(), file, regionalId);
                        }
                    }
                }
            }

            RelatorioResponse response = new RelatorioResponse(
                    savedRelatorio.getId(),
                    savedRelatorio.getDataRegistro(),
                    savedRelatorio.getTemplate().getId(),
                    savedRelatorio.getCidade(),
                    savedRelatorio.getMunicipal().getId(),
                    savedRelatorio.getRegional().getId()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao criar relatório: " + e.getMessage()));
        }
    }

    private void salvarFoto(RelatorioDinamico relatorio, String chave, MultipartFile file, Long regionalId) {
        StoredFileMetadata meta = fileStorageService.storeRegionalPhoto(file, regionalId);

        RelatorioFoto foto = new RelatorioFoto();
        foto.setChave(chave);
        foto.setNomeOriginal(meta.nomeOriginal());
        foto.setNomeGerado(meta.nomeGerado());
        foto.setCaminho(meta.caminho());
        foto.setContentType(meta.contentType());
        foto.setTamanho(meta.tamanho());
        foto.setRelatorio(relatorio);
        fotoRepository.save(foto);
    }
}
