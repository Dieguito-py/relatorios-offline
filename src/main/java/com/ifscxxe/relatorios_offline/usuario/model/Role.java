package com.ifscxxe.relatorios_offline.usuario.model;

public enum Role {
    USER,
    ADMIN;

    public static Role fromString(String value) {
        if (value == null) return null;
        String v = value.trim().toUpperCase();
        if (v.startsWith("ROLE_")) {
            v = v.substring(5);
        }
        try {
            return Role.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String asAuthority() {
        return "ROLE_" + name();
    }
}
