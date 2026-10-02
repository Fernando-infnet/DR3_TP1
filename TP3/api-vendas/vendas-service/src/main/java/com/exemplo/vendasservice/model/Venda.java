package com.exemplo.vendasservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de venda (Spring Data R2DBC):
 * ID_VENDA | ID_PRODUTO | QUANTIDADE | VALOR_PRODUTO (+ valor total, usuario e data).
 * O VALOR_PRODUTO e copiado do produtos-service no momento da venda.
 */
@Table("venda")
public class Venda {

    @Id
    @Column("id_venda")
    private Long idVenda;

    @Column("id_produto")
    private Long idProduto;

    private Integer quantidade;

    @Column("valor_produto")
    private BigDecimal valorProduto;

    @Column("valor_total")
    private BigDecimal valorTotal;

    /** Usuario autenticado que registrou a venda (claim "sub" do JWT). */
    private String usuario;

    @Column("data_venda")
    private LocalDateTime dataVenda;

    public Venda() {
    }

    public Venda(Long idProduto, Integer quantidade, BigDecimal valorProduto, String usuario) {
        this.idProduto = idProduto;
        this.quantidade = quantidade;
        this.valorProduto = valorProduto;
        this.valorTotal = valorProduto.multiply(BigDecimal.valueOf(quantidade));
        this.usuario = usuario;
        this.dataVenda = LocalDateTime.now();
    }

    public Long getIdVenda() {
        return idVenda;
    }

    public void setIdVenda(Long idVenda) {
        this.idVenda = idVenda;
    }

    public Long getIdProduto() {
        return idProduto;
    }

    public void setIdProduto(Long idProduto) {
        this.idProduto = idProduto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValorProduto() {
        return valorProduto;
    }

    public void setValorProduto(BigDecimal valorProduto) {
        this.valorProduto = valorProduto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getDataVenda() {
        return dataVenda;
    }

    public void setDataVenda(LocalDateTime dataVenda) {
        this.dataVenda = dataVenda;
    }
}
