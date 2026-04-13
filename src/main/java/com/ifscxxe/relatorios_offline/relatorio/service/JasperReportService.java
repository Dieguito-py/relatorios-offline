package com.ifscxxe.relatorios_offline.relatorio.service;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class JasperReportService {

    private static final String DEFAULT_TEMPLATE = "relatorioFamilias.jrxml";

    private final Path baseDir;
    private final Path uploadBaseDir;
    private final boolean cacheEnabled;
    private final boolean reloadOnChange;
    private final ConcurrentMap<Path, CachedReport> cache = new ConcurrentHashMap<>();

    public JasperReportService(
            @Value("${app.reports.base-dir}") String baseDir,
            @Value("${app.upload.base-dir:relatorioOffline/uploads}") String uploadBaseDir,
            @Value("${app.reports.cache-enabled:true}") boolean cacheEnabled,
            @Value("${app.reports.reload-on-change:true}") boolean reloadOnChange
    ) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
        this.uploadBaseDir = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        this.cacheEnabled = cacheEnabled;
        this.reloadOnChange = reloadOnChange;
    }

    public byte[] gerarRelatorioPdf(
            Long regionalId,
            String templateRelativoConfigurado,
            Collection<?> dados,
            Map<String, Object> parametrosAdicionais
    ) {
        Path templatePath = resolveTemplatePath(regionalId, templateRelativoConfigurado);
        JasperReport jasperReport = getCompiledReport(templatePath);

        try {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("REPORT_LOCALE", Locale.forLanguageTag("pt-BR"));
            parametros.put("REGIONAL_ID", regionalId);
            parametros.put("GERADO_EM", LocalDateTime.now());
            parametros.put("UPLOAD_BASE_DIR", uploadBaseDir.toString());
            if (parametrosAdicionais != null && !parametrosAdicionais.isEmpty()) {
                parametros.putAll(parametrosAdicionais);
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(dados == null ? List.of() : dados);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (JRException ex) {
            throw new IllegalStateException("Falha ao gerar PDF do relatório: " + ex.getMessage(), ex);
        }
    }


    private JasperReport getCompiledReport(Path templatePath) {
        try {
            long lastModified = Files.getLastModifiedTime(templatePath).toMillis();

            if (!cacheEnabled) {
                return JasperCompileManager.compileReport(templatePath.toString());
            }

            CachedReport cached = cache.get(templatePath);
            if (cached != null && (!reloadOnChange || cached.lastModified == lastModified)) {
                return cached.jasperReport;
            }

            JasperReport compiled = JasperCompileManager.compileReport(templatePath.toString());
            cache.put(templatePath, new CachedReport(compiled, lastModified));
            return compiled;
        } catch (JRException | IOException ex) {
            throw new IllegalStateException(
                    "Falha ao compilar o template Jasper configurado para a regional (" + templatePath + "): " + ex.getMessage(),
                    ex
            );
        }
    }

    private Path resolveTemplatePath(Long regionalId, String templateRelativoConfigurado) {
        if (regionalId == null) {
            throw new IllegalArgumentException("Regional não encontrada para o usuário autenticado.");
        }

        String templateRelativo = sanitizeRelativeTemplatePath(templateRelativoConfigurado);
        Path regionalDir = baseDir.resolve(String.valueOf(regionalId)).normalize();
        Path resolved = regionalDir.resolve(templateRelativo).normalize();

        if (!resolved.startsWith(regionalDir)) {
            throw new IllegalArgumentException("Caminho do template inválido para a regional.");
        }

        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("Template JRXML não encontrado para a regional informada.");
        }

        return resolved;
    }

    private String sanitizeRelativeTemplatePath(String rawPath) {
        String value = (rawPath == null || rawPath.trim().isEmpty()) ? DEFAULT_TEMPLATE : rawPath.trim();
        value = value.replace('\\', '/');

        if (value.startsWith("/") || value.matches("^[a-zA-Z]:.*")) {
            throw new IllegalArgumentException("Informe apenas caminho relativo do template.");
        }

        Path relative = Paths.get(value).normalize();
        if (relative.isAbsolute()) {
            throw new IllegalArgumentException("Informe apenas caminho relativo do template.");
        }

        for (Path part : relative) {
            if ("..".equals(part.toString())) {
                throw new IllegalArgumentException("Caminho do template não pode conter '..'.");
            }
        }

        String normalized = relative.toString().replace('\\', '/');
        if (!normalized.toLowerCase(Locale.ROOT).endsWith(".jrxml")) {
            throw new IllegalArgumentException("O template deve ter extensão .jrxml.");
        }

        return normalized;
    }

    private record CachedReport(JasperReport jasperReport, long lastModified) {
    }
}

