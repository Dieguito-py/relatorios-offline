package com.ifscxxe.relatorios_offline.api.controller;

import com.ifscxxe.relatorios_offline.api.dto.relatorio.request.RelatorioRequest;
import com.ifscxxe.relatorios_offline.api.dto.relatorio.response.RelatorioResponse;
import com.ifscxxe.relatorios_offline.coordenadoria.model.Municipal;
import com.ifscxxe.relatorios_offline.coordenadoria.repository.MunicipalRepository;
import com.ifscxxe.relatorios_offline.core.storage.FileStorageService;
import com.ifscxxe.relatorios_offline.core.storage.StoredFileMetadata;
import com.ifscxxe.relatorios_offline.relatorio.model.CadastroFamilia;
import com.ifscxxe.relatorios_offline.relatorio.repository.CadastroFamiliaRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/relatorios", "/api/cadastros-familia"})
public class RelatorioController {

    private final CadastroFamiliaRepository cadastroFamiliaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MunicipalRepository municipalRepository;
    private final FileStorageService fileStorageService;

    public RelatorioController(CadastroFamiliaRepository cadastroFamiliaRepository,
                               UsuarioRepository usuarioRepository,
                               MunicipalRepository municipalRepository,
                               FileStorageService fileStorageService) {
        this.cadastroFamiliaRepository = cadastroFamiliaRepository;
        this.usuarioRepository = usuarioRepository;
        this.municipalRepository = municipalRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(path = "/criar", consumes = "multipart/form-data")
    public ResponseEntity<?> criarRelatorio(@ModelAttribute RelatorioRequest request) {
        if (request == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Dados do relatório são obrigatórios"));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuário não autenticado"));
        }

        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));

        try {
            CadastroFamilia relatorio = new CadastroFamilia();

            relatorio.setNomeAtingido(request.nomeAtingido());
            relatorio.setCpfAtingido(request.cpfAtingido());
            relatorio.setRgAtingido(request.rgAtingido());
            relatorio.setDataNascimentoAtingido(request.dataNascimentoAtingido());
            relatorio.setEnderecoAtingido(request.enderecoAtingido());
            relatorio.setBairroAtingido(request.bairroAtingido());
            relatorio.setCidadeAtingido(request.cidadeAtingido());
            relatorio.setComplementoAtingido(request.complementoAtingido());

            relatorio.setLocalizacao(request.localizacao());
            relatorio.setMoradia(request.moradia());
            relatorio.setDanoResidencia(request.danoResidencia());
            relatorio.setEstimativaDanoMoveis(request.estimativaDanoMoveis());
            relatorio.setEstimativaDanoEdificacao(request.estimativaDanoEdificacao());
            relatorio.setOcupacao(request.ocupacao());
            relatorio.setTipoConstrucao(request.tipoConstrucao());
            relatorio.setAlternativaMoradia(request.alternativaMoradia());
            relatorio.setObservacaoImovel(request.observacaoImovel());

            relatorio.setNumeroTotalPessoas(request.numeroTotalPessoas());
            relatorio.setMenores0a12(request.menores0a12());
            relatorio.setMenores13a17(request.menores13a17());
            relatorio.setMaiores18a59(request.maiores18a59());
            relatorio.setIdosos60mais(request.idosos60mais());
            relatorio.setPossuiNecessidadesEspeciais(request.possuiNecessidadesEspeciais());
            relatorio.setQuantidadeNecessidadesEspeciais(request.quantidadeNecessidadesEspeciais());
            relatorio.setObservacaoNecessidades(request.observacaoNecessidades());
            relatorio.setUsoMedicamentoContinuo(request.usoMedicamentoContinuo());
            relatorio.setMedicamento(request.medicamento());
            relatorio.setPossuiDesaparecidos(request.possuiDesaparecidos());
            relatorio.setQuantidadeDesaparecidos(request.quantidadeDesaparecidos());
            relatorio.setQuantidadeFeridos(request.quantidadeFeridos());
            relatorio.setQuantidadeObitos(request.quantidadeObitos());

            relatorio.setQtdAguaPotavel5L(request.qtdAguaPotavel5L());
            relatorio.setQtdColchoesSolteiro(request.qtdColchoesSolteiro());
            relatorio.setQtdColchoesCasal(request.qtdColchoesCasal());
            relatorio.setQtdCestasBasicas(request.qtdCestasBasicas());
            relatorio.setQtdKitHigienePessoal(request.qtdKitHigienePessoal());
            relatorio.setQtdKitLimpeza(request.qtdKitLimpeza());
            relatorio.setQtdMoveis(request.qtdMoveis());
            relatorio.setQtdTelhas6mm(request.qtdTelhas6mm());
            relatorio.setQtdTelhas4mm(request.qtdTelhas4mm());
            relatorio.setQtdRoupas(request.qtdRoupas());
            relatorio.setOutrasNecessidades(request.outrasNecessidades());
            relatorio.setObservacaoAssistencia(request.observacaoAssistencia());

            relatorio.setLatitude(request.latitude());
            relatorio.setLongitude(request.longitude());

            relatorio.setUsuario(usuario);

            Municipal municipal = usuario.getMunicipal();
            if (request.municipalId() != null) {
                municipal = municipalRepository.findById(request.municipalId())
                        .orElseThrow(() -> new IllegalArgumentException("Municipal não encontrado"));
            }

            relatorio.setMunicipal(municipal);
            relatorio.setRegional(municipal != null && municipal.getRegional() != null
                    ? municipal.getRegional()
                    : usuario.getRegional());

            List<MultipartFile> fotosParaSalvar = new ArrayList<>();
            if (request.fotosResidencia() != null) {
                request.fotosResidencia().stream()
                        .filter(file -> file != null && !file.isEmpty())
                        .forEach(fotosParaSalvar::add);
            }

            if (!fotosParaSalvar.isEmpty()) {
                if (relatorio.getRegional() == null || relatorio.getRegional().getId() == null) {
                    throw new IllegalArgumentException("Regional não encontrada para salvar a foto da residência");
                }

                for (MultipartFile foto : fotosParaSalvar) {
                    StoredFileMetadata fotoSalva = fileStorageService.storeRegionalPhoto(
                            foto,
                            relatorio.getRegional().getId()
                    );
                    relatorio.addFotoResidencia(fotoSalva);
                }
            }

            CadastroFamilia savedRelatorio = cadastroFamiliaRepository.save(relatorio);

            RelatorioResponse response = new RelatorioResponse(
                    savedRelatorio.getId(),
                    savedRelatorio.getDataDesastre(),
                    savedRelatorio.getNomeAtingido(),
                    savedRelatorio.getCidadeAtingido()
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


}
