package com.ifscxxe.relatorios_offline.api.dto.relatorio.request;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public record RelatorioRequest(
        String nomeAtingido,
        String cpfAtingido,
        String rgAtingido,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
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
        Long municipalId,
        String latitude,
        String longitude,
        List<MultipartFile> fotosResidencia
) {}


