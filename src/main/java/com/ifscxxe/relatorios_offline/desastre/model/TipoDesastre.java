package com.ifscxxe.relatorios_offline.desastre.model;

public enum TipoDesastre {
    INUNDACOES("Inundações"),
    ENXURRADAS("Enxurradas"),
    ALAGAMENTOS("Alagamentos"),
    CHUVAS_INTENSAS("Chuvas Intensas"),
    TORNADOS("Tornados"),
    GRANIZO("Granizo"),
    VENDAVAL("Vendaval"),
    ESTIAGEM("Estiagem"),
    OUTROS("Outros");

    private final String descricao;

    TipoDesastre(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
