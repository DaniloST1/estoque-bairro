package br.com.estoquebairro.web;

import jakarta.validation.constraints.*;

public class UserForm {
  @NotBlank
  @Size(max = 100)
  private String nome = "";

  public String getNome() {
    return nome;
  }

  public void setNome(String value) {
    nome = value;
  }

  @Size(max = 60)
  private String login = "";

  public String getLogin() {
    return login;
  }

  public void setLogin(String value) {
    login = value;
  }

  private String senha = "";

  public String getSenha() {
    return senha;
  }

  public void setSenha(String value) {
    senha = value;
  }

  @NotBlank private String perfil = "OPERADOR";

  public String getPerfil() {
    return perfil;
  }

  public void setPerfil(String value) {
    perfil = value;
  }

  private boolean ativo = true;

  public boolean getAtivo() {
    return ativo;
  }

  public void setAtivo(boolean value) {
    ativo = value;
  }
}
