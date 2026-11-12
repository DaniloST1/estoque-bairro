package br.com.estoquebairro.web;

import jakarta.validation.constraints.*;

public class CategoryForm {
  @NotBlank
  @Size(max = 80)
  private String nome = "";

  public String getNome() {
    return nome;
  }

  public void setNome(String value) {
    nome = value;
  }
}
