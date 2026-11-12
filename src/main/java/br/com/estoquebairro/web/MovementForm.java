package br.com.estoquebairro.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public class MovementForm {
  @NotNull private UUID chave = UUID.randomUUID();

  public UUID getChave() {
    return chave;
  }

  public void setChave(UUID value) {
    chave = value;
  }

  @NotNull private Long produtoId = null;

  public Long getProdutoId() {
    return produtoId;
  }

  public void setProdutoId(Long value) {
    produtoId = value;
  }

  @NotNull
  @Min(1)
  private Integer quantidade = 1;

  public Integer getQuantidade() {
    return quantidade;
  }

  public void setQuantidade(Integer value) {
    quantidade = value;
  }

  @NotBlank private String motivo = "COMPRA";

  public String getMotivo() {
    return motivo;
  }

  public void setMotivo(String value) {
    motivo = value;
  }

  private BigDecimal custo = null;

  public BigDecimal getCusto() {
    return custo;
  }

  public void setCusto(BigDecimal value) {
    custo = value;
  }

  private BigDecimal preco = null;

  public BigDecimal getPreco() {
    return preco;
  }

  public void setPreco(BigDecimal value) {
    preco = value;
  }

  @Size(max = 300)
  private String justificativa = "";

  public String getJustificativa() {
    return justificativa;
  }

  public void setJustificativa(String value) {
    justificativa = value;
  }
}
