package com.ifscxxe.relatorios_offline.api.dto.relatorio.response;

import java.time.LocalDateTime;

public record RelatorioResponse(
        Long id,
        LocalDateTime dataDesastre,
        String nomeAtingido,
        String cidadeAtingido
) {}


