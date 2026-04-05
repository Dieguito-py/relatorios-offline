package com.ifscxxe.relatorios_offline.usuario.model;

public enum Role {
    AGENTECAMPO("Agente de Campo"),
    MUNICIPAL("Municipal"),
    REGIONAL("Regional"),
    MASTER("Master");

    private final String descricao;

    Role(String descricao) {
        this.descricao = descricao;
    }

    public static Role fromString(String value) {
        if (value == null) return null;
        String v = value.trim().toUpperCase();
        if (v.startsWith("ROLE_")) {
            v = v.substring(5);
        }
        return switch (v) {
            case "USER", "AGENTECAMPO" -> AGENTECAMPO;
            case "ADMIN", "MUNICIPAL" -> MUNICIPAL;
            case "SUPERADMIN", "REGIONAL" -> REGIONAL;
            case "MASTER" -> MASTER;
            default -> null;
        };
    }

    public String asAuthority() {
        return "ROLE_" + name();
    }

    public String getDescricao() {
        return descricao;
    }
}
