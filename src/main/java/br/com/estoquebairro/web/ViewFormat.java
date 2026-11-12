package br.com.estoquebairro.web;

import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("view")
public class ViewFormat {
  public String money(Number value) {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
        .format(value == null ? 0 : value);
  }

  /** Número inteiro com separador de milhar (1.234). */
  public String number(Number value) {
    return NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))
        .format(value == null ? 0 : value);
  }

  /** Número com uma casa decimal (12,5). */
  public String decimal(Number value) {
    var f = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
    f.setMinimumFractionDigits(1);
    f.setMaximumFractionDigits(1);
    return f.format(value == null ? 0 : value);
  }

  /** Valor unitário × quantidade, formatado em reais. */
  public String total(java.math.BigDecimal unit, int quantity) {
    return money(unit == null ? null : unit.multiply(java.math.BigDecimal.valueOf(quantity)));
  }

  /** Data curta dd/MM/yyyy HH:mm. */
  public String shortDate(OffsetDateTime value) {
    return value
        .atZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
  }

  /** Mês por extenso abreviado (nov/2026). */
  public String month(LocalDate value) {
    return value
        .format(DateTimeFormatter.ofPattern("MMM/yyyy", Locale.forLanguageTag("pt-BR")))
        .replace(".", "");
  }

  public String date(OffsetDateTime value) {
    return value
        .atZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
  }
}
