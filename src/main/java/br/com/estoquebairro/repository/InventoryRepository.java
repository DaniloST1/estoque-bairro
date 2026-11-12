package br.com.estoquebairro.repository;

import br.com.estoquebairro.model.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryRepository {
  private final JdbcTemplate db;

  public InventoryRepository(JdbcTemplate db) {
    this.db = db;
  }

  private static final String PRODUCTS =
      "SELECT p.*,c.nome categoria FROM produto p JOIN categoria c ON c.id=p.categoria_id ";
  private static final RowMapper<Product> PRODUCT =
      (r, n) ->
          new Product(
              r.getLong("id"),
              r.getString("sku"),
              r.getString("nome"),
              r.getLong("categoria_id"),
              r.getString("categoria"),
              r.getBigDecimal("custo_referencia"),
              r.getBigDecimal("preco_venda"),
              r.getInt("estoque_minimo"),
              r.getInt("saldo"),
              r.getBoolean("ativo"));
  private static final String MOVEMENTS =
      "SELECT m.*,p.nome produto,p.sku,u.nome usuario FROM movimentacao m JOIN produto p ON"
          + " p.id=m.produto_id JOIN usuario u ON u.id=m.usuario_id ";
  private static final RowMapper<Movement> MOVEMENT =
      (r, n) ->
          new Movement(
              r.getLong("id"),
              r.getLong("produto_id"),
              r.getString("produto"),
              r.getString("sku"),
              r.getString("usuario"),
              r.getString("tipo"),
              r.getString("motivo"),
              r.getInt("quantidade"),
              r.getBigDecimal("custo_unitario"),
              r.getBigDecimal("preco_venda_unitario"),
              r.getObject("data_hora", OffsetDateTime.class),
              r.getString("justificativa"),
              r.getString("assinatura"),
              r.getInt("saldo_apos"));

  public Optional<Product> product(long id, boolean lock) {
    return db
        .query(PRODUCTS + "WHERE p.id=?" + (lock ? " FOR UPDATE OF p" : ""), PRODUCT, id)
        .stream()
        .findFirst();
  }

  /** Produtos ativos para seleção nos formulários de movimentação. */
  public List<Product> activeProducts() {
    return db.query(PRODUCTS + "WHERE p.ativo ORDER BY p.nome,p.id", PRODUCT);
  }

  /** Últimas movimentações de um tipo (ENTRADA ou SAIDA). */
  public List<Movement> recent(String type, int limit) {
    return db.query(
        MOVEMENTS + "WHERE m.tipo=? ORDER BY m.data_hora DESC,m.id DESC LIMIT ?",
        MOVEMENT,
        type,
        limit);
  }

  public List<Category> categories() {
    return db.query(
        "SELECT id,nome FROM categoria ORDER BY nome",
        (r, n) -> new Category(r.getLong(1), r.getString(2)));
  }

  public boolean categoryExists(long id) {
    return Boolean.TRUE.equals(
        db.queryForObject("SELECT EXISTS(SELECT 1 FROM categoria WHERE id=?)", Boolean.class, id));
  }

  public long addCategory(String name, String normalized) {
    return db.queryForObject(
        "INSERT INTO categoria(nome,nome_normalizado) VALUES (?,?) RETURNING id",
        Long.class,
        name,
        normalized);
  }

  public int editCategory(long id, String name, String normalized) {
    return db.update(
        "UPDATE categoria SET nome=?,nome_normalizado=? WHERE id=?", name, normalized, id);
  }

  public long addProduct(
      String sku, String name, long category, BigDecimal cost, BigDecimal price, int minimum) {
    return db.queryForObject(
        "INSERT INTO produto(sku,nome,categoria_id,custo_referencia,preco_venda,estoque_minimo)"
            + " VALUES (?,?,?,?,?,?) RETURNING id",
        Long.class,
        sku,
        name,
        category,
        cost,
        price,
        minimum);
  }

  public void editProduct(
      long id,
      String sku,
      String name,
      long category,
      BigDecimal price,
      int minimum,
      boolean active) {
    db.update(
        "UPDATE produto SET sku=?,nome=?,categoria_id=?,preco_venda=?,estoque_minimo=?,ativo=?"
            + " WHERE id=?",
        sku,
        name,
        category,
        price,
        minimum,
        active,
        id);
  }

  public void stock(long id, int balance, BigDecimal cost) {
    db.update("UPDATE produto SET saldo=?,custo_referencia=? WHERE id=?", balance, cost, id);
  }

  public void lockRequest(UUID key) {
    db.queryForObject(
        "SELECT pg_advisory_xact_lock(hashtextextended(?,0))", Object.class, key.toString());
  }

  public Optional<Movement> byKey(UUID key) {
    return db.query(MOVEMENTS + "WHERE m.chave_requisicao=?", MOVEMENT, key).stream().findFirst();
  }

  public Optional<Movement> movement(long id) {
    return db.query(MOVEMENTS + "WHERE m.id=?", MOVEMENT, id).stream().findFirst();
  }

  public long insertMovement(
      MovementCommand c, long user, BigDecimal cost, BigDecimal price, String hash, int balance) {
    return db.queryForObject(
        "INSERT INTO"
            + " movimentacao(produto_id,usuario_id,tipo,motivo,quantidade,custo_unitario,preco_venda_unitario,justificativa,chave_requisicao,assinatura,saldo_apos)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?) RETURNING id",
        Long.class,
        c.produtoId(),
        user,
        c.motivo().equals("COMPRA") ? "ENTRADA" : "SAIDA",
        c.motivo(),
        c.quantidade(),
        cost,
        price,
        c.justificativa(),
        c.chave(),
        hash,
        balance);
  }

  public record ProductPage(List<Product> items, long total, int page) {
    public long pages() {
      return Math.max(1, (total + 19) / 20);
    }
  }

  public ProductPage search(String query, Long category, String status, boolean low, int page) {
    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> args = new ArrayList<>();
    if (query != null && !query.isBlank()) {
      where.append(" AND (p.nome ILIKE ? ESCAPE '!' OR p.sku ILIKE ? ESCAPE '!')");
      String q = "%" + query.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
      args.add(q);
      args.add(q);
    }
    if (category != null) {
      where.append(" AND p.categoria_id=?");
      args.add(category);
    }
    if ("ativo".equals(status) || "inativo".equals(status)) {
      where.append(" AND p.ativo=?");
      args.add("ativo".equals(status));
    }
    if (low) where.append(" AND p.ativo AND p.saldo<=p.estoque_minimo");
    long total =
        db.queryForObject("SELECT count(*) FROM produto p" + where, Long.class, args.toArray());
    int safe = (int) Math.max(0, Math.min(page, Math.max(0, (total - 1) / 20)));
    args.add(20);
    args.add(safe * 20);
    return new ProductPage(
        db.query(
            PRODUCTS + where + " ORDER BY p.nome,p.id LIMIT ? OFFSET ?", PRODUCT, args.toArray()),
        total,
        safe);
  }

  public record HistoryPage(List<Movement> items, long total, int page) {
    public long pages() {
      return Math.max(1, (total + 19) / 20);
    }
  }

  public HistoryPage history(
      OffsetDateTime from, OffsetDateTime to, Long product, String type, int page) {
    String where = " WHERE m.data_hora>=? AND m.data_hora<?";
    List<Object> a = new ArrayList<>(List.of(from, to));
    if (product != null) {
      where += " AND m.produto_id=?";
      a.add(product);
    }
    if ("ENTRADA".equals(type) || "SAIDA".equals(type)) {
      where += " AND m.tipo=?";
      a.add(type);
    }
    long count =
        db.queryForObject("SELECT count(*) FROM movimentacao m" + where, Long.class, a.toArray());
    int safe = (int) Math.max(0, Math.min(page, Math.max(0, (count - 1) / 20)));
    a.add(20);
    a.add(safe * 20);
    return new HistoryPage(
        db.query(
            MOVEMENTS + where + " ORDER BY m.data_hora DESC,m.id DESC LIMIT ? OFFSET ?",
            MOVEMENT,
            a.toArray()),
        count,
        safe);
  }

  public Map<String, Object> summary(OffsetDateTime from, OffsetDateTime to) {
    Map<String, Object> result =
        new LinkedHashMap<>(
            db.queryForMap(
                "SELECT count(*) FILTER(WHERE ativo) ativos,count(*) FILTER(WHERE ativo AND"
                    + " saldo<=estoque_minimo) baixos,coalesce(sum(saldo*custo_referencia),0) valor"
                    + " FROM produto"));
    result.putAll(
        db.queryForMap(
            "SELECT coalesce(sum(quantidade*custo_unitario) FILTER(WHERE motivo='COMPRA'),0)"
                + " entradas,coalesce(sum(quantidade*preco_venda_unitario) FILTER(WHERE"
                + " motivo='VENDA'),0) vendas,coalesce(sum(quantidade*custo_unitario) FILTER(WHERE"
                + " motivo='PERDA'),0) perdas FROM movimentacao WHERE data_hora>=? AND data_hora<?",
            from,
            to));
    return result;
  }

  public List<Map<String, Object>> quantities(OffsetDateTime from, OffsetDateTime to) {
    return db.queryForList(
        "SELECT p.id,p.sku,p.nome,coalesce(sum(m.quantidade) FILTER(WHERE m.motivo='COMPRA'),0)"
            + " entradas,coalesce(sum(m.quantidade) FILTER(WHERE m.motivo='VENDA'),0)"
            + " vendas,coalesce(sum(m.quantidade) FILTER(WHERE m.motivo='PERDA'),0) perdas FROM"
            + " movimentacao m JOIN produto p ON p.id=m.produto_id WHERE m.data_hora>=? AND"
            + " m.data_hora<? GROUP BY p.id ORDER BY p.nome,p.id",
        from,
        to);
  }
}
