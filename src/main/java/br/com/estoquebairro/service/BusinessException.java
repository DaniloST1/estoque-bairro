package br.com.estoquebairro.service;

public class BusinessException extends RuntimeException {
  private final String field;

  public BusinessException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String field() {
    return field;
  }
}
