package br.com.estoquebairro.repository;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas somente leitura usadas pela página de relatórios. Todas as datas usam São Paulo. */
@Repository
public class ReportRepository {
  private final JdbcTemplate db;

  public ReportRepository(JdbcTemplate db) {
    this.db = db;
  }

  /** Linha mensal: valores em reais e quantidades em unidades. */
  public record Month(
      LocalDate mes,
      long entradasQtd,
      BigDecimal entradasValor,
      long vendasQtd,
      BigDecimal vendasValor,
      long perdasQtd,
      BigDecimal perdasValor,
      long movimentacoes) {
    public long saidasQtd() {
      return vendasQtd + perdasQtd;
    }

    public long saldoLiquido() {
      return entradasQtd - vendasQtd - perdasQtd;
    }
  }

  /** Totais do período. */
  public record Totals(
      long entradasQtd,
      BigDecimal entradasValor,
      long vendasQtd,
      BigDecimal vendasValor,
      long perdasQtd,
      BigDecimal perdasValor,
      long movimentacoes,
      long nEntradas,
      long nVendas,
      long nPerdas,
      long produtosMovimentados) {}

  public record Ranking(String nome, String sku, long quantidade, BigDecimal valor) {}

  public record CategoryRow(
      String categoria,
      long entradasQtd,
      BigDecimal entradasValor,
      long vendasQtd,
      BigDecimal vendasValor,
      long perdasQtd,
      BigDecimal perdasValor) {}

  public record UserRow(String usuario, long entradas, long vendas, long perdas, long total) {}

  private static final String SUMS =
      "coalesce(sum(quantidade) FILTER(WHERE motivo='COMPRA'),0) eq,"
          + " coalesce(sum(quantidade*custo_unitario) FILTER(WHERE motivo='COMPRA'),0) ev,"
          + " coalesce(sum(quantidade) FILTER(WHERE motivo='VENDA'),0) vq,"
          + " coalesce(sum(quantidade*preco_venda_unitario) FILTER(WHERE motivo='VENDA'),0) vv,"
          + " coalesce(sum(quantidade) FILTER(WHERE motivo='PERDA'),0) pq,"
          + " coalesce(sum(quantidade*custo_unitario) FILTER(WHERE motivo='PERDA'),0) pv";

  /** Data (em São Paulo) da primeira movimentação registrada, se houver. */
  public Optional<LocalDate> firstMovement() {
    return Optional.ofNullable(
        db.queryForObject(
            "SELECT min((data_hora AT TIME ZONE 'America/Sao_Paulo')::date) FROM movimentacao",
            LocalDate.class));
  }

  /** Um registro por mês do intervalo, inclusive os meses sem movimentação. */
  public List<Month> monthly(LocalDate inicio, LocalDate fim) {
    return db.query(
        "WITH meses AS (SELECT generate_series(date_trunc('month', CAST(? AS date)),"
            + " date_trunc('month', CAST(? AS date)), interval '1 month')::date mes),"
            + " mov AS (SELECT date_trunc('month', data_hora AT TIME ZONE"
            + " 'America/Sao_Paulo')::date mes, motivo, quantidade, custo_unitario,"
            + " preco_venda_unitario FROM movimentacao WHERE data_hora>=? AND data_hora<?)"
            + " SELECT m.mes, "
            + SUMS
            + ", count(mov.motivo) n FROM meses m LEFT JOIN mov ON mov.mes=m.mes"
            + " GROUP BY m.mes ORDER BY m.mes",
        (r, n) ->
            new Month(
                r.getObject("mes", LocalDate.class),
                r.getLong("eq"),
                r.getBigDecimal("ev"),
                r.getLong("vq"),
                r.getBigDecimal("vv"),
                r.getLong("pq"),
                r.getBigDecimal("pv"),
                r.getLong("n")),
        inicio,
        fim,
        start(inicio),
        end(fim));
  }

  public Totals totals(LocalDate inicio, LocalDate fim) {
    return db.queryForObject(
        "SELECT "
            + SUMS
            + ", count(*) n, count(*) FILTER(WHERE motivo='COMPRA') ne,"
            + " count(*) FILTER(WHERE motivo='VENDA') nv, count(*) FILTER(WHERE motivo='PERDA') np,"
            + " count(DISTINCT produto_id) prod FROM movimentacao WHERE data_hora>=? AND"
            + " data_hora<?",
        (r, n) ->
            new Totals(
                r.getLong("eq"),
                r.getBigDecimal("ev"),
                r.getLong("vq"),
                r.getBigDecimal("vv"),
                r.getLong("pq"),
                r.getBigDecimal("pv"),
                r.getLong("n"),
                r.getLong("ne"),
                r.getLong("nv"),
                r.getLong("np"),
                r.getLong("prod")),
        start(inicio),
        end(fim));
  }

  /** Produtos com mais unidades no motivo informado (COMPRA, VENDA ou PERDA). */
  public List<Ranking> top(LocalDate inicio, LocalDate fim, String motivo, int limit) {
    String value = "VENDA".equals(motivo) ? "m.preco_venda_unitario" : "m.custo_unitario";
    return db.query(
        "SELECT p.nome, p.sku, sum(m.quantidade) q, sum(m.quantidade*"
            + value
            + ") v FROM movimentacao m JOIN produto p ON p.id=m.produto_id WHERE m.motivo=? AND"
            + " m.data_hora>=? AND m.data_hora<? GROUP BY p.id ORDER BY q DESC, v DESC, p.nome"
            + " LIMIT ?",
        (r, n) -> new Ranking(r.getString(1), r.getString(2), r.getLong(3), r.getBigDecimal(4)),
        motivo,
        start(inicio),
        end(fim),
        limit);
  }

  public List<CategoryRow> byCategory(LocalDate inicio, LocalDate fim) {
    return db.query(
        "SELECT c.nome, "
            + SUMS
            + " FROM movimentacao m JOIN produto p ON p.id=m.produto_id JOIN categoria c ON"
            + " c.id=p.categoria_id WHERE m.data_hora>=? AND m.data_hora<? GROUP BY c.id, c.nome"
            + " ORDER BY vv DESC, ev DESC, c.nome",
        (r, n) ->
            new CategoryRow(
                r.getString(1),
                r.getLong("eq"),
                r.getBigDecimal("ev"),
                r.getLong("vq"),
                r.getBigDecimal("vv"),
                r.getLong("pq"),
                r.getBigDecimal("pv")),
        start(inicio),
        end(fim));
  }

  public List<UserRow> byUser(LocalDate inicio, LocalDate fim) {
    return db.query(
        "SELECT u.nome, count(*) FILTER(WHERE m.motivo='COMPRA') e,"
            + " count(*) FILTER(WHERE m.motivo='VENDA') v, count(*) FILTER(WHERE"
            + " m.motivo='PERDA') p, count(*) t FROM movimentacao m JOIN usuario u ON"
            + " u.id=m.usuario_id WHERE m.data_hora>=? AND m.data_hora<? GROUP BY u.id, u.nome"
            + " ORDER BY t DESC, u.nome",
        (r, n) ->
            new UserRow(r.getString(1), r.getLong(2), r.getLong(3), r.getLong(4), r.getLong(5)),
        start(inicio),
        end(fim));
  }

  private static OffsetDateTime start(LocalDate d) {
    return d.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toOffsetDateTime();
  }

  private static OffsetDateTime end(LocalDate d) {
    return d.plusDays(1).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toOffsetDateTime();
  }
}
