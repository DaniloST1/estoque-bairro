package br.com.estoquebairro.model;

public record Account(
    long id,
    String nome,
    String login,
    String senhaHash,
    String perfil,
    boolean ativo,
    long versao) {}
