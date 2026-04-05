package com.ifscxxe.relatorios_offline.core.storage;

public record StoredFileMetadata(
        String nomeOriginal,
        String nomeGerado,
        String caminho,
        String contentType,
        Long tamanho
) {}

