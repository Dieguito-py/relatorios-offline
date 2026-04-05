package com.ifscxxe.relatorios_offline.api.dto.auth.response;

import java.util.List;

public record RegisterResponse(String username, List<String> roles) {}

