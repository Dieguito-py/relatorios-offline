package com.ifscxxe.relatorios_offline.relatorio.dto;

import java.util.ArrayList;
import java.util.List;

public class RelatorioTemplateForm {
    private Long id;
    private String nome;
    private String descricao;
    private Boolean ativo = true;
    private List<RelatorioTemplateCampoForm> campos = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public List<RelatorioTemplateCampoForm> getCampos() {
        return campos;
    }

    public void setCampos(List<RelatorioTemplateCampoForm> campos) {
        this.campos = campos;
    }

    public static class RelatorioTemplateCampoForm {
        private String chave;
        private String rotulo;
        private String tipo;
        private Boolean obrigatorio = false;
        private String opcoes;
        private Integer ordem;

        public String getChave() {
            return chave;
        }

        public void setChave(String chave) {
            this.chave = chave;
        }

        public String getRotulo() {
            return rotulo;
        }

        public void setRotulo(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public Boolean getObrigatorio() {
            return obrigatorio;
        }

        public void setObrigatorio(Boolean obrigatorio) {
            this.obrigatorio = obrigatorio;
        }

        public String getOpcoes() {
            return opcoes;
        }

        public void setOpcoes(String opcoes) {
            this.opcoes = opcoes;
        }

        public Integer getOrdem() {
            return ordem;
        }

        public void setOrdem(Integer ordem) {
            this.ordem = ordem;
        }
    }
}

