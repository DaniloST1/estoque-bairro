package br.com.estoquebairro.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductForm {
  @NotBlank
  @Size(max = 40)
  private String sku = "";

  public String getSku() {
    return sku;
  }

  public void setSku(String value) {
    sku = value;
  }

  @NotBlank
  @Size(max = 120)
  private String nome = "";

  public String getNome() {
    return nome;
  }

  public void setNome(String value) {
    nome = value;
  }

  @NotNull private Long categoriaId = null;

  public Long getCategoriaId() {
    return categoriaId;
  }

  public void setCategoriaId(Long value) {
    categoriaId = value;
  }

  private BigDecimal custo = null;

  public BigDecimal getCusto() {
    return custo;
  }

  public void setCusto(BigDecimal value) {
    custo = value;
  }

  @NotNull
  @DecimalMin("0.01")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal preco = null;

  public BigDecimal getPreco() {
    return preco;
  }

  public void setPreco(BigDecimal value) {
    preco = value;
  }

  @NotNull
  @Min(0)
  private Integer minimo = 5;

  public Integer getMinimo() {
    return minimo;
  }

  public void setMinimo(Integer value) {
    minimo = value;
  }

  private boolean ativo = true;

  public boolean getAtivo() {
    return ativo;
  }

  public void setAtivo(boolean value) {
    ativo = value;
  }
}
