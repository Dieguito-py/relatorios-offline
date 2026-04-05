package com.ifscxxe.relatorios_offline.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadBaseDir;

    public WebConfig(@Value("${app.upload.base-dir:relatorioOffline/uploads}") String uploadBaseDir) {
        this.uploadBaseDir = uploadBaseDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadPath = Paths.get(uploadBaseDir).toAbsolutePath().normalize().toUri().toString();
        if (!absoluteUploadPath.endsWith("/")) {
            absoluteUploadPath = absoluteUploadPath + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absoluteUploadPath);
    }
}

