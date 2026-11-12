package br.com.estoquebairro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Rules {
  private Rules() {}

  public static String text(String v, String field, int max) {
    if (v == null || v.isBlank() || v.strip().length() > max)
      throw new BusinessException(field, "Informe um valor de 1 a " + max + " caracteres.");
    return v.strip();
  }

  public static String normalized(String v, String field, int max) {
    return text(v, field, max).toUpperCase(Locale.ROOT);
  }

  public static BigDecimal money(BigDecimal v, String field) {
    if (v == null || v.signum() <= 0 || v.compareTo(new BigDecimal("9999999999.99")) > 0)
      throw new BusinessException(field, "Informe um valor positivo de até R$ 9.999.999.999,99.");
    try {
      return v.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new BusinessException(field, "Use no máximo duas casas decimais.");
    }
  }
}
