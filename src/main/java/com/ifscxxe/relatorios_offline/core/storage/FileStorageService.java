package com.ifscxxe.relatorios_offline.core.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadBaseDir;

    public FileStorageService(@Value("${app.upload.base-dir:relatorioOffline/uploads}") String uploadBaseDir) {
        try {
            this.uploadBaseDir = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
            Files.createDirectories(this.uploadBaseDir);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao inicializar diretório de uploads", e);
        }
    }

    public StoredFileMetadata storeRegionalPhoto(MultipartFile file, Long regionalId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de foto é obrigatório");
        }

        try {
            Path regionalDir = uploadBaseDir.resolve(Paths.get("regional", regionalId.toString())).normalize();
            Files.createDirectories(regionalDir);

            String nomeOriginal = StringUtils.cleanPath(file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename());
            String extension = getExtension(nomeOriginal);
            String nomeGerado = UUID.randomUUID() + extension;
            Path destination = regionalDir.resolve(nomeGerado).normalize();

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            String caminho = "/uploads/regional/" + regionalId + "/" + nomeGerado;
            return new StoredFileMetadata(
                    nomeOriginal,
                    nomeGerado,
                    caminho,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar foto da residência", e);
        }
    }

    private String getExtension(String originalFilename) {
        String cleanName = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int extensionIndex = cleanName.lastIndexOf('.');
        if (extensionIndex < 0) {
            return ".jpg";
        }
        return cleanName.substring(extensionIndex);
    }
}

