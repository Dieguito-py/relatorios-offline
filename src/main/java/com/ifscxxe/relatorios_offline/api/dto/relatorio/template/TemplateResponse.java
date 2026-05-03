package com.ifscxxe.relatorios_offline.api.dto.relatorio.template;

import java.util.List;

public record TemplateResponse(
        Long id,
        Long regionalId,
        String nome,
        String descricao,
        boolean ativo,
        List<TemplateCampoResponse> campos
) {
}

