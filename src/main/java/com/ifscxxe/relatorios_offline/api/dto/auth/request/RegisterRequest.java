package com.ifscxxe.relatorios_offline.api.dto.auth.request;

import java.util.List;

public record RegisterRequest(String username, String password, List<String> roles) {}

