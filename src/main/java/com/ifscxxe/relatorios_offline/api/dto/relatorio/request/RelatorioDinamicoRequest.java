package com.ifscxxe.relatorios_offline.api.dto.relatorio.request;

import java.util.Map;

public record RelatorioDinamicoRequest(
        Long templateId,
        String cidade,
        Map<String, Object> dados,
        Long municipalId
) {
}

