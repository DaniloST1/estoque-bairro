package br.com.estoquebairro.service;

import java.time.*;

public record Period(LocalDate inicio, LocalDate fim) {
  public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

  public Period {
    if (inicio == null || fim == null || fim.isBefore(inicio))
      throw new BusinessException("fim", "O fim deve ser igual ou posterior ao início.");
  }

  public OffsetDateTime from() {
    return inicio.atStartOfDay(ZONE).toOffsetDateTime();
  }

  public OffsetDateTime until() {
    return fim.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
  }

  public static Period current() {
    LocalDate now = LocalDate.now(ZONE);
    return new Period(now.withDayOfMonth(1), now);
  }
}
