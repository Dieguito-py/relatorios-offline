package com.ifscxxe.relatorios_offline.api.controller;

import com.ifscxxe.relatorios_offline.coordenadoria.model.CoordenadoriaMunicipal;
import com.ifscxxe.relatorios_offline.relatorio.model.Relatorio;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioRepository relatorioRepository;
    private final UsuarioRepository usuarioRepository;

    public RelatorioController(RelatorioRepository relatorioRepository, UsuarioRepository usuarioRepository) {
        this.relatorioRepository = relatorioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @RequestMapping("/criar")
    public ResponseEntity<?> criarRelatorio(@RequestBody RelatorioRequest request) {
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
            Relatorio relatorio = new Relatorio();

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

            relatorio.setUsuario(usuario);
            relatorio.setCoordenadoriaMunicipal(usuario.getCoordenadoriaMunicipal());

            if (request.coordenadoriaMunicipalId() != null) {
                CoordenadoriaMunicipal coordenadoria = new CoordenadoriaMunicipal();
                coordenadoria.setId(request.coordenadoriaMunicipalId());
                relatorio.setCoordenadoriaMunicipal(coordenadoria);
            }

            Relatorio savedRelatorio = relatorioRepository.save(relatorio);

            RelatorioResponse response = new RelatorioResponse(
                    savedRelatorio.getId(),
                    savedRelatorio.getDataDesastre(),
                    savedRelatorio.getNomeAtingido(),
                    savedRelatorio.getCidadeAtingido()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao criar relatório: " + e.getMessage()));
        }
    }

    public record RelatorioRequest(
            String nomeAtingido,
            String cpfAtingido,
            String rgAtingido,
            LocalDateTime dataNascimentoAtingido,
            String enderecoAtingido,
            String bairroAtingido,
            String cidadeAtingido,
            String complementoAtingido,
            String localizacao,
            String moradia,
            String danoResidencia,
            Double estimativaDanoMoveis,
            Double estimativaDanoEdificacao,
            String ocupacao,
            String tipoConstrucao,
            String alternativaMoradia,
            String observacaoImovel,
            Integer numeroTotalPessoas,
            Integer menores0a12,
            Integer menores13a17,
            Integer maiores18a59,
            Integer idosos60mais,
            Boolean possuiNecessidadesEspeciais,
            Integer quantidadeNecessidadesEspeciais,
            String observacaoNecessidades,
            Boolean usoMedicamentoContinuo,
            String medicamento,
            Boolean possuiDesaparecidos,
            Integer quantidadeDesaparecidos,
            Integer quantidadeFeridos,
            Integer quantidadeObitos,
            Integer qtdAguaPotavel5L,
            Integer qtdColchoesSolteiro,
            Integer qtdColchoesCasal,
            Integer qtdCestasBasicas,
            Integer qtdKitHigienePessoal,
            Integer qtdKitLimpeza,
            Integer qtdMoveis,
            Integer qtdTelhas6mm,
            Integer qtdTelhas4mm,
            Integer qtdRoupas,
            String outrasNecessidades,
            String observacaoAssistencia,
            Long coordenadoriaMunicipalId
    ) {}

    public record RelatorioResponse(
            Long id,
            LocalDateTime dataDesastre,
            String nomeAtingido,
            String cidadeAtingido
    ) {}
}
