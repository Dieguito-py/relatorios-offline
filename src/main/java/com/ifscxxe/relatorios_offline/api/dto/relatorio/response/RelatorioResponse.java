package com.ifscxxe.relatorios_offline.api.dto.relatorio.response;

import java.time.LocalDateTime;

public record RelatorioResponse(
        Long id,
        LocalDateTime dataRegistro,
        Long templateId,
        String cidade,
        Long municipalId,
        Long regionalId
) {}


