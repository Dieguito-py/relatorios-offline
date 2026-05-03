package com.ifscxxe.relatorios_offline.api.dto.auth.response;

public record LoginResponse(String token, String nome, Long municipalId, String municipalNome) {}

