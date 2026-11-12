package br.com.estoquebairro.web;

import br.com.estoquebairro.repository.InventoryRepository;
import br.com.estoquebairro.repository.ReportRepository;
import br.com.estoquebairro.repository.ReportRepository.Month;
import br.com.estoquebairro.repository.ReportRepository.Totals;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.ToDoubleFunction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Relatórios de entradas e saídas por período, com gráficos SVG gerados no servidor. */
@Controller
@RequestMapping("/relatorios")
public class ReportController {
  static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Locale BR = Locale.forLanguageTag("pt-BR");
  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM/yy", BR);
  private static final DateTimeFormatter MONTH_LONG = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", BR);
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final ReportRepository reports;
  private final InventoryRepository inventory;

  public ReportController(ReportRepository reports, InventoryRepository inventory) {
    this.reports = reports;
    this.inventory = inventory;
  }

  /** Intervalo escolhido na tela. */
  record Range(String periodo, LocalDate inicio, LocalDate fim, String erro) {
    long dias() {
      return ChronoUnit.DAYS.between(inicio, fim) + 1;
    }
  }

  Range range(String periodo, LocalDate inicio, LocalDate fim) {
    LocalDate today = LocalDate.now(ZONE);
    String p = Set.of("3", "6", "12", "tudo", "personalizado").contains(periodo) ? periodo : "6";
    switch (p) {
      case "tudo" -> {
        LocalDate first = reports.firstMovement().orElse(today);
        return new Range(p, first.isAfter(today) ? today : first.withDayOfMonth(1), today, null);
      }
      case "personalizado" -> {
        LocalDate a = inicio == null ? today.withDayOfMonth(1) : inicio;
        LocalDate b = fim == null ? today : fim;
        if (b.isBefore(a))
          return new Range(
              "6",
              today.minusMonths(5).withDayOfMonth(1),
              today,
              "A data final deve ser igual ou posterior à inicial. Mostrando os últimos 6 meses.");
        if (ChronoUnit.YEARS.between(a, b) >= 20)
          return new Range(
              "6",
              today.minusMonths(5).withDayOfMonth(1),
              today,
              "Escolha um intervalo de até 20 anos. Mostrando os últimos 6 meses.");
        return new Range(p, a, b, null);
      }
      default -> {
        int months = Integer.parseInt(p);
        return new Range(p, today.minusMonths(months - 1L).withDayOfMonth(1), today, null);
      }
    }
  }

  @GetMapping
  public String page(
      @RequestParam(defaultValue = "6") String periodo,
      @RequestParam(required = false) LocalDate inicio,
      @RequestParam(required = false) LocalDate fim,
      Model m) {
    Range r = range(periodo, inicio, fim);
    List<Month> months = reports.monthly(r.inicio(), r.fim());
    Totals t = reports.totals(r.inicio(), r.fim());

    m.addAttribute("periodo", r.periodo());
    m.addAttribute("inicio", r.inicio());
    m.addAttribute("fim", r.fim());
    m.addAttribute("rotulo", DAY.format(r.inicio()) + " a " + DAY.format(r.fim()));
    if (r.erro() != null) m.addAttribute("error", r.erro());
    m.addAttribute("t", t);
    m.addAttribute("months", months);
    m.addAttribute("monthsDesc", months.reversed());

    // Comparação com o período anterior de mesma duração (não se aplica a "todo o período").
    if (!"tudo".equals(r.periodo())) {
      LocalDate prevEnd = r.inicio().minusDays(1);
      LocalDate prevStart = prevEnd.minusDays(r.dias() - 1);
      Totals prev = reports.totals(prevStart, prevEnd);
      m.addAttribute("cmpEntradas", delta(t.entradasValor(), prev.entradasValor()));
      m.addAttribute("cmpVendas", delta(t.vendasValor(), prev.vendasValor()));
      m.addAttribute("cmpPerdas", delta(t.perdasValor(), prev.perdasValor()));
      m.addAttribute("cmpRotulo", DAY.format(prevStart) + " a " + DAY.format(prevEnd));
    }

    // Indicadores derivados.
    m.addAttribute(
        "ticketMedio",
        t.nVendas() == 0
            ? null
            : t.vendasValor().divide(BigDecimal.valueOf(t.nVendas()), 2, RoundingMode.HALF_UP));
    long saidas = t.vendasQtd() + t.perdasQtd();
    m.addAttribute(
        "taxaPerda",
        saidas == 0 ? null : percent(BigDecimal.valueOf(t.perdasQtd()), BigDecimal.valueOf(saidas)));
    m.addAttribute(
        "mediaMensalVendas",
        t.vendasValor().divide(BigDecimal.valueOf(Math.max(1, months.size())), 2, RoundingMode.HALF_UP));
    m.addAttribute(
        "mediaMensalEntradas",
        t.entradasValor()
            .divide(BigDecimal.valueOf(Math.max(1, months.size())), 2, RoundingMode.HALF_UP));
    m.addAttribute("saldoLiquido", t.entradasQtd() - saidas);
    months.stream()
        .filter(x -> x.vendasValor().signum() > 0)
        .max(Comparator.comparing(Month::vendasValor))
        .ifPresent(
            x -> {
              String label = MONTH_LONG.format(x.mes());
              m.addAttribute("melhorMes", Character.toUpperCase(label.charAt(0)) + label.substring(1));
            });
    m.addAttribute(
        "melhorMesValor",
        months.stream().map(Month::vendasValor).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO));
    var agora = java.time.OffsetDateTime.now(ZONE);
    m.addAttribute("estoqueAtual", inventory.summary(agora, agora).get("valor"));

    // Gráficos.
    m.addAttribute(
        "valueChart",
        chart(
            months,
            true,
            List.of(
                new Series("entrada", "Entradas", x -> x.entradasValor().doubleValue()),
                new Series("venda", "Vendas", x -> x.vendasValor().doubleValue()),
                new Series("perda", "Perdas", x -> x.perdasValor().doubleValue()))));
    m.addAttribute(
        "unitChart",
        chart(
            months,
            false,
            List.of(
                new Series("entrada", "Entradas", x -> x.entradasQtd()),
                new Series("venda", "Vendas", x -> x.vendasQtd()),
                new Series("perda", "Perdas", x -> x.perdasQtd()))));

    m.addAttribute("topVendas", reports.top(r.inicio(), r.fim(), "VENDA", 10));
    m.addAttribute("topPerdas", reports.top(r.inicio(), r.fim(), "PERDA", 5));
    m.addAttribute("topEntradas", reports.top(r.inicio(), r.fim(), "COMPRA", 5));
    m.addAttribute("categorias", reports.byCategory(r.inicio(), r.fim()));
    m.addAttribute("usuarios", reports.byUser(r.inicio(), r.fim()));
    return "reports";
  }

  /** Exporta a tabela mensal em CSV (separador ';' e vírgula decimal, compatível com Excel). */
  @GetMapping("/exportar")
  public ResponseEntity<byte[]> export(
      @RequestParam(defaultValue = "6") String periodo,
      @RequestParam(required = false) LocalDate inicio,
      @RequestParam(required = false) LocalDate fim) {
    Range r = range(periodo, inicio, fim);
    StringBuilder csv = new StringBuilder("\uFEFF");
    csv.append(
        "Mês;Entradas (UN);Entradas (R$);Vendas (UN);Vendas (R$);Perdas (UN);Perdas (R$);"
            + "Saldo líquido (UN);Movimentações\r\n");
    for (Month x : reports.monthly(r.inicio(), r.fim())) {
      csv.append(DateTimeFormatter.ofPattern("MM/yyyy").format(x.mes()))
          .append(';')
          .append(x.entradasQtd())
          .append(';')
          .append(decimal(x.entradasValor()))
          .append(';')
          .append(x.vendasQtd())
          .append(';')
          .append(decimal(x.vendasValor()))
          .append(';')
          .append(x.perdasQtd())
          .append(';')
          .append(decimal(x.perdasValor()))
          .append(';')
          .append(x.saldoLiquido())
          .append(';')
          .append(x.movimentacoes())
          .append("\r\n");
    }
    String name = "relatorio-estoque_" + r.inicio() + "_a_" + r.fim() + ".csv";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .body(csv.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String decimal(BigDecimal v) {
    return v.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
  }

  static BigDecimal percent(BigDecimal part, BigDecimal whole) {
    return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
  }

  /** Variação percentual; nulo quando não há base de comparação. */
  public record Delta(BigDecimal percent, boolean up, boolean semBase) {}

  static Delta delta(BigDecimal now, BigDecimal before) {
    if (before.signum() == 0) return new Delta(null, now.signum() > 0, true);
    BigDecimal p = percent(now.subtract(before), before);
    return new Delta(p.abs(), p.signum() >= 0, false);
  }

  // ---------- Gráfico de barras agrupadas em SVG ----------

  record Series(String kind, String label, ToDoubleFunction<Month> value) {}

  public record Bar(double x, double y, double w, double h, String kind, String tip) {}

  public record Grid(double y, String label) {}

  public record Label(double x, String text) {}

  public record Chart(
      int width, int height, double left, double right, double base, List<Grid> grid,
      List<Bar> bars, List<Label> labels, boolean empty) {}

  static Chart chart(List<Month> months, boolean money, List<Series> series) {
    final double left = money ? 78 : 52, right = 16, top = 14, bottom = 34, plotH = 220;
    int n = Math.max(1, months.size());
    double group = Math.max(30, Math.min(110, (720 - left - right) / n));
    int width = (int) Math.ceil(left + right + group * n);
    int height = (int) (top + plotH + bottom);
    double max = 0;
    for (Month x : months) for (Series s : series) max = Math.max(max, s.value().applyAsDouble(x));
    double step = niceStep(max <= 0 ? 1 : max / 4);
    double ceil = Math.max(step, Math.ceil(max / step) * step);
    double base = top + plotH;

    List<Grid> grid = new ArrayList<>();
    for (double v = 0; v <= ceil + step / 2; v += step)
      grid.add(new Grid(base - v / ceil * plotH, axis(v, money)));

    List<Bar> bars = new ArrayList<>();
    List<Label> labels = new ArrayList<>();
    double barW = Math.min(18, (group - 10) / series.size());
    int every = n <= 14 ? 1 : (int) Math.ceil(n / 12.0);
    for (int i = 0; i < months.size(); i++) {
      Month x = months.get(i);
      double gx = left + group * i;
      double start = gx + (group - barW * series.size()) / 2;
      for (int j = 0; j < series.size(); j++) {
        Series s = series.get(j);
        double v = s.value().applyAsDouble(x);
        double h = v / ceil * plotH;
        if (v > 0 && h < 1.5) h = 1.5;
        String tip =
            s.label()
                + " em "
                + MONTH_LONG.format(x.mes())
                + ": "
                + (money ? money(v) : String.format(BR, "%,.0f UN", v));
        bars.add(new Bar(round(start + barW * j), round(base - h), round(barW - 2), round(h), s.kind(), tip));
      }
      if (i % every == 0) labels.add(new Label(round(gx + group / 2), MONTH.format(x.mes()).replace(".", "")));
    }
    boolean empty = max <= 0;
    return new Chart(width, height, left, right, base, grid, bars, labels, empty);
  }

  private static double round(double v) {
    return Math.round(v * 10) / 10.0;
  }

  static double niceStep(double raw) {
    double mag = Math.pow(10, Math.floor(Math.log10(raw)));
    double f = raw / mag;
    double nice = f <= 1 ? 1 : f <= 2 ? 2 : f <= 2.5 ? 2.5 : f <= 5 ? 5 : 10;
    return Math.max(nice * mag, 1);
  }

  private static String axis(double v, boolean money) {
    String n;
    if (v >= 1_000_000) n = String.format(BR, "%,.1f mi", v / 1_000_000).replace(",0 ", " ");
    else if (v >= 1_000) n = String.format(BR, "%,.1f mil", v / 1_000).replace(",0 ", " ");
    else n = String.format(BR, "%,.0f", v);
    return money ? "R$ " + n : n;
  }

  private static String money(double v) {
    return java.text.NumberFormat.getCurrencyInstance(BR).format(v);
  }
}
