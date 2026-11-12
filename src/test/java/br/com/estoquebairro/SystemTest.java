package br.com.estoquebairro;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.estoquebairro.config.SessionUser;
import br.com.estoquebairro.model.*;
import br.com.estoquebairro.repository.*;
import br.com.estoquebairro.service.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;

@SpringBootTest(
    properties = {"app.bootstrap.password=SomenteTeste_12345", "app.demo.enabled=false"})
@AutoConfigureMockMvc
class SystemTest {
  static final EmbeddedPostgres PG = start();

  static EmbeddedPostgres start() {
    try {
      return EmbeddedPostgres.builder()
          .setServerConfig("unix_socket_directories", "")
          .setPort(0)
          .start();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
    r.add("spring.datasource.username", () -> "postgres");
    r.add("spring.datasource.password", () -> "");
  }

  static final String PASSWORD = "Apenas_fixture_12345";
  static final String HASH = new BCryptPasswordEncoder(4).encode(PASSWORD);
  @Autowired InventoryService service;
  @Autowired InventoryRepository repo;
  @Autowired UserService users;
  @Autowired UserRepository accounts;
  @Autowired JdbcTemplate db;
  @Autowired MockMvc mvc;
  long cat, product, admin, operator;

  @BeforeEach
  void setup() {
    db.execute("DROP TRIGGER IF EXISTS falha_teste ON movimentacao");
    db.execute("TRUNCATE movimentacao,produto,categoria,usuario RESTART IDENTITY CASCADE");
    admin = accounts.add("Administrador teste", "admin", HASH, "ADMIN", true);
    operator = accounts.add("Operador teste", "operador", HASH, "OPERADOR", true);
    cat = service.category(null, "Alimentos");
    product =
        service.saveProduct(
            null, " sku-001 ", "Arroz", cat, new BigDecimal("20"), new BigDecimal("28"), 5, true);
  }

  SessionUser principal(long id) {
    return new SessionUser(accounts.byId(id).orElseThrow());
  }

  MovementCommand command(long id, int qty, String reason, UUID key) {
    return new MovementCommand(
        key,
        id,
        qty,
        reason,
        new BigDecimal("20"),
        new BigDecimal("28"),
        reason.equals("PERDA") ? "Avaria" : "");
  }

  Movement move(int qty, String reason) {
    return service.move(command(product, qty, reason, UUID.randomUUID()), admin);
  }

  long count() {
    return db.queryForObject("SELECT count(*) FROM movimentacao", Long.class);
  }

  void reconciled() {
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM (SELECT p.id,p.saldo,coalesce(sum(CASE WHEN m.tipo='ENTRADA'"
                    + " THEN m.quantidade ELSE -m.quantidade END),0) real FROM produto p LEFT JOIN"
                    + " movimentacao m ON m.produto_id=p.id GROUP BY p.id) x WHERE saldo<>real OR"
                    + " saldo<0",
                Long.class))
        .isZero();
  }

  @Test
  void T01_login_valid_invalid_inactive() throws Exception {
    mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", PASSWORD))
        .andExpect(authenticated());
    mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "errada"))
        .andExpect(unauthenticated())
        .andExpect(redirectedUrl("/login?erro"));
    db.update("UPDATE usuario SET ativo=false WHERE id=?", operator);
    mvc.perform(
            post("/login").with(csrf()).param("username", "operador").param("password", PASSWORD))
        .andExpect(unauthenticated());
  }

  @Test
  void T02_logout_and_anonymous() throws Exception {
    var r =
        mvc.perform(
                post("/login").with(csrf()).param("username", "admin").param("password", PASSWORD))
            .andReturn();
    var s = (org.springframework.mock.web.MockHttpSession) r.getRequest().getSession(false);
    mvc.perform(post("/logout").session(s).with(csrf()))
        .andExpect(redirectedUrl("/login?saiu"))
        .andExpect(unauthenticated());
    assertThat(s.isInvalid()).isTrue();
    mvc.perform(get("/produtos")).andExpect(status().is3xxRedirection());
  }

  @Test
  void T03_product_and_category() {
    assertThat(service.product(product).sku()).isEqualTo("SKU-001");
    assertThat(service.product(product).saldo()).isZero();
    assertThat(repo.categories()).hasSize(1);
    reconciled();
  }

  @Test
  void T04_duplicate_sku_and_category() {
    assertThatThrownBy(
            () ->
                service.saveProduct(
                    null, "sku-001", "Outro", cat, BigDecimal.ONE, BigDecimal.TEN, 0, true))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.category(null, " alimentos "))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.category(null, " ")).isInstanceOf(BusinessException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void T05_invalid_quantity(int qty) {
    assertThatThrownBy(() -> move(qty, "COMPRA")).isInstanceOf(BusinessException.class);
    assertThat(count()).isZero();
    reconciled();
  }

  @Test
  void T05_decimal_input_and_negative_money() throws Exception {
    mvc.perform(
            post("/movimentos/revisar")
                .with(user(principal(admin)))
                .with(csrf())
                .param("produtoId", "" + product)
                .param("quantidade", "1.5")
                .param("motivo", "COMPRA")
                .param("custo", "20")
                .param("chave", UUID.randomUUID().toString()))
        .andExpect(model().attributeHasFieldErrors("form", "quantidade"));
    assertThatThrownBy(
            () ->
                service.move(
                    new MovementCommand(
                        UUID.randomUUID(), product, 1, "COMPRA", new BigDecimal("-1"), null, ""),
                    admin))
        .isInstanceOf(BusinessException.class);
    assertThat(count()).isZero();
  }

  @Test
  void T06_entry() {
    var m = move(10, "COMPRA");
    assertThat(m.saldoApos()).isEqualTo(10);
    assertThat(service.product(product).custo()).isEqualByComparingTo("20");
    assertThat(m.custo().multiply(BigDecimal.valueOf(m.quantidade()))).isEqualByComparingTo("200");
    reconciled();
  }

  @Test
  void T07_sale() {
    move(10, "COMPRA");
    var m = move(3, "VENDA");
    assertThat(m.saldoApos()).isEqualTo(7);
    assertThat(m.preco().multiply(BigDecimal.valueOf(3))).isEqualByComparingTo("84");
    reconciled();
  }

  @Test
  void T08_loss() {
    move(10, "COMPRA");
    move(1, "PERDA");
    assertThat(service.product(product).saldo()).isEqualTo(9);
    assertThatThrownBy(
            () ->
                service.move(
                    new MovementCommand(UUID.randomUUID(), product, 1, "PERDA", null, null, ""),
                    admin))
        .isInstanceOf(BusinessException.class);
    reconciled();
  }

  @Test
  void T09_insufficient_then_exact() {
    move(7, "COMPRA");
    assertThatThrownBy(() -> move(8, "VENDA")).hasMessageContaining("Saldo insuficiente");
    assertThat(count()).isEqualTo(1);
    move(7, "VENDA");
    assertThat(service.product(product).saldo()).isZero();
    reconciled();
  }

  @Test
  void T10_indicators() {
    move(10, "COMPRA");
    move(3, "VENDA");
    move(1, "PERDA");
    var p = br.com.estoquebairro.service.Period.current();
    var m = repo.summary(p.from(), p.until());
    assertThat((BigDecimal) m.get("valor")).isEqualByComparingTo("120");
    assertThat((BigDecimal) m.get("vendas")).isEqualByComparingTo("84");
    assertThat((BigDecimal) m.get("entradas")).isEqualByComparingTo("200");
    assertThat((BigDecimal) m.get("perdas")).isEqualByComparingTo("20");
    assertThat(repo.quantities(p.from(), p.until())).hasSize(1);
    reconciled();
  }

  @Test
  void T11_filters_and_pagination() {
    move(5, "COMPRA");
    assertThat(repo.search("sku-001", cat, "ativo", true, 0).total()).isEqualTo(1);
    assertThat(repo.search("%", null, "todos", false, 0).total()).isZero();
    for (int i = 0; i < 21; i++)
      service.saveProduct(
          null, "P" + i, "Produto " + i, cat, BigDecimal.ONE, BigDecimal.TEN, 0, true);
    assertThat(repo.search("", null, "todos", false, 1).items()).hasSize(2);
    assertThat(repo.search("", null, "todos", false, -9).page()).isZero();
    assertThat(repo.search("", null, "todos", false, 999).page()).isEqualTo(1);
  }

  @Test
  void T12_last_day_timezone() {
    String sql =
        "INSERT INTO"
            + " movimentacao(produto_id,usuario_id,tipo,motivo,quantidade,custo_unitario,chave_requisicao,assinatura,saldo_apos,data_hora)"
            + " VALUES (?,?,'ENTRADA','COMPRA',1,20,?,'x',?,?)";
    db.update(
        sql,
        product,
        admin,
        UUID.randomUUID(),
        1,
        OffsetDateTime.parse("2026-11-09T23:59:59-03:00"));
    db.update(
        sql,
        product,
        admin,
        UUID.randomUUID(),
        2,
        OffsetDateTime.parse("2026-11-10T00:00:00-03:00"));
    db.update("UPDATE produto SET saldo=2 WHERE id=?", product);
    var period =
        new br.com.estoquebairro.service.Period(
            LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9));
    assertThat(repo.history(period.from(), period.until(), product, "ENTRADA", 0).total())
        .isEqualTo(1);
    reconciled();
  }

  @Test
  void T13_deactivation() {
    move(1, "COMPRA");
    assertThatThrownBy(
            () ->
                service.saveProduct(
                    product, "SKU-001", "Arroz", cat, null, new BigDecimal("28"), 5, false))
        .isInstanceOf(BusinessException.class);
    move(1, "VENDA");
    service.saveProduct(product, "SKU-001", "Arroz", cat, null, new BigDecimal("28"), 5, false);
    assertThat(service.product(product).ativo()).isFalse();
    assertThat(count()).isEqualTo(2);
    assertThatThrownBy(() -> move(1, "COMPRA")).hasMessageContaining("inativo");
    assertThat(repo.search(null, null, "inativo", false, 0).total()).isEqualTo(1);
    reconciled();
  }

  @Test
  void T14_admin_routes_protected() throws Exception {
    for (String path : List.of("/admin/usuarios", "/admin/categorias", "/admin/produtos/novo")) {
      mvc.perform(get(path).with(user(principal(operator)))).andExpect(status().isForbidden());
      mvc.perform(post(path).with(user(principal(operator))).with(csrf()))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void T15_last_admin() {
    assertThatThrownBy(() -> users.save(admin, "Admin", "admin", "", "OPERADOR", true))
        .hasMessageContaining("administrador ativo");
    assertThatThrownBy(() -> users.save(admin, "Admin", "admin", "", "ADMIN", false))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void T16_concurrent_stock() throws Exception {
    move(5, "COMPRA");
    var pool = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    Callable<Boolean> task =
        () -> {
          gate.await();
          try {
            move(4, "VENDA");
            return true;
          } catch (BusinessException e) {
            return false;
          }
        };
    try {
      var a = pool.submit(task);
      var b = pool.submit(task);
      gate.countDown();
      assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
    } finally {
      pool.shutdownNow();
    }
    assertThat(service.product(product).saldo()).isEqualTo(1);
    assertThat(count()).isEqualTo(2);
    reconciled();
  }

  @Test
  void T17_idempotency() {
    var c = command(product, 5, "COMPRA", UUID.randomUUID());
    var a = service.move(c, admin);
    var b = service.move(c, admin);
    assertThat(a.id()).isEqualTo(b.id());
    assertThat(count()).isEqualTo(1);
    assertThatThrownBy(() -> service.move(command(product, 6, "COMPRA", c.chave()), admin))
        .hasMessageContaining("outros dados");
    assertThatThrownBy(() -> service.move(c, operator)).isInstanceOf(BusinessException.class);
    reconciled();
  }

  @Test
  void T17_concurrent_replay() throws Exception {
    var c = command(product, 5, "COMPRA", UUID.randomUUID());
    var pool = Executors.newFixedThreadPool(2);
    try {
      var a = pool.submit(() -> service.move(c, admin));
      var b = pool.submit(() -> service.move(c, admin));
      assertThat(a.get(20, TimeUnit.SECONDS).id()).isEqualTo(b.get(20, TimeUnit.SECONDS).id());
    } finally {
      pool.shutdownNow();
    }
    assertThat(count()).isEqualTo(1);
    reconciled();
  }

  @Test
  void T18_transaction_rollback() {
    db.execute(
        "CREATE OR REPLACE FUNCTION falhar_teste() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
            + " RAISE EXCEPTION 'falha controlada'; END; $$");
    db.execute(
        "CREATE TRIGGER falha_teste BEFORE INSERT ON movimentacao FOR EACH ROW EXECUTE FUNCTION"
            + " falhar_teste()");
    assertThatThrownBy(() -> move(10, "COMPRA"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThat(count()).isZero();
    assertThat(service.product(product).saldo()).isZero();
    reconciled();
  }

  @Test
  void T19_price_snapshot() {
    move(10, "COMPRA");
    var sale = move(3, "VENDA");
    service.saveProduct(product, "SKU-001", "Arroz", cat, null, new BigDecimal("35"), 5, true);
    assertThat(repo.movement(sale.id()).orElseThrow().preco()).isEqualByComparingTo("28");
    assertThatThrownBy(() -> move(1, "VENDA")).hasMessageContaining("preço mudou");
    reconciled();
  }

  @Test
  void T20_sql_html_escape() throws Exception {
    String text = "<script>alert('x')</script>";
    service.saveProduct(product, "SKU-001", text, cat, null, new BigDecimal("28"), 5, true);
    assertThat(repo.search("' OR 1=1 --", null, "todos", false, 0).total()).isZero();
    mvc.perform(get("/produtos").with(user(principal(admin))))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(text))));
  }

  @Test
  void T25_csrf_required() throws Exception {
    mvc.perform(post("/admin/categorias").with(user(principal(admin))).param("nome", "Ataque"))
        .andExpect(status().isForbidden());
    assertThat(repo.categories()).hasSize(1);
  }

  @Test
  void full_review_confirmation_and_replay() throws Exception {
    String key = UUID.randomUUID().toString();
    var result =
        mvc.perform(
                post("/movimentos/revisar")
                    .with(user(principal(admin)))
                    .with(csrf())
                    .param("produtoId", "" + product)
                    .param("chave", key)
                    .param("quantidade", "10")
                    .param("motivo", "COMPRA")
                    .param("custo", "20"))
            .andExpect(status().isOk())
            .andExpect(view().name("movement-review"))
            .andReturn();
    var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession();
    assertThat(count()).isZero();
    for (int i = 0; i < 2; i++)
      mvc.perform(
              post("/movimentos/confirmar")
                  .session(session)
                  .with(user(principal(admin)))
                  .with(csrf())
                  .param("chave", key))
          .andExpect(status().is3xxRedirection());
    assertThat(count()).isEqualTo(1);
    mvc.perform(get("/movimentos/1").with(user(principal(admin)))).andExpect(status().isOk());
    reconciled();
  }

  @Test
  void all_pages_render() throws Exception {
    for (String path :
        List.of(
            "/login",
            "/",
            "/produtos",
            "/historico",
            "/admin/categorias",
            "/admin/usuarios",
            "/admin/usuarios/novo",
            "/admin/usuarios/" + admin,
            "/admin/produtos/novo",
            "/admin/produtos/" + product,
            "/admin/categorias/" + cat,
            "/movimentos/novo?produto=" + product)) {
      mvc.perform(get(path).with(user(principal(admin)))).andExpect(status().isOk());
    }
  }

  @Test
  void forged_confirmation_is_rejected() throws Exception {
    mvc.perform(
            post("/movimentos/confirmar")
                .with(user(principal(admin)))
                .with(csrf())
                .param("chave", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest());
    assertThat(count()).isZero();
  }

  @Test
  void deactivated_session_is_invalidated() throws Exception {
    var p = principal(operator);
    db.update("UPDATE usuario SET ativo=false WHERE id=?", operator);
    mvc.perform(get("/produtos").with(user(p))).andExpect(redirectedUrl("/login?expirou"));
  }

  @Test
  void changed_role_invalidates_session() throws Exception {
    var p = principal(operator);
    users.save(operator, "Operador", "operador", "", "ADMIN", true);
    mvc.perform(get("/produtos").with(user(p))).andExpect(redirectedUrl("/login?expirou"));
  }

  @Test
  void immutable_database_history() {
    move(1, "COMPRA");
    assertThatThrownBy(() -> db.update("DELETE FROM movimentacao"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThatThrownBy(() -> db.update("UPDATE movimentacao SET quantidade=2"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    reconciled();
  }

  @Test
  void database_constraints() {
    assertThatThrownBy(() -> db.update("UPDATE produto SET saldo=-1"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThatThrownBy(() -> db.update("UPDATE produto SET categoria_id=999"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThatThrownBy(() -> db.update("DELETE FROM categoria"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
  }

  @Test
  void category_edit() {
    service.category(cat, "Mercearia");
    assertThat(repo.categories().getFirst().nome()).isEqualTo("Mercearia");
    assertThatThrownBy(() -> service.category(999L, "Teste")).isInstanceOf(BusinessException.class);
  }

  @Test
  void product_validation() {
    assertThatThrownBy(() -> service.product(999)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                service.saveProduct(
                    999L, "XX", "Teste", cat, BigDecimal.ONE, BigDecimal.TEN, 0, true))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                service.saveProduct(
                    null, "XX", "Teste", 999, BigDecimal.ONE, BigDecimal.TEN, 0, true))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                service.saveProduct(
                    null, "XX", "Teste", cat, BigDecimal.ONE, BigDecimal.TEN, -1, true))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void movement_validation() {
    assertThatThrownBy(() -> service.move(command(999, 1, "COMPRA", UUID.randomUUID()), admin))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.move(command(product, 1, "COMPRA", UUID.randomUUID()), 999))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.validate(command(product, 1, "OUTRO", UUID.randomUUID())))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.validate(command(product, 1, "COMPRA", null)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                service.validate(
                    new MovementCommand(
                        UUID.randomUUID(), product, 1, "PERDA", null, null, "x".repeat(301))))
        .isInstanceOf(BusinessException.class);
    db.update("UPDATE produto SET saldo=? WHERE id=?", Integer.MAX_VALUE, product);
    assertThatThrownBy(() -> move(1, "COMPRA")).hasMessageContaining("limite");
  }

  @Test
  void users_create_edit_validation() {
    long id = users.save(null, "Novo", " NEW ", "Nova_senha_12345", "ADMIN", true);
    assertThat(accounts.byId(id).orElseThrow().login()).isEqualTo("new");
    users.save(admin, "Admin", "admin", "", "OPERADOR", true);
    assertThat(accounts.admins()).isEqualTo(1);
    users.save(id, "Alterado", "new", "Outra_senha_12345", "ADMIN", true);
    assertThat(accounts.byId(id).orElseThrow().nome()).isEqualTo("Alterado");
    assertThatThrownBy(() -> users.save(null, "Novo", "new", "Nova_senha_12345", "ADMIN", true))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () -> users.save(null, "Novo", "não válido", "Nova_senha_12345", "ADMIN", true))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> users.save(null, "Novo", "ok", "curta", "ADMIN", true))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> users.save(null, "Novo", "ok", "a".repeat(73), "ADMIN", true))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> users.save(null, "Novo", "ok", null, "ADMIN", true))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> users.save(null, "Novo", "ok", PASSWORD, "INVALID", true))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> users.save(999L, "Novo", "ok", "", "ADMIN", true))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void last_admin_concurrency() throws Exception {
    users.save(operator, "Operador", "operador", "", "ADMIN", true);
    var pool = Executors.newFixedThreadPool(2);
    try {
      Callable<Boolean> a =
          () -> {
            try {
              users.save(admin, "Admin", "admin", "", "OPERADOR", true);
              return true;
            } catch (BusinessException e) {
              return false;
            }
          };
      Callable<Boolean> b =
          () -> {
            try {
              users.save(operator, "Operador", "operador", "", "OPERADOR", true);
              return true;
            } catch (BusinessException e) {
              return false;
            }
          };
      var x = pool.submit(a);
      var y = pool.submit(b);
      assertThat(List.of(x.get(), y.get())).containsExactlyInAnyOrder(true, false);
    } finally {
      pool.shutdownNow();
    }
    assertThat(accounts.admins()).isEqualTo(1);
  }

  @Test
  void money_and_text_boundaries() {
    assertThat(Rules.money(new BigDecimal("1.20"), "x")).isEqualByComparingTo("1.20");
    for (BigDecimal v :
        Arrays.asList(
            null, BigDecimal.ZERO, new BigDecimal("10000000000"), new BigDecimal("1.234")))
      assertThatThrownBy(() -> Rules.money(v, "x")).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> Rules.text(null, "x", 3)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> Rules.text("abcd", "x", 3)).isInstanceOf(BusinessException.class);
  }

  @Test
  void period_validation() {
    assertThatThrownBy(() -> new br.com.estoquebairro.service.Period(null, LocalDate.now()))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> new br.com.estoquebairro.service.Period(LocalDate.now(), null))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                new br.com.estoquebairro.service.Period(
                    LocalDate.now(), LocalDate.now().minusDays(1)))
        .isInstanceOf(BusinessException.class);
  }
}
