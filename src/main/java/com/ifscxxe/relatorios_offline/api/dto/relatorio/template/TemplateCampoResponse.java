package com.ifscxxe.relatorios_offline.api.dto.relatorio.template;

import java.util.List;

public record TemplateCampoResponse(
        String chave,
        String rotulo,
        String tipo,
        boolean obrigatorio,
        Integer ordem,
        List<String> opcoes
) {
}

