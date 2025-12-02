package com.ifscxxe.relatorios_offline.relatorio.controller;

import com.ifscxxe.relatorios_offline.relatorio.model.Relatorio;
import com.ifscxxe.relatorios_offline.relatorio.repository.RelatorioRepository;
import com.ifscxxe.relatorios_offline.usuario.model.Usuario;
import com.ifscxxe.relatorios_offline.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/relatorios")
public class ExportarRelatorioController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private RelatorioRepository relatorioRepository;

    @GetMapping("/exportar")
    public void exportar(
            Authentication authentication,
            HttpServletResponse response,
            @RequestParam(value = "inicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(value = "format", required = false, defaultValue = "xlsx") String format
    ) throws IOException {
        List<Relatorio> relatorios = Collections.emptyList();
        
        if (authentication != null) {
            Usuario usuario = usuarioRepository.findByUsername(authentication.getName()).orElse(null);
            if (usuario != null && usuario.getCoordenadoriaMunicipal() != null) {
                Long coordenadoriaId = usuario.getCoordenadoriaMunicipal().getId();
                if (inicio == null && fim == null) {
                    relatorios = relatorioRepository.findByCoordenadoriaMunicipalIdOrderByIdDesc(coordenadoriaId);
                } else {
                    LocalDateTime inicioDateTime = inicio != null
                            ? inicio.atStartOfDay()
                            : LocalDate.of(1970, Month.JANUARY, 1).atStartOfDay();
                    LocalDateTime fimDateTime = fim != null
                            ? fim.atTime(LocalTime.MAX)
                            : LocalDate.of(9999, Month.DECEMBER, 31).atTime(LocalTime.MAX);
                    relatorios = relatorioRepository.findByCoordenadoriaMunicipalIdAndDataDesastreBetweenOrderByDataDesastreDesc(
                            coordenadoriaId,
                            inicioDateTime,
                            fimDateTime
                    );
                }
            }
        }

        if ("xlsx".equalsIgnoreCase(format)) {
            gerarExcel(relatorios, response);
        } else if ("pdf".equalsIgnoreCase(format)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exportação PDF ainda não implementada");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Formato de exportação não suportado: " + format);
        }
    }

    private void gerarExcel(List<Relatorio> relatorios, HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Relatórios");

        // Criar estilo para o cabeçalho
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);

        // Criar estilo para dados
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);

        // Criar linha de cabeçalho com TODOS os campos
        Row headerRow = sheet.createRow(0);
        String[] columns = {
            "ID", "Data Desastre", "Nome Atingido", "CPF", "RG", "Data Nascimento",
            "Endereço", "Bairro", "Cidade", "Complemento",
            "Localização", "Moradia", "Dano Residência", "Estimativa Dano Móveis", "Estimativa Dano Edificação",
            "Ocupação", "Tipo Construção", "Alternativa Moradia", "Observação Imóvel",
            "Nº Total Pessoas", "Menores 0-12", "Menores 13-17", "Maiores 18-59", "Idosos 60+",
            "Possui Necessidades Especiais", "Qtd Necessidades Especiais", "Observação Necessidades",
            "Uso Medicamento Contínuo", "Medicamento", "Possui Desaparecidos", "Qtd Desaparecidos",
            "Qtd Feridos", "Qtd Óbitos",
            "Qtd Água Potável 5L", "Qtd Colchões Solteiro", "Qtd Colchões Casal", "Qtd Cestas Básicas",
            "Qtd Kit Higiene Pessoal", "Qtd Kit Limpeza", "Qtd Móveis", "Qtd Telhas 6mm", "Qtd Telhas 4mm",
            "Qtd Roupas", "Outras Necessidades", "Observação Assistência",
            "Usuário", "Coordenadoria Municipal"
        };

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Preencher dados
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        int rowNum = 1;
        for (Relatorio relatorio : relatorios) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;

            // Dados básicos
            createCell(row, colNum++, relatorio.getId(), dataStyle);
            createCell(row, colNum++, relatorio.getDataDesastre() != null ? relatorio.getDataDesastre().format(dateTimeFormatter) : "", dataStyle);
            createCell(row, colNum++, relatorio.getNomeAtingido(), dataStyle);
            createCell(row, colNum++, relatorio.getCpfAtingido(), dataStyle);
            createCell(row, colNum++, relatorio.getRgAtingido(), dataStyle);
            createCell(row, colNum++, relatorio.getDataNascimentoAtingido() != null ? relatorio.getDataNascimentoAtingido().format(dateTimeFormatter) : "", dataStyle);
            
            // Endereço
            createCell(row, colNum++, relatorio.getEnderecoAtingido(), dataStyle);
            createCell(row, colNum++, relatorio.getBairroAtingido(), dataStyle);
            createCell(row, colNum++, relatorio.getCidadeAtingido(), dataStyle);
            createCell(row, colNum++, relatorio.getComplementoAtingido(), dataStyle);
            
            // Informações do imóvel
            createCell(row, colNum++, relatorio.getLocalizacao(), dataStyle);
            createCell(row, colNum++, relatorio.getMoradia(), dataStyle);
            createCell(row, colNum++, relatorio.getDanoResidencia(), dataStyle);
            createCell(row, colNum++, relatorio.getEstimativaDanoMoveis(), dataStyle);
            createCell(row, colNum++, relatorio.getEstimativaDanoEdificacao(), dataStyle);
            createCell(row, colNum++, relatorio.getOcupacao(), dataStyle);
            createCell(row, colNum++, relatorio.getTipoConstrucao(), dataStyle);
            createCell(row, colNum++, relatorio.getAlternativaMoradia(), dataStyle);
            createCell(row, colNum++, relatorio.getObservacaoImovel(), dataStyle);
            
            // Informações de pessoas
            createCell(row, colNum++, relatorio.getNumeroTotalPessoas(), dataStyle);
            createCell(row, colNum++, relatorio.getMenores0a12(), dataStyle);
            createCell(row, colNum++, relatorio.getMenores13a17(), dataStyle);
            createCell(row, colNum++, relatorio.getMaiores18a59(), dataStyle);
            createCell(row, colNum++, relatorio.getIdosos60mais(), dataStyle);
            createCell(row, colNum++, relatorio.getPossuiNecessidadesEspeciais() != null ? (relatorio.getPossuiNecessidadesEspeciais() ? "Sim" : "Não") : "", dataStyle);
            createCell(row, colNum++, relatorio.getQuantidadeNecessidadesEspeciais(), dataStyle);
            createCell(row, colNum++, relatorio.getObservacaoNecessidades(), dataStyle);
            createCell(row, colNum++, relatorio.getUsoMedicamentoContinuo() != null ? (relatorio.getUsoMedicamentoContinuo() ? "Sim" : "Não") : "", dataStyle);
            createCell(row, colNum++, relatorio.getMedicamento(), dataStyle);
            createCell(row, colNum++, relatorio.getPossuiDesaparecidos() != null ? (relatorio.getPossuiDesaparecidos() ? "Sim" : "Não") : "", dataStyle);
            createCell(row, colNum++, relatorio.getQuantidadeDesaparecidos(), dataStyle);
            createCell(row, colNum++, relatorio.getQuantidadeFeridos(), dataStyle);
            createCell(row, colNum++, relatorio.getQuantidadeObitos(), dataStyle);
            
            // Assistência
            createCell(row, colNum++, relatorio.getQtdAguaPotavel5L(), dataStyle);
            createCell(row, colNum++, relatorio.getQtdColchoesSolteiro(), dataStyle);
            createCell(row, colNum++, relatorio.getQtdColchoesCasal(), dataStyle);
            createCell(row, colNum++, relatorio.getQtdCestasBasicas(), dataStyle);
            createCell(row, colNum++, relatorio.getQtdKitHigienePessoal(), dataStyle);
            createCell(row, colNum++, relatorio.getQtdKitLimpeza(), dataStyle);
            createCell(row, colNum++, relatorio.getQtdMoveis(), dataStyle);
            createCell(row, colNum++, relatorio.getQtdTelhas6mm(), dataStyle);
            createCell(row, colNum++, relatorio.getQtdTelhas4mm(), dataStyle);
            createCell(row, colNum++, relatorio.getQtdRoupas(), dataStyle);
            createCell(row, colNum++, relatorio.getOutrasNecessidades(), dataStyle);
            createCell(row, colNum++, relatorio.getObservacaoAssistencia(), dataStyle);
            
            // Relações
            createCell(row, colNum++, relatorio.getUsuario() != null ? relatorio.getUsuario().getNome() : "", dataStyle);
            createCell(row, colNum++, relatorio.getCoordenadoriaMunicipal() != null ? relatorio.getCoordenadoriaMunicipal().getNome() : "", dataStyle);
        }

        // Auto-ajustar largura das colunas
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Configurar resposta HTTP
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=relatorios_" + System.currentTimeMillis() + ".xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }
}
