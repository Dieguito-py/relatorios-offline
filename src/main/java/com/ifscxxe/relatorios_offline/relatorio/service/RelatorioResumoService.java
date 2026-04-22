package com.ifscxxe.relatorios_offline.relatorio.service;

import com.ifscxxe.relatorios_offline.relatorio.model.CadastroFamilia;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class RelatorioResumoService {

    public int calcularTotalVitimas(List<CadastroFamilia> relatorios) {
        return somarCampo(relatorios, CadastroFamilia::getNumeroTotalPessoas);
    }

    public int calcularTotalFeridos(List<CadastroFamilia> relatorios) {
        return somarCampo(relatorios, CadastroFamilia::getQuantidadeFeridos);
    }

    public int calcularTotalObitos(List<CadastroFamilia> relatorios) {
        return somarCampo(relatorios, CadastroFamilia::getQuantidadeObitos);
    }

    public int calcularTotalDesaparecidos(List<CadastroFamilia> relatorios) {
        return somarCampo(relatorios, CadastroFamilia::getQuantidadeDesaparecidos);
    }

    public Map<String, Integer> calcularTotaisSuprimentos(List<CadastroFamilia> relatorios) {
        Map<String, Integer> totais = new LinkedHashMap<>();
        totais.put("Agua potavel 5L", somarCampo(relatorios, CadastroFamilia::getQtdAguaPotavel5L));
        totais.put("Colchoes solteiro", somarCampo(relatorios, CadastroFamilia::getQtdColchoesSolteiro));
        totais.put("Colchoes casal", somarCampo(relatorios, CadastroFamilia::getQtdColchoesCasal));
        totais.put("Cestas basicas", somarCampo(relatorios, CadastroFamilia::getQtdCestasBasicas));
        totais.put("Kit higiene pessoal", somarCampo(relatorios, CadastroFamilia::getQtdKitHigienePessoal));
        totais.put("Kit limpeza", somarCampo(relatorios, CadastroFamilia::getQtdKitLimpeza));
        totais.put("Moveis", somarCampo(relatorios, CadastroFamilia::getQtdMoveis));
        totais.put("Telhas 6mm", somarCampo(relatorios, CadastroFamilia::getQtdTelhas6mm));
        totais.put("Telhas 4mm", somarCampo(relatorios, CadastroFamilia::getQtdTelhas4mm));
        totais.put("Roupas", somarCampo(relatorios, CadastroFamilia::getQtdRoupas));
        return totais;
    }

    public Map<String, Object> montarParametrosResumo(List<CadastroFamilia> relatorios) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("TOTAL_VITIMAS", calcularTotalVitimas(relatorios));
        parametros.put("TOTAL_DESAPARECIDOS", calcularTotalDesaparecidos(relatorios));
        parametros.put("TOTAL_FERIDOS", calcularTotalFeridos(relatorios));
        parametros.put("TOTAL_OBITOS", calcularTotalObitos(relatorios));
        parametros.put("TOTAL_SUPRIMENTO_AGUA_5L", somarCampo(relatorios, CadastroFamilia::getQtdAguaPotavel5L));
        parametros.put("TOTAL_SUPRIMENTO_COLCHOES_SOLTEIRO", somarCampo(relatorios, CadastroFamilia::getQtdColchoesSolteiro));
        parametros.put("TOTAL_SUPRIMENTO_COLCHOES_CASAL", somarCampo(relatorios, CadastroFamilia::getQtdColchoesCasal));
        parametros.put("TOTAL_SUPRIMENTO_CESTAS_BASICAS", somarCampo(relatorios, CadastroFamilia::getQtdCestasBasicas));
        parametros.put("TOTAL_SUPRIMENTO_KIT_HIGIENE", somarCampo(relatorios, CadastroFamilia::getQtdKitHigienePessoal));
        parametros.put("TOTAL_SUPRIMENTO_KIT_LIMPEZA", somarCampo(relatorios, CadastroFamilia::getQtdKitLimpeza));
        parametros.put("TOTAL_SUPRIMENTO_MOVEIS", somarCampo(relatorios, CadastroFamilia::getQtdMoveis));
        parametros.put("TOTAL_SUPRIMENTO_TELHAS_6MM", somarCampo(relatorios, CadastroFamilia::getQtdTelhas6mm));
        parametros.put("TOTAL_SUPRIMENTO_TELHAS_4MM", somarCampo(relatorios, CadastroFamilia::getQtdTelhas4mm));
        parametros.put("TOTAL_SUPRIMENTO_ROUPAS", somarCampo(relatorios, CadastroFamilia::getQtdRoupas));
        return parametros;
    }

    private int somarCampo(List<CadastroFamilia> relatorios, Function<CadastroFamilia, Integer> extrator) {
        if (relatorios == null || relatorios.isEmpty()) {
            return 0;
        }
        return relatorios.stream()
                .map(extrator)
                .filter(valor -> valor != null && valor > 0)
                .mapToInt(Integer::intValue)
                .sum();
    }
}


