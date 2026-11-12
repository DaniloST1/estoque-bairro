# Código fonte integral

Os arquivos originais editáveis estão na pasta src. Este índice reúne o conteúdo para consulta e apoio ao relatório.

## pom.xml

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-parent</artifactId><version>3.5.16</version><relativePath/></parent>
  <groupId>br.com.estoquebairro</groupId><artifactId>estoque-bairro</artifactId><version>1.0.0</version><name>Estoque Bairro</name>
  <properties><java.version>21</java.version><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId></dependency>
    <dependency><groupId>org.thymeleaf.extras</groupId><artifactId>thymeleaf-extras-springsecurity6</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-jdbc</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>io.zonky.test</groupId><artifactId>embedded-postgres</artifactId><version>2.1.1</version><scope>test</scope></dependency>
  </dependencies>
  <build><plugins>
    <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
    <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><configuration><useModulePath>false</useModulePath></configuration></plugin>
    <plugin><groupId>org.jacoco</groupId><artifactId>jacoco-maven-plugin</artifactId><version>0.8.14</version><executions>
      <execution><goals><goal>prepare-agent</goal></goals></execution>
      <execution><id>report</id><phase>verify</phase><goals><goal>report</goal><goal>check</goal></goals><configuration><rules><rule><element>PACKAGE</element><includes><include>br.com.estoquebairro.service</include></includes><limits><limit><counter>LINE</counter><value>COVEREDRATIO</value><minimum>0.80</minimum></limit><limit><counter>BRANCH</counter><value>COVEREDRATIO</value><minimum>0.70</minimum></limit></limits></rule></rules></configuration></execution>
    </executions></plugin>
  </plugins></build>
</project>
```

## src/main/java/br/com/estoquebairro/EstoqueBairroApplication.java

```java
package br.com.estoquebairro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EstoqueBairroApplication {
  public static void main(String[] args) {
    SpringApplication.run(EstoqueBairroApplication.class, args);
  }
}
```

## src/main/java/br/com/estoquebairro/config/Bootstrap.java

```java
package br.com.estoquebairro.config;

import br.com.estoquebairro.repository.*;
import br.com.estoquebairro.service.*;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class Bootstrap implements ApplicationRunner {
  private final UserRepository repo;
  private final UserService users;
  private final InventoryService inventory;

  @Value("${app.bootstrap.login}")
  String login;

  @Value("${app.bootstrap.password}")
  String password;

  @Value("${app.demo.enabled}")
  boolean demo;

  @Value("${app.demo.password}")
  String operatorPassword;

  public Bootstrap(UserRepository repo, UserService users, InventoryService inventory) {
    this.repo = repo;
    this.users = users;
    this.inventory = inventory;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    repo.lockAdministration();
    if (repo.count() > 0) return;
    users.save(null, "Administrador", login, password, "ADMIN", true);
    if (!demo) return;
    users.save(null, "Operador de demonstração", "operador", operatorPassword, "OPERADOR", true);
    long foods = inventory.category(null, "Alimentos"),
        drinks = inventory.category(null, "Bebidas"),
        clean = inventory.category(null, "Limpeza");
    String[] names = {
      "Arroz 5 kg",
      "Feijão 1 kg",
      "Açúcar 1 kg",
      "Café 500 g",
      "Macarrão 500 g",
      "Leite 1 L",
      "Água mineral 500 ml",
      "Suco 1 L",
      "Detergente 500 ml",
      "Sabão em pó 1 kg"
    };
    for (int i = 0; i < 10; i++)
      inventory.saveProduct(
          null,
          String.format("MER-%03d", i + 1),
          names[i],
          i < 5 ? foods : i < 8 ? drinks : clean,
          new BigDecimal("10.00"),
          new BigDecimal("15.00"),
          5,
          true);
  }
}
```

## src/main/java/br/com/estoquebairro/config/SecurityConfig.java

```java
package br.com.estoquebairro.config;

import br.com.estoquebairro.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository repo) {
    return login ->
        repo.byLogin(login.strip().toLowerCase(Locale.ROOT))
            .map(SessionUser::new)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, UserRepository repo) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/login", "/style.css", "/app.js", "/error")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?erro")
                    .permitAll())
        .logout(
            l ->
                l.logoutSuccessUrl("/login?saiu")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .exceptionHandling(e -> e.accessDeniedPage("/acesso-negado"))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'; img-src"
                                + " 'self' data:; base-uri 'self'; form-action 'self';"
                                + " frame-ancestors 'none'")))
        .addFilterBefore(
            new OncePerRequestFilter() {
              @Override
              protected void doFilterInternal(
                  HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                  throws IOException, ServletException {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof SessionUser p) {
                  try {
                    var current = repo.byId(p.id);
                    if (current.isEmpty()
                        || !current.get().ativo()
                        || current.get().versao() != p.version) {
                      SecurityContextHolder.clearContext();
                      if (request.getSession(false) != null) request.getSession(false).invalidate();
                      response.sendRedirect(request.getContextPath() + "/login?expirou");
                      return;
                    }
                  } catch (org.springframework.dao.DataAccessException e) {
                    response.setStatus(503);
                    response.setContentType("text/plain;charset=UTF-8");
                    response
                        .getWriter()
                        .write(
                            "Banco indisponível. Nenhuma operação foi confirmada nesta requisição."
                                + " Tente novamente.");
                    return;
                  }
                }
                chain.doFilter(request, response);
              }
            },
            AuthorizationFilter.class);
    return http.build();
  }
}
```

## src/main/java/br/com/estoquebairro/config/SessionUser.java

```java
package br.com.estoquebairro.config;

import br.com.estoquebairro.model.Account;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

public class SessionUser extends User {
  public final long id, version;
  public final String nome;

  public SessionUser(Account a) {
    super(
        a.login(),
        a.senhaHash(),
        a.ativo(),
        true,
        true,
        true,
        AuthorityUtils.createAuthorityList("ROLE_" + a.perfil()));
    id = a.id();
    version = a.versao();
    nome = a.nome();
  }
}
```

## src/main/java/br/com/estoquebairro/model/Account.java

```java
package br.com.estoquebairro.model;

public record Account(
    long id,
    String nome,
    String login,
    String senhaHash,
    String perfil,
    boolean ativo,
    long versao) {}
```

## src/main/java/br/com/estoquebairro/model/Category.java

```java
package br.com.estoquebairro.model;

public record Category(long id, String nome) {}
```

## src/main/java/br/com/estoquebairro/model/Movement.java

```java
package br.com.estoquebairro.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Movement(
    long id,
    long produtoId,
    String produto,
    String sku,
    String usuario,
    String tipo,
    String motivo,
    int quantidade,
    BigDecimal custo,
    BigDecimal preco,
    OffsetDateTime dataHora,
    String justificativa,
    String assinatura,
    int saldoApos) {}
```

## src/main/java/br/com/estoquebairro/model/MovementCommand.java

```java
package br.com.estoquebairro.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record MovementCommand(
    UUID chave,
    long produtoId,
    int quantidade,
    String motivo,
    BigDecimal custo,
    BigDecimal preco,
    String justificativa)
    implements Serializable {}
```

## src/main/java/br/com/estoquebairro/model/Product.java

```java
package br.com.estoquebairro.model;

import java.math.BigDecimal;

public record Product(
    long id,
    String sku,
    String nome,
    long categoriaId,
    String categoria,
    BigDecimal custo,
    BigDecimal preco,
    int minimo,
    int saldo,
    boolean ativo) {}
```

## src/main/java/br/com/estoquebairro/repository/InventoryRepository.java

```java
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
```

## src/main/java/br/com/estoquebairro/repository/ReportRepository.java

```java
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
```

## src/main/java/br/com/estoquebairro/repository/UserRepository.java

```java
package br.com.estoquebairro.repository;

import br.com.estoquebairro.model.Account;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  private final JdbcTemplate db;
  private final RowMapper<Account> row =
      (r, n) ->
          new Account(
              r.getLong("id"),
              r.getString("nome"),
              r.getString("login"),
              r.getString("senha_hash"),
              r.getString("perfil"),
              r.getBoolean("ativo"),
              r.getLong("versao"));

  public UserRepository(JdbcTemplate db) {
    this.db = db;
  }

  public Optional<Account> byLogin(String login) {
    return db.query("SELECT * FROM usuario WHERE login=?", row, login).stream().findFirst();
  }

  public Optional<Account> byId(long id) {
    return db.query("SELECT * FROM usuario WHERE id=?", row, id).stream().findFirst();
  }

  public List<Account> all() {
    return db.query("SELECT * FROM usuario ORDER BY nome,id", row);
  }

  public long count() {
    return db.queryForObject("SELECT count(*) FROM usuario", Long.class);
  }

  public long admins() {
    return db.queryForObject(
        "SELECT count(*) FROM usuario WHERE ativo AND perfil='ADMIN'", Long.class);
  }

  public void lockAdministration() {
    db.queryForObject("SELECT pg_advisory_xact_lock(642001)", Object.class);
  }

  public long add(String name, String login, String hash, String role, boolean active) {
    return db.queryForObject(
        "INSERT INTO usuario(nome,login,senha_hash,perfil,ativo) VALUES (?,?,?,?,?) RETURNING id",
        Long.class,
        name,
        login,
        hash,
        role,
        active);
  }

  public void edit(long id, String name, String hash, String role, boolean active) {
    db.update(
        "UPDATE usuario SET nome=?,senha_hash=?,perfil=?,ativo=?,versao=versao+1 WHERE id=?",
        name,
        hash,
        role,
        active,
        id);
  }
}
```

## src/main/java/br/com/estoquebairro/service/BusinessException.java

```java
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
```

## src/main/java/br/com/estoquebairro/service/InventoryService.java

```java
package br.com.estoquebairro.service;

import br.com.estoquebairro.model.*;
import br.com.estoquebairro.repository.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
  private final InventoryRepository repo;
  private final UserRepository users;

  public InventoryService(InventoryRepository repo, UserRepository users) {
    this.repo = repo;
    this.users = users;
  }

  public Product product(long id) {
    return repo.product(id, false)
        .orElseThrow(() -> new BusinessException("produtoId", "Produto não encontrado."));
  }

  @Transactional
  public long category(Long id, String name) {
    String n = Rules.text(name, "nome", 80);
    try {
      if (id == null) return repo.addCategory(n, Rules.normalized(n, "nome", 80));
      if (repo.editCategory(id, n, Rules.normalized(n, "nome", 80)) == 0)
        throw new BusinessException("nome", "Categoria não encontrada.");
      return id;
    } catch (DuplicateKeyException e) {
      throw new BusinessException("nome", "Já existe uma categoria com esse nome.");
    }
  }

  @Transactional
  public long saveProduct(
      Long id,
      String sku,
      String name,
      long cat,
      BigDecimal cost,
      BigDecimal price,
      int min,
      boolean active) {
    String s = Rules.normalized(sku, "sku", 40), n = Rules.text(name, "nome", 120);
    BigDecimal pr = Rules.money(price, "preco");
    if (min < 0) throw new BusinessException("minimo", "O mínimo não pode ser negativo.");
    if (!repo.categoryExists(cat))
      throw new BusinessException("categoriaId", "Selecione uma categoria existente.");
    try {
      if (id == null) return repo.addProduct(s, n, cat, Rules.money(cost, "custo"), pr, min);
      Product old =
          repo.product(id, true)
              .orElseThrow(() -> new BusinessException("nome", "Produto não encontrado."));
      if (!active && old.saldo() != 0)
        throw new BusinessException("ativo", "Zere o saldo por movimentações antes de inativar.");
      repo.editProduct(id, s, n, cat, pr, min, active);
      return id;
    } catch (DuplicateKeyException e) {
      throw new BusinessException("sku", "Este SKU já está cadastrado.");
    }
  }

  public MovementCommand validate(MovementCommand c) {
    if (c.chave() == null)
      throw new BusinessException("chave", "Solicitação inválida. Abra um novo formulário.");
    if (c.quantidade() <= 0)
      throw new BusinessException("quantidade", "Use uma quantidade inteira maior que zero.");
    if (!Set.of("COMPRA", "VENDA", "PERDA").contains(Objects.toString(c.motivo(), "")))
      throw new BusinessException("motivo", "Selecione compra, venda ou perda.");
    String reason = Objects.toString(c.justificativa(), "").strip();
    if (reason.length() > 300)
      throw new BusinessException("justificativa", "Use até 300 caracteres.");
    if (c.motivo().equals("PERDA")) reason = Rules.text(reason, "justificativa", 300);
    BigDecimal cost = c.motivo().equals("COMPRA") ? Rules.money(c.custo(), "custo") : null;
    BigDecimal price = c.motivo().equals("VENDA") ? Rules.money(c.preco(), "preco") : null;
    return new MovementCommand(
        c.chave(), c.produtoId(), c.quantidade(), c.motivo(), cost, price, reason);
  }

  @Transactional
  public Movement move(MovementCommand input, long user) {
    MovementCommand c = validate(input);
    if (users.byId(user).filter(Account::ativo).isEmpty())
      throw new BusinessException("chave", "Usuário inativo ou inexistente.");
    String hash = fingerprint(c, user);
    // Lock by request BEFORE product: even a replay with another product is serialized.
    repo.lockRequest(c.chave());
    Optional<Movement> prior = repo.byKey(c.chave());
    if (prior.isPresent()) {
      if (!prior.get().assinatura().equals(hash))
        throw new BusinessException("chave", "Esta solicitação já foi usada com outros dados.");
      return prior.get();
    }
    Product p =
        repo.product(c.produtoId(), true)
            .orElseThrow(() -> new BusinessException("produtoId", "Produto não encontrado."));
    if (!p.ativo())
      throw new BusinessException("produtoId", "Produto inativo não pode ser movimentado.");
    boolean entry = c.motivo().equals("COMPRA");
    if (!entry && c.quantidade() > p.saldo())
      throw new BusinessException(
          "quantidade", "Saldo insuficiente. Disponível: " + p.saldo() + " UN.");
    if (c.motivo().equals("VENDA") && p.preco().compareTo(c.preco()) != 0)
      throw new BusinessException("preco", "O preço mudou. Abra uma nova saída e confira o valor.");
    long next = (long) p.saldo() + (entry ? c.quantidade() : -c.quantidade());
    if (next > Integer.MAX_VALUE)
      throw new BusinessException("quantidade", "A quantidade ultrapassa o limite do estoque.");
    BigDecimal cost = entry ? c.custo() : p.custo();
    repo.stock(p.id(), (int) next, cost);
    long id = repo.insertMovement(c, user, cost, c.preco(), hash, (int) next);
    return repo.movement(id).orElseThrow();
  }

  private String fingerprint(MovementCommand c, long user) {
    String value =
        user
            + "|"
            + c.produtoId()
            + "|"
            + c.quantidade()
            + "|"
            + c.motivo()
            + "|"
            + c.custo()
            + "|"
            + c.preco()
            + "|"
            + c.justificativa();
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
```

## src/main/java/br/com/estoquebairro/service/Period.java

```java
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
```

## src/main/java/br/com/estoquebairro/service/Rules.java

```java
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
```

## src/main/java/br/com/estoquebairro/service/UserService.java

```java
package br.com.estoquebairro.service;

import br.com.estoquebairro.model.Account;
import br.com.estoquebairro.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository repo;
  private final PasswordEncoder encoder;

  public UserService(UserRepository repo, PasswordEncoder encoder) {
    this.repo = repo;
    this.encoder = encoder;
  }

  @Transactional
  public long save(
      Long id, String name, String login, String password, String role, boolean active) {
    repo.lockAdministration();
    String n = Rules.text(name, "nome", 100);
    if (!Set.of("ADMIN", "OPERADOR").contains(Objects.toString(role, "")))
      throw new BusinessException("perfil", "Perfil inválido.");
    Account old =
        id == null
            ? null
            : repo.byId(id)
                .orElseThrow(() -> new BusinessException("nome", "Conta não encontrada."));
    if (old != null
        && old.ativo()
        && old.perfil().equals("ADMIN")
        && (!active || !role.equals("ADMIN"))
        && repo.admins() <= 1)
      throw new BusinessException(
          "perfil", "É necessário manter pelo menos um administrador ativo.");
    String hash;
    if (old != null && (password == null || password.isEmpty())) hash = old.senhaHash();
    else {
      if (password == null
          || password.length() < 12
          || password.getBytes(StandardCharsets.UTF_8).length > 72)
        throw new BusinessException("senha", "Use pelo menos 12 caracteres e no máximo 72 bytes.");
      hash = encoder.encode(password);
    }
    try {
      if (id == null) {
        String l = Rules.text(login, "login", 60).toLowerCase(Locale.ROOT);
        if (!l.matches("[a-z0-9._-]+"))
          throw new BusinessException("login", "Use letras, números, ponto, hífen ou sublinhado.");
        return repo.add(n, l, hash, role, active);
      }
      repo.edit(id, n, hash, role, active);
      return id;
    } catch (DuplicateKeyException e) {
      throw new BusinessException("login", "Este login já está em uso.");
    }
  }
}
```

## src/main/java/br/com/estoquebairro/web/AdminController.java

```java
package br.com.estoquebairro.web;

import br.com.estoquebairro.repository.*;
import br.com.estoquebairro.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
  private final InventoryService service;
  private final InventoryRepository repo;
  private final UserService users;
  private final UserRepository accounts;

  public AdminController(
      InventoryService service,
      InventoryRepository repo,
      UserService users,
      UserRepository accounts) {
    this.service = service;
    this.repo = repo;
    this.users = users;
    this.accounts = accounts;
  }

  @GetMapping("/produtos/novo")
  public String newProduct(Model m) {
    m.addAttribute("form", new ProductForm());
    return productPage(null, m);
  }

  @GetMapping("/produtos/{id}")
  public String editProduct(@PathVariable long id, Model m) {
    var p = service.product(id);
    var f = new ProductForm();
    f.setSku(p.sku());
    f.setNome(p.nome());
    f.setCategoriaId(p.categoriaId());
    f.setCusto(p.custo());
    f.setPreco(p.preco());
    f.setMinimo(p.minimo());
    f.setAtivo(p.ativo());
    m.addAttribute("form", f);
    return productPage(id, m);
  }

  private String productPage(Long id, Model m) {
    m.addAttribute("id", id);
    m.addAttribute("categories", repo.categories());
    return "product-form";
  }

  @PostMapping({"/produtos/novo", "/produtos/{id}"})
  public String saveProduct(
      @PathVariable(required = false) Long id,
      @Valid @ModelAttribute("form") ProductForm f,
      BindingResult b,
      Model m,
      RedirectAttributes flash) {
    if (!b.hasErrors())
      try {
        service.saveProduct(
            id,
            f.getSku(),
            f.getNome(),
            f.getCategoriaId(),
            f.getCusto(),
            f.getPreco(),
            f.getMinimo(),
            f.getAtivo());
        flash.addFlashAttribute(
            "success", "Produto salvo. O saldo é alterado somente por movimentações.");
        return "redirect:/produtos";
      } catch (BusinessException e) {
        b.rejectValue(e.field(), "business", e.getMessage());
      }
    return productPage(id, m);
  }

  @GetMapping("/categorias")
  public String categories(Model m) {
    m.addAttribute("form", new CategoryForm());
    m.addAttribute("categories", repo.categories());
    return "categories";
  }

  @GetMapping("/categorias/{id}")
  public String category(@PathVariable long id, Model m) {
    var c =
        repo.categories().stream()
            .filter(x -> x.id() == id)
            .findFirst()
            .orElseThrow(() -> new BusinessException("nome", "Categoria não encontrada."));
    var f = new CategoryForm();
    f.setNome(c.nome());
    m.addAttribute("form", f);
    m.addAttribute("id", id);
    m.addAttribute("categories", repo.categories());
    return "categories";
  }

  @PostMapping({"/categorias", "/categorias/{id}"})
  public String saveCategory(
      @PathVariable(required = false) Long id,
      @Valid @ModelAttribute("form") CategoryForm f,
      BindingResult b,
      Model m,
      RedirectAttributes flash) {
    if (!b.hasErrors())
      try {
        service.category(id, f.getNome());
        flash.addFlashAttribute("success", "Categoria salva.");
        return "redirect:/admin/categorias";
      } catch (BusinessException e) {
        b.rejectValue(e.field(), "business", e.getMessage());
      }
    m.addAttribute("id", id);
    m.addAttribute("categories", repo.categories());
    return "categories";
  }

  @GetMapping("/usuarios")
  public String users(Model m) {
    m.addAttribute("accounts", accounts.all());
    return "users";
  }

  @GetMapping("/usuarios/novo")
  public String newUser(Model m) {
    m.addAttribute("form", new UserForm());
    return "user-form";
  }

  @GetMapping("/usuarios/{id}")
  public String user(@PathVariable long id, Model m) {
    var a =
        accounts.byId(id).orElseThrow(() -> new BusinessException("nome", "Conta não encontrada."));
    var f = new UserForm();
    f.setNome(a.nome());
    f.setLogin(a.login());
    f.setPerfil(a.perfil());
    f.setAtivo(a.ativo());
    m.addAttribute("form", f);
    m.addAttribute("id", id);
    return "user-form";
  }

  @PostMapping({"/usuarios/novo", "/usuarios/{id}"})
  public String saveUser(
      @PathVariable(required = false) Long id,
      @Valid @ModelAttribute("form") UserForm f,
      BindingResult b,
      Model m,
      RedirectAttributes flash) {
    if (!b.hasErrors())
      try {
        users.save(id, f.getNome(), f.getLogin(), f.getSenha(), f.getPerfil(), f.getAtivo());
        flash.addFlashAttribute(
            "success", "Conta salva. Alterações de acesso encerram as sessões anteriores.");
        return "redirect:/admin/usuarios";
      } catch (BusinessException e) {
        b.rejectValue(e.field(), "business", e.getMessage());
      }
    f.setSenha("");
    m.addAttribute("id", id);
    return "user-form";
  }
}
```

## src/main/java/br/com/estoquebairro/web/CategoryForm.java

```java
package br.com.estoquebairro.web;

import jakarta.validation.constraints.*;

public class CategoryForm {
  @NotBlank
  @Size(max = 80)
  private String nome = "";

  public String getNome() {
    return nome;
  }

  public void setNome(String value) {
    nome = value;
  }
}
```

## src/main/java/br/com/estoquebairro/web/ErrorAdvice.java

```java
package br.com.estoquebairro.web;

import br.com.estoquebairro.service.BusinessException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice(annotations = Controller.class)
public class ErrorAdvice {
  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String business(BusinessException e, Model m) {
    m.addAttribute("message", e.getMessage());
    return "error";
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public String invalid(Model m) {
    m.addAttribute(
        "message", "Parâmetro inválido. Confira números e datas antes de tentar novamente.");
    return "error";
  }

  @ExceptionHandler(DataAccessException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public String database(Model m) {
    m.addAttribute(
        "message",
        "Não foi possível confirmar a operação. Confira o histórico antes de tentar novamente; o"
            + " mesmo formulário preserva a chave contra duplicação.");
    return "error";
  }
}
```

## src/main/java/br/com/estoquebairro/web/InventoryController.java

```java
package br.com.estoquebairro.web;

import br.com.estoquebairro.config.SessionUser;
import br.com.estoquebairro.model.*;
import br.com.estoquebairro.repository.InventoryRepository;
import br.com.estoquebairro.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InventoryController {
  private final InventoryRepository repo;
  private final InventoryService service;

  public InventoryController(InventoryRepository repo, InventoryService service) {
    this.repo = repo;
    this.service = service;
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @RequestMapping("/acesso-negado")
  @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
  public String forbidden() {
    return "denied";
  }

  @ModelAttribute("categories")
  public List<Category> categories() {
    return repo.categories();
  }

  @GetMapping("/")
  public String dashboard(
      @RequestParam(required = false) LocalDate inicio,
      @RequestParam(required = false) LocalDate fim,
      Model m) {
    var period = period(inicio, fim, m);
    m.addAttribute("metrics", repo.summary(period.from(), period.until()));
    m.addAttribute("low", repo.search(null, null, "ativo", true, 0));
    m.addAttribute("quantities", repo.quantities(period.from(), period.until()));
    return "dashboard";
  }

  @GetMapping("/produtos")
  public String products(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) Long categoria,
      @RequestParam(defaultValue = "ativo") String situacao,
      @RequestParam(defaultValue = "false") boolean baixo,
      @RequestParam(defaultValue = "0") int pagina,
      Model m) {
    if (q.length() > 100) q = q.substring(0, 100);
    m.addAttribute("q", q);
    m.addAttribute("categoria", categoria);
    m.addAttribute("situacao", situacao);
    m.addAttribute("baixo", baixo);
    m.addAttribute("result", repo.search(q, categoria, situacao, baixo, pagina));
    return "products";
  }

  @GetMapping("/historico")
  public String history(
      @RequestParam(required = false) LocalDate inicio,
      @RequestParam(required = false) LocalDate fim,
      @RequestParam(required = false) Long produto,
      @RequestParam(defaultValue = "") String tipo,
      @RequestParam(defaultValue = "0") int pagina,
      Model m) {
    var period = period(inicio, fim, m);
    m.addAttribute("produto", produto);
    m.addAttribute("tipo", tipo);
    m.addAttribute("result", repo.history(period.from(), period.until(), produto, tipo, pagina));
    return "history";
  }

  private br.com.estoquebairro.service.Period period(LocalDate start, LocalDate end, Model m) {
    var now = br.com.estoquebairro.service.Period.current();
    var p = now;
    try {
      p =
          new br.com.estoquebairro.service.Period(
              start == null ? now.inicio() : start, end == null ? now.fim() : end);
    } catch (BusinessException e) {
      m.addAttribute("error", e.getMessage());
    }
    m.addAttribute("inicio", p.inicio());
    m.addAttribute("fim", p.fim());
    return p;
  }

  @GetMapping("/movimentos/novo")
  public String newMovement(
      @RequestParam(defaultValue = "COMPRA") String motivo,
      @RequestParam(required = false) Long produto,
      Model m) {
    var f = new MovementForm();
    f.setMotivo(Set.of("COMPRA", "VENDA", "PERDA").contains(motivo) ? motivo : "COMPRA");
    if (produto != null) {
      var p = service.product(produto);
      f.setProdutoId(p.id());
      f.setCusto(p.custo());
      f.setPreco(p.preco());
    }
    m.addAttribute("form", f);
    return movementPage(f, m);
  }

  private String movementPage(MovementForm f, Model m) {
    if (f.getProdutoId() != null) {
      try {
        m.addAttribute("product", service.product(f.getProdutoId()));
      } catch (BusinessException ignored) {
      }
    }
    if (!m.containsAttribute("product")) overview(f, m);
    return "movement-form";
  }

  /** Página de entrada ou saída sem produto escolhido: resumo do mês, histórico recente e modal. */
  private void overview(MovementForm f, Model m) {
    boolean entry = "COMPRA".equals(f.getMotivo());
    var period = br.com.estoquebairro.service.Period.current();
    var modal = new MovementForm();
    modal.setMotivo(entry ? "COMPRA" : "VENDA");
    m.addAttribute("modalForm", modal);
    m.addAttribute("modalProducts", repo.activeProducts());
    m.addAttribute("recent", repo.recent(entry ? "ENTRADA" : "SAIDA", 12));
    m.addAttribute("metrics", repo.summary(period.from(), period.until()));
    m.addAttribute(
        "monthQuantities",
        repo.quantities(period.from(), period.until()).stream()
            .mapToLong(
                q ->
                    entry
                        ? ((Number) q.get("entradas")).longValue()
                        : ((Number) q.get("vendas")).longValue()
                            + ((Number) q.get("perdas")).longValue())
            .sum());
    if (entry) m.addAttribute("low", repo.search(null, null, "ativo", true, 0));
  }

  @PostMapping("/movimentos/revisar")
  public String review(
      @Valid @ModelAttribute("form") MovementForm f,
      BindingResult b,
      Model m,
      HttpSession session) {
    if (b.hasErrors()) return movementPage(f, m);
    try {
      Product p = service.product(f.getProdutoId());
      if (!p.ativo()) throw new BusinessException("produtoId", "Selecione um produto ativo.");
      if (!"COMPRA".equals(f.getMotivo()) && f.getQuantidade() > p.saldo())
        throw new BusinessException(
            "quantidade", "Saldo insuficiente. Disponível: " + p.saldo() + " UN.");
      MovementCommand c =
          service.validate(
              new MovementCommand(
                  f.getChave(),
                  f.getProdutoId(),
                  f.getQuantidade(),
                  f.getMotivo(),
                  f.getCusto(),
                  f.getPreco(),
                  f.getJustificativa()));
      if (c.motivo().equals("VENDA") && p.preco().compareTo(c.preco()) != 0)
        throw new BusinessException("preco", "Preço alterado. Reabra a saída para atualizar.");
      synchronized (session) {
        var drafts = drafts(session);
        drafts.put(c.chave(), c);
        while (drafts.size() > 10) drafts.remove(drafts.keySet().iterator().next());
      }
      m.addAttribute("command", c);
      m.addAttribute("product", p);
      m.addAttribute(
          "total",
          (c.motivo().equals("COMPRA")
                  ? c.custo()
                  : c.motivo().equals("VENDA") ? c.preco() : p.custo())
              .multiply(java.math.BigDecimal.valueOf(c.quantidade())));
      return "movement-review";
    } catch (BusinessException e) {
      b.rejectValue(e.field(), "business", e.getMessage());
      return movementPage(f, m);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<UUID, MovementCommand> drafts(HttpSession session) {
    var map = (Map<UUID, MovementCommand>) session.getAttribute("drafts");
    if (map == null) {
      map = new LinkedHashMap<>();
      session.setAttribute("drafts", map);
    }
    return map;
  }

  @PostMapping("/movimentos/confirmar")
  public String confirm(
      @RequestParam UUID chave,
      HttpSession session,
      @AuthenticationPrincipal SessionUser u,
      RedirectAttributes flash) {
    MovementCommand c;
    synchronized (session) {
      c = drafts(session).get(chave);
    }
    if (c == null)
      throw new BusinessException("chave", "Revisão expirada. Abra uma nova movimentação.");
    try {
      var result = service.move(c, u.id);
      flash.addFlashAttribute(
          "success",
          "Movimentação #"
              + result.id()
              + " confirmada. Saldo após o registro: "
              + result.saldoApos()
              + " UN. Reenvios não geram outro registro.");
      return "redirect:/movimentos/" + result.id();
    } catch (BusinessException e) {
      flash.addFlashAttribute("error", e.getMessage());
      return "redirect:/movimentos/novo?produto=" + c.produtoId() + "&motivo=" + c.motivo();
    }
  }

  @GetMapping("/movimentos/{id}")
  public String detail(@PathVariable long id, Model m) {
    var movement =
        repo.movement(id)
            .orElseThrow(() -> new BusinessException("id", "Registro não encontrado."));
    m.addAttribute("movement", movement);
    m.addAttribute("product", service.product(movement.produtoId()));
    return "movement-detail";
  }
}
```

## src/main/java/br/com/estoquebairro/web/MovementForm.java

```java
package br.com.estoquebairro.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public class MovementForm {
  @NotNull private UUID chave = UUID.randomUUID();

  public UUID getChave() {
    return chave;
  }

  public void setChave(UUID value) {
    chave = value;
  }

  @NotNull private Long produtoId = null;

  public Long getProdutoId() {
    return produtoId;
  }

  public void setProdutoId(Long value) {
    produtoId = value;
  }

  @NotNull
  @Min(1)
  private Integer quantidade = 1;

  public Integer getQuantidade() {
    return quantidade;
  }

  public void setQuantidade(Integer value) {
    quantidade = value;
  }

  @NotBlank private String motivo = "COMPRA";

  public String getMotivo() {
    return motivo;
  }

  public void setMotivo(String value) {
    motivo = value;
  }

  private BigDecimal custo = null;

  public BigDecimal getCusto() {
    return custo;
  }

  public void setCusto(BigDecimal value) {
    custo = value;
  }

  private BigDecimal preco = null;

  public BigDecimal getPreco() {
    return preco;
  }

  public void setPreco(BigDecimal value) {
    preco = value;
  }

  @Size(max = 300)
  private String justificativa = "";

  public String getJustificativa() {
    return justificativa;
  }

  public void setJustificativa(String value) {
    justificativa = value;
  }
}
```

## src/main/java/br/com/estoquebairro/web/ProductForm.java

```java
package br.com.estoquebairro.web;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductForm {
  @NotBlank
  @Size(max = 40)
  private String sku = "";

  public String getSku() {
    return sku;
  }

  public void setSku(String value) {
    sku = value;
  }

  @NotBlank
  @Size(max = 120)
  private String nome = "";

  public String getNome() {
    return nome;
  }

  public void setNome(String value) {
    nome = value;
  }

  @NotNull private Long categoriaId = null;

  public Long getCategoriaId() {
    return categoriaId;
  }

  public void setCategoriaId(Long value) {
    categoriaId = value;
  }

  private BigDecimal custo = null;

  public BigDecimal getCusto() {
    return custo;
  }

  public void setCusto(BigDecimal value) {
    custo = value;
  }

  @NotNull
  @DecimalMin("0.01")
  @Digits(integer = 10, fraction = 2)
  private BigDecimal preco = null;

  public BigDecimal getPreco() {
    return preco;
  }

  public void setPreco(BigDecimal value) {
    preco = value;
  }

  @NotNull
  @Min(0)
  private Integer minimo = 5;

  public Integer getMinimo() {
    return minimo;
  }

  public void setMinimo(Integer value) {
    minimo = value;
  }

  private boolean ativo = true;

  public boolean getAtivo() {
    return ativo;
  }

  public void setAtivo(boolean value) {
    ativo = value;
  }
}
```

## src/main/java/br/com/estoquebairro/web/ReportController.java

```java
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
```

## src/main/java/br/com/estoquebairro/web/UserForm.java

```java
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
```

## src/main/java/br/com/estoquebairro/web/ViewFormat.java

```java
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
```

## src/main/resources/application.properties

```properties
spring.application.name=Estoque Bairro
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/estoque_bairro}
spring.datasource.username=${DB_USER:estoque}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.connection-timeout=5000
spring.flyway.enabled=true
spring.thymeleaf.cache=true
server.port=${PORT:8080}
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
server.servlet.session.cookie.secure=${COOKIE_SECURE:false}
server.error.include-message=never
server.error.include-stacktrace=never
spring.web.locale=pt_BR
spring.web.locale-resolver=fixed
app.bootstrap.login=${ADMIN_LOGIN:admin}
app.bootstrap.password=${ADMIN_PASSWORD:}
app.demo.enabled=${DEMO_DATA:false}
app.demo.password=${OPERATOR_PASSWORD:}
```

## src/main/resources/db/migration/V1__estrutura.sql

```sql
CREATE TABLE usuario (
 id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
 nome VARCHAR(100) NOT NULL CHECK (length(trim(nome)) > 0),
 login VARCHAR(60) NOT NULL UNIQUE CHECK (login = lower(trim(login)) AND length(login)>0),
 senha_hash VARCHAR(100) NOT NULL,
 perfil VARCHAR(10) NOT NULL CHECK (perfil IN ('ADMIN','OPERADOR')),
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 versao BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE categoria (
 id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
 nome VARCHAR(80) NOT NULL CHECK (length(trim(nome))>0),
 nome_normalizado VARCHAR(80) NOT NULL UNIQUE
);
CREATE TABLE produto (
 id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
 sku VARCHAR(40) NOT NULL UNIQUE CHECK (sku=upper(trim(sku)) AND length(sku)>0),
 nome VARCHAR(120) NOT NULL CHECK (length(trim(nome))>0),
 categoria_id BIGINT NOT NULL REFERENCES categoria(id),
 custo_referencia NUMERIC(12,2) NOT NULL CHECK (custo_referencia>0),
 preco_venda NUMERIC(12,2) NOT NULL CHECK (preco_venda>0),
 estoque_minimo INTEGER NOT NULL CHECK (estoque_minimo>=0),
 saldo INTEGER NOT NULL DEFAULT 0 CHECK (saldo>=0),
 ativo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE movimentacao (
 id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
 produto_id BIGINT NOT NULL REFERENCES produto(id),
 usuario_id BIGINT NOT NULL REFERENCES usuario(id),
 tipo VARCHAR(7) NOT NULL CHECK (tipo IN ('ENTRADA','SAIDA')),
 motivo VARCHAR(6) NOT NULL,
 quantidade INTEGER NOT NULL CHECK (quantidade>0),
 custo_unitario NUMERIC(12,2) NOT NULL CHECK (custo_unitario>0),
 preco_venda_unitario NUMERIC(12,2),
 data_hora TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
 justificativa VARCHAR(300) NOT NULL DEFAULT '',
 chave_requisicao UUID NOT NULL UNIQUE,
 assinatura CHAR(64) NOT NULL,
 saldo_apos INTEGER NOT NULL CHECK (saldo_apos>=0),
 CHECK ((tipo='ENTRADA' AND motivo='COMPRA' AND preco_venda_unitario IS NULL)
  OR (tipo='SAIDA' AND motivo='VENDA' AND preco_venda_unitario IS NOT NULL AND preco_venda_unitario>0)
  OR (tipo='SAIDA' AND motivo='PERDA' AND preco_venda_unitario IS NULL AND length(trim(justificativa))>0))
);
CREATE INDEX idx_produto_categoria ON produto(categoria_id);
CREATE INDEX idx_mov_produto_data ON movimentacao(produto_id,data_hora);
CREATE INDEX idx_mov_data ON movimentacao(data_hora);
CREATE INDEX idx_mov_usuario ON movimentacao(usuario_id);
CREATE FUNCTION impedir_mutacao_historico() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Histórico confirmado é imutável'; END; $$;
CREATE TRIGGER historico_imutavel BEFORE UPDATE OR DELETE ON movimentacao
 FOR EACH ROW EXECUTE FUNCTION impedir_mutacao_historico();
```

## src/main/resources/messages.properties

```properties
NotBlank=Preencha este campo.
NotNull=Preencha este campo.
Size=O texto ultrapassa o tamanho permitido.
Min=Use um valor igual ou maior que {1}.
DecimalMin=Use um valor igual ou maior que {1}.
Digits=Use até 10 dígitos inteiros e duas casas decimais.
typeMismatch.java.lang.Integer=Informe um número inteiro válido.
typeMismatch.java.lang.Long=Informe um identificador válido.
typeMismatch.java.math.BigDecimal=Informe um valor numérico válido.
typeMismatch.java.util.UUID=Abra um novo formulário e tente novamente.
```

## src/main/resources/static/app.js

```javascript
'use strict';
const here = location.pathname;
const mode = new URLSearchParams(location.search).get('motivo');
for (const link of document.querySelectorAll('[data-nav]')) {
  const target = link.dataset.nav;
  const match = target === '/' ? here === '/' : target === 'COMPRA' || target === 'VENDA' ? here === '/movimentos/novo' && (mode || 'COMPRA') === target : here.startsWith(target);
  if (match) { link.classList.add('active'); link.setAttribute('aria-current','page'); }
}
const reason = document.querySelector('#motivo');
function updateFields() {
  for (const section of document.querySelectorAll('[data-mode]')) {
    section.hidden = section.dataset.mode !== reason.value;
    const input = section.querySelector('input');
    if (input) { input.required = !section.hidden; input.disabled = section.hidden; }
  }
  const justification = document.querySelector('#justificativa');
  if (justification) justification.required = reason.value === 'PERDA';
}
if (reason) { reason.addEventListener('change',updateFields); updateFields(); }

// ---------- Modal de entrada/saída ----------
const dialog = document.getElementById('movement-dialog');
if (dialog) {
  const form = dialog.querySelector('form');
  const select = form.querySelector('#m-produto');
  const search = form.querySelector('#m-busca');
  const qty = form.querySelector('#m-quantidade');
  const reasonSelect = form.querySelector('#m-motivo');
  const cost = form.querySelector('#m-custo');
  const price = form.querySelector('#m-preco');
  const ref = form.querySelector('#m-custo-ref');
  const note = form.querySelector('#m-justificativa');
  const noteLabel = form.querySelector('#m-justificativa-label');
  const info = form.querySelector('#m-info');
  const total = form.querySelector('#m-total');
  const totalLabel = form.querySelector('#m-total-label');
  const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  let opener = null;
  let costEdited = false;

  const chosen = () => (select.value ? select.selectedOptions[0] : null);

  function sync() {
    const m = reasonSelect.value;
    const opt = chosen();
    for (const section of form.querySelectorAll('[data-dialog-mode]')) {
      const on = section.dataset.dialogMode === m;
      section.hidden = !on;
      for (const input of section.querySelectorAll('input')) {
        input.disabled = !on;
        if (input.id === 'm-custo') input.required = on;
      }
    }
    note.required = m === 'PERDA';
    noteLabel.textContent = m === 'PERDA' ? 'Justificativa da perda *' : 'Observação (opcional)';
    note.placeholder = m === 'PERDA' ? 'Ex.: embalagem danificada, produto vencido' : '';
    totalLabel.textContent = m === 'PERDA' ? 'Perda estimada' : m === 'VENDA' ? 'Total da venda' : 'Total da compra';

    qty.setCustomValidity('');
    info.classList.remove('is-error');
    if (opt) {
      const saldo = Number(opt.dataset.saldo);
      price.value = opt.dataset.preco;
      ref.value = opt.dataset.custo;
      if (!costEdited) cost.value = opt.dataset.custo;
      info.innerHTML = '';
      const strong = document.createElement('strong');
      strong.textContent = `${saldo} UN`;
      info.append('Saldo atual: ', strong, `, mínimo de ${opt.dataset.minimo} UN`);
      if (m !== 'COMPRA') {
        qty.max = saldo;
        if (Number(qty.value) > saldo) {
          const msg = `Saldo insuficiente. Disponível: ${saldo} UN.`;
          qty.setCustomValidity(msg);
          info.textContent = msg;
          info.classList.add('is-error');
        }
      } else {
        qty.removeAttribute('max');
      }
    } else {
      info.textContent = 'Escolha um produto para ver o saldo.';
    }
    const unit = parseFloat(m === 'COMPRA' ? cost.value : m === 'VENDA' ? price.value : ref.value);
    const q = parseInt(qty.value, 10);
    total.textContent = brl.format(opt && unit > 0 && q > 0 ? unit * q : 0);
  }

  search.addEventListener('input', () => {
    const term = search.value.trim().toLocaleLowerCase('pt-BR');
    const visible = [];
    for (const o of select.options) {
      if (!o.value) continue;
      const show = !term || o.textContent.toLocaleLowerCase('pt-BR').includes(term);
      o.hidden = !show;
      if (show && !o.disabled) visible.push(o);
    }
    if (visible.length === 1 && select.value !== visible[0].value) {
      select.value = visible[0].value;
      costEdited = false;
      sync();
    }
  });
  search.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') { e.preventDefault(); qty.focus(); }
  });
  select.addEventListener('change', () => { costEdited = false; sync(); });
  cost.addEventListener('input', () => { costEdited = true; sync(); });
  for (const el of [qty, reasonSelect]) {
    el.addEventListener('input', sync);
    el.addEventListener('change', sync);
  }

  function open(productId, button) {
    opener = button;
    if (productId) { select.value = productId; costEdited = false; }
    sync();
    dialog.showModal();
    (productId ? qty : search).focus();
    if (productId) qty.select();
  }
  for (const b of document.querySelectorAll('[data-open-dialog]')) {
    b.addEventListener('click', () => open(b.dataset.product, b));
  }
  for (const b of dialog.querySelectorAll('[data-close-dialog]')) {
    b.addEventListener('click', () => dialog.close());
  }
  dialog.addEventListener('click', (e) => { if (e.target === dialog) dialog.close(); });
  dialog.addEventListener('close', () => { if (opener) opener.focus(); });
  form.addEventListener('submit', (e) => {
    sync();
    if (!form.reportValidity()) e.preventDefault();
  });
  if (new URLSearchParams(location.search).has('abrir')) {
    history.replaceState(null, '', location.pathname + '?motivo=' + (reasonSelect.value === 'COMPRA' ? 'COMPRA' : 'VENDA'));
    open(null, null);
  }
  sync();
}
```

## src/main/resources/static/style.css

```css
:root{--ink:#16392e;--green:#176548;--deep:#133e30;--mint:#eef5ef;--bg:#f6f7f3;--line:#e0e5dc;--muted:#6b786e;--amber:#9e6512;--red:#a32b28;--radius:14px}*{box-sizing:border-box}body{margin:0;font-family:Inter,Segoe UI,Arial,sans-serif;color:var(--ink);background:var(--bg);font-size:14px;line-height:1.5}a{color:var(--green);text-decoration:none}a:hover{text-decoration:underline}button,input,select,textarea{font:inherit}button,a,input,select,textarea{outline-offset:4px}button:focus-visible,a:focus-visible,input:focus-visible,select:focus-visible,textarea:focus-visible{outline:3px solid #d8a441}.skip{position:fixed;top:-100px;left:10px;background:white;padding:12px;z-index:100}.skip:focus{top:10px}.sidebar{width:236px;position:fixed;inset:0 auto 0 0;background:var(--deep);color:#fff;padding:30px 22px;display:flex;flex-direction:column}.brand{display:flex;gap:12px;align-items:center;color:inherit;font-size:19px;line-height:1.12;letter-spacing:-.5px}.brand:hover{text-decoration:none}.brand-mark{background:#dcedbb;color:var(--deep);width:47px;height:47px;border-radius:13px;display:grid;place-items:center;font-weight:800;font-size:24px}.brand strong{font-weight:700}.nav-caption{font-size:10px;letter-spacing:1.8px;color:#aac2b1;display:block;margin:36px 12px 12px}.sidebar nav a{display:flex;gap:13px;align-items:center;color:#d6e3d8;border-radius:8px;padding:12px;margin-bottom:5px;font-size:17px}.sidebar nav a span{font-size:13px;font-weight:500}.sidebar nav a:hover,.sidebar nav a.active{background:#2c5843;color:#fff;text-decoration:none}.sidebar-bottom{margin-top:auto;padding-top:25px;border-top:1px solid #41624e;font-size:12px}.sidebar-bottom small{display:block;color:#aac2b1;margin-top:6px;font-size:10px}.status-dot{display:inline-block;width:6px;height:6px;border-radius:50%;background:#d3e6aa;margin-right:7px}.workspace{margin-left:236px;min-height:100vh;display:flex;flex-direction:column}.topbar{height:76px;background:#fff;border-bottom:1px solid var(--line);display:flex;align-items:center;justify-content:space-between;padding:0 38px;font-size:12px;gap:15px}.account{display:flex;gap:13px;align-items:center}.account form{margin:0}.text-button{border:0;background:none;color:var(--muted);cursor:pointer;font-size:12px}.muted{color:var(--muted)}main{padding:34px 38px;flex:1;min-width:0}.page-heading{display:flex;gap:20px;align-items:center;justify-content:space-between;margin-bottom:25px}.eyebrow{font-size:10px;letter-spacing:1.7px;font-weight:700;margin:0 0 9px;color:var(--green)}h1{font-size:30px;letter-spacing:-.9px;line-height:1.2;margin:0 0 10px}h2{font-size:17px;line-height:1.35;letter-spacing:-.2px;margin:0 0 8px}p{margin:0 0 15px}.page-heading p:last-child{margin:0;font-size:13px}.button{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:42px;padding:10px 18px;border:1px solid var(--green);border-radius:8px;background:var(--green);color:white;font-size:13px;font-weight:600;cursor:pointer;white-space:nowrap}.button:hover{background:#104d35;text-decoration:none}.button.secondary{background:white;border-color:var(--line);color:var(--ink)}.button.secondary:hover{background:var(--mint)}.filterbar{display:flex;gap:14px;align-items:end;flex-wrap:wrap;background:#fff;border:1px solid var(--line);border-radius:var(--radius);padding:17px 20px;margin-bottom:22px}.filterbar strong{margin-right:auto;align-self:center;font-size:13px}.filterbar label{font-size:11px;color:var(--muted);display:flex;flex-direction:column;gap:5px}.filterbar input,.filterbar select{height:38px;min-width:110px}.filterbar input[type=number]{max-width:145px}.filterbar a{align-self:center;font-size:12px}.grow{flex:1}.stats{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px;margin-bottom:18px}.stat{background:#fff;border:1px solid var(--line);border-radius:var(--radius);padding:22px;min-height:154px}.stat>span{font-size:12px;color:var(--muted)}.stat strong{display:block;font-size:32px;letter-spacing:-1px;font-weight:650;margin:10px 0 4px}.stat small{font-size:10px;color:var(--muted)}.stat.featured{background:var(--deep);border-color:var(--deep);color:#fff}.stat.featured>span,.stat.featured small{color:#c4d9cb}.stat.attention strong{color:var(--amber)}.mini .stat{min-height:126px;padding:18px 22px}.mini .stat strong{font-size:24px;margin:8px 0}.panel{background:white;border:1px solid var(--line);border-radius:var(--radius);margin-bottom:22px;overflow:hidden}.panel-heading{padding:22px 22px 12px;display:flex;justify-content:space-between;align-items:center;gap:15px}.panel-heading h2{margin:0}.panel-heading p{font-size:12px;margin:5px 0 0}.panel-heading>a{font-size:12px}.table-scroll{overflow:auto}table{border-collapse:collapse;width:100%;text-align:left;font-size:12px}th{font-size:10px;text-transform:uppercase;letter-spacing:.5px;color:var(--muted);background:#fafbf8;font-weight:600}th,td{padding:14px 20px;border-bottom:1px solid var(--line);vertical-align:middle}tr:last-child td{border-bottom:0}td strong{font-size:12px}td small{display:block;color:var(--muted);font-size:10px;margin-top:3px}td .badge{margin-top:3px}.badge{border-radius:5px;background:var(--mint);color:var(--green);font-size:10px;padding:4px 8px;white-space:nowrap}.badge.warning{background:#fff2db;color:#8b590a}.actions{display:flex;flex-wrap:wrap;gap:8px 12px;font-size:11px;min-width:90px}.empty{padding:35px 20px;text-align:center;color:var(--muted)}.pagination{display:flex;align-items:center;justify-content:center;gap:25px;padding:18px;font-size:12px;border-top:1px solid var(--line)}input,select,textarea{border:1px solid #cad4c9;border-radius:7px;background:#fff;color:var(--ink);padding:10px 12px;max-width:100%;min-width:0}input:read-only{background:#f3f5f0;color:var(--muted)}textarea{resize:vertical}.field{display:flex;flex-direction:column;gap:7px;margin:0 0 18px;min-width:0}.field label{font-size:12px;font-weight:600}.field input,.field select{width:100%;height:44px}.form-panel{padding:28px;max-width:840px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 22px}.form-actions{display:flex;gap:12px;margin-top:25px;align-items:center}.hint{color:var(--muted);font-size:12px}.field-error{color:var(--red);font-size:12px}.notice{border:1px solid #d8dfcc;background:#f7f9ed;padding:14px 18px;border-radius:8px;margin-bottom:20px;font-size:13px}.notice.error{background:#fff0ee;border-color:#efc9c3;color:var(--red)}.notice.success{background:#edf7ef;border-color:#b6d9bd;color:#245735}.checkbox{display:flex!important;flex-direction:row!important;align-items:center;gap:9px!important;font-size:12px}.checkbox input{width:16px;height:16px;min-width:16px}.product-summary{display:flex;justify-content:space-between;gap:20px;padding-bottom:22px;margin-bottom:25px;border-bottom:1px solid var(--line);align-items:center}.product-summary>div:last-child strong{display:block;font-size:23px}.product-summary h2{font-size:23px;margin:5px 0}.product-summary span{font-size:12px}.review{display:grid;grid-template-columns:1fr 1fr;margin:0 0 25px;gap:15px}.review dt{color:var(--muted);font-size:13px}.review dd{margin:0;text-align:right;font-weight:600;overflow-wrap:anywhere}.two-columns{display:grid;grid-template-columns:1fr 1fr;gap:22px;align-items:start}.footer{padding:20px 38px;border-top:1px solid var(--line);font-size:11px;color:var(--muted);display:flex;justify-content:space-between}.login-page{display:grid;grid-template-columns:48% 52%;min-height:100vh}.login-story{background:var(--deep);color:#f8faef;padding:50px 60px;display:flex;flex-direction:column;justify-content:space-between;min-height:100vh}.login-story>div{margin:70px 0}.login-story .eyebrow{color:#d8e9b6}.login-story h1{font-size:64px;line-height:1.08;letter-spacing:-3px;margin:24px 0}.login-story p{color:#bed2c4;font-size:17px;line-height:1.8}.login-story>small{color:#aac2b1;font-size:11px}.login-art{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-top:50px}.login-art>div{background:#29523e;border:1px solid #42674e;border-radius:12px;padding:18px 12px;font-size:22px}.login-art strong{display:block;font-size:13px;margin:15px 0 5px}.login-art span{display:block;font-size:10px;color:#c2d4c5}.login-main{padding:40px;display:flex;flex-direction:column;justify-content:center;align-items:center;position:relative}.login-form{width:100%;max-width:365px}.login-form h2{font-size:30px;letter-spacing:-.8px;margin:14px 0}.login-form .muted{font-size:13px;margin-bottom:30px}.login-form .field{margin-bottom:22px}.login-form .hint{font-size:11px;margin-top:23px;text-align:center}.full{width:100%}.login-footer{position:absolute;bottom:34px;font-size:10px;color:var(--muted)}[hidden]{display:none!important}@media(min-width:1600px){main{max-width:1500px;width:100%;margin:auto}}@media(max-width:1100px){.sidebar{width:205px;padding:25px 16px}.workspace{margin-left:205px}.topbar{padding:0 24px}main{padding:28px 24px}.stat{padding:17px}.stat strong{font-size:27px}.login-story{padding:40px}.login-story h1{font-size:53px}.login-art{gap:8px}}@media(max-width:800px){.sidebar{position:static;width:100%;padding:18px}.sidebar .brand{font-size:16px}.sidebar .brand-mark{width:35px;height:35px;font-size:19px}.sidebar>.nav-caption,.sidebar-bottom,.sidebar nav .nav-caption{display:none}.sidebar nav{display:flex;flex-wrap:wrap;margin-top:12px;gap:4px}.sidebar nav>div{display:contents}.sidebar nav a{padding:6px 8px;margin:0}.sidebar nav a span{font-size:11px}.workspace{margin:0}.topbar{height:auto;min-height:60px;flex-wrap:wrap;padding:14px 20px}.topbar>.muted{display:none}main{padding:24px 18px}.stats{gap:10px}.stat strong{font-size:24px}.stat small{font-size:10px}.page-heading{align-items:start;flex-direction:column}h1{font-size:26px}.form-panel{padding:20px}.two-columns{grid-template-columns:1fr}.footer{padding:20px;gap:15px}.login-page{grid-template-columns:1fr}.login-story{display:none}.login-main{min-height:100vh;padding:30px 22px}}@media(max-width:540px){.stats{grid-template-columns:1fr}.stat{min-height:115px}.stat strong{margin:6px 0}.form-grid{grid-template-columns:1fr}.filterbar{padding:15px;gap:10px}.filterbar label{flex:1;min-width:120px}.filterbar strong{width:100%}.filterbar input,.filterbar select{width:100%}.form-actions{flex-wrap:wrap}.account .badge{display:none}.footer span{max-width:60%;text-align:right}.review{grid-template-columns:1fr 1fr}.product-summary{align-items:start}.product-summary h2{font-size:20px}.login-footer{max-width:80%;text-align:center}}

.icon{display:inline-block;width:18px;height:18px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round;vertical-align:middle;flex-shrink:0}

/* ---------- Modal de movimentação ---------- */
.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap}
.button.small{min-height:34px;padding:6px 12px;font-size:12px}
.row-action{text-align:right;white-space:nowrap}
.badge.danger{background:#fbe8e6;color:var(--red)}
.dialog{border:0;padding:0;border-radius:16px;width:min(620px,calc(100vw - 32px));max-height:calc(100vh - 48px);color:var(--ink);box-shadow:0 24px 60px rgba(19,62,48,.28)}
.dialog::backdrop{background:rgba(19,62,48,.45)}
.dialog[open]{animation:dialog-in .18s ease-out}
@keyframes dialog-in{from{opacity:0;transform:translateY(8px) scale(.98)}to{opacity:1;transform:none}}
@media(prefers-reduced-motion:reduce){.dialog[open]{animation:none}}
.dialog-form{display:flex;flex-direction:column;max-height:calc(100vh - 48px)}
.dialog-head{display:flex;justify-content:space-between;align-items:start;gap:16px;padding:24px 26px 14px;border-bottom:1px solid var(--line)}
.dialog-head h2{font-size:21px;margin:0 0 4px}
.dialog-head p{margin:0;font-size:12px}
.dialog-close{border:0;background:var(--mint);color:var(--ink);width:34px;height:34px;border-radius:8px;font-size:20px;line-height:1;cursor:pointer;flex:none}
.dialog-close:hover{background:#dfeee1}
.dialog-body{padding:20px 26px 4px;overflow:auto}
.dialog-body .field{margin-bottom:16px}
.dialog-body textarea{width:100%}
.product-filter{height:38px!important;margin-bottom:6px;font-size:13px}
.product-info{margin:2px 0 0;font-size:12px;color:var(--muted)}
.product-info strong{color:var(--ink)}
.product-info.is-error{color:var(--red);font-weight:600}
.dialog-body .hint{margin:0;font-size:11px}
.dialog-foot{display:flex;justify-content:space-between;align-items:center;gap:16px;padding:16px 26px 22px;border-top:1px solid var(--line);background:#fafbf8;border-radius:0 0 16px 16px}
.dialog-total span{display:block;font-size:11px;color:var(--muted)}
.dialog-total strong{font-size:22px;letter-spacing:-.5px}
.dialog-actions{display:flex;gap:10px}

/* ---------- Relatórios ---------- */
.report-filter{align-items:center}
.segmented{display:flex;background:var(--bg);border:1px solid var(--line);border-radius:9px;padding:3px;gap:2px;flex-wrap:wrap}
.segmented a{padding:8px 14px;border-radius:7px;font-size:12px;font-weight:600;color:var(--muted)}
.segmented a:hover{color:var(--ink);text-decoration:none;background:#fff}
.segmented a.active{background:var(--deep);color:#fff}
.custom-range{display:flex;gap:10px;align-items:end;margin-left:auto;flex-wrap:wrap}
.custom-range label{font-size:11px;color:var(--muted);display:flex;flex-direction:column;gap:5px}
.custom-range input{height:38px}
.custom-range .button.active{border-color:var(--deep);box-shadow:inset 0 0 0 1px var(--deep)}
.stat .delta{display:block;margin-top:8px;font-weight:600;color:var(--muted)}
.stat .delta.good{color:#2d7a4f}
.stat .delta.bad{color:var(--red)}
.stat.featured .delta.good{color:#cde8a6}
.stat.featured .delta.bad{color:#ffb4aa}
.stat strong.loss{color:var(--red)}
.kpis{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));background:#fff;border:1px solid var(--line);border-radius:var(--radius);margin-bottom:22px}
.kpis>div{padding:18px 20px;border-right:1px solid var(--line);border-bottom:1px solid var(--line);min-width:0}
.kpis>div:nth-child(4n){border-right:0}
.kpis>div:nth-last-child(-n+4){border-bottom:0}
.kpis span{display:block;font-size:11px;color:var(--muted)}
.kpis strong{display:block;font-size:20px;letter-spacing:-.4px;margin:6px 0 2px;font-weight:650;overflow-wrap:anywhere}
.kpis strong.text-value{font-size:16px}
.kpis small{font-size:10px;color:var(--muted)}
.legend{list-style:none;display:flex;gap:16px;margin:0;padding:0;font-size:11px;color:var(--muted);flex-wrap:wrap}
.legend li::before{content:"";display:inline-block;width:10px;height:10px;border-radius:3px;margin-right:6px;vertical-align:-1px}
.legend .entrada::before{background:var(--green)}
.legend .venda::before{background:#d8a441}
.legend .perda::before{background:#c25b52}
.chart-wrap{padding:6px 18px 18px;overflow-x:auto}
.chart{display:block;width:100%;height:auto;max-height:330px}
.chart .grid line{stroke:var(--line);stroke-width:1}
.chart .grid text,.chart .axis-month{font-size:11px;fill:var(--muted)}
.chart .axis-month{text-transform:capitalize}
.chart .bar.entrada{fill:var(--green)}
.chart .bar.venda{fill:#d8a441}
.chart .bar.perda{fill:#c25b52}
.chart .bar:hover{opacity:.8}
.chart-empty{font-size:13px;fill:var(--muted)}
th.num,td.num{text-align:right;white-space:nowrap}
td.num small{margin-top:2px}
.rank{display:inline-grid;place-items:center;width:20px;height:20px;border-radius:6px;background:var(--mint);color:var(--green);font-size:10px;font-weight:700;margin-right:8px;vertical-align:1px}
.monthly tr.quiet td{color:#9aa69c}
.monthly td:first-child strong{text-transform:capitalize}
.monthly tfoot th{background:var(--mint);color:var(--ink);font-size:12px;text-transform:none;letter-spacing:0;border-bottom:0}
.footnote{font-size:11px;color:var(--muted);max-width:760px}
@media(max-width:1100px){.kpis{grid-template-columns:repeat(2,minmax(0,1fr))}.kpis>div:nth-child(4n){border-right:1px solid var(--line)}.kpis>div:nth-child(2n){border-right:0}.kpis>div:nth-last-child(-n+4){border-bottom:1px solid var(--line)}.kpis>div:nth-last-child(-n+2){border-bottom:0}.custom-range{margin-left:0}}
@media(max-width:540px){.dialog-foot{flex-direction:column;align-items:stretch}.dialog-actions .button{flex:1}.dialog-head,.dialog-body,.dialog-foot{padding-left:18px;padding-right:18px}.kpis{grid-template-columns:1fr}.kpis>div{border-right:0!important;border-bottom:1px solid var(--line)!important}.kpis>div:last-child{border-bottom:0!important}}
```

## src/main/resources/templates/categories.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Categorias')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">ADMINISTRAÇÃO</p><h1>Categorias</h1><p class="muted">Organize os produtos por grupos, sem duplicar nomes.</p></div></div><div class="two-columns"><section class="panel"><div class="panel-heading"><h2>Categorias cadastradas</h2></div><table><thead><tr><th>Nome</th><th>Ação</th></tr></thead><tbody><tr th:each="c:${categories}"><td th:text="${c.nome}"></td><td><a th:href="@{/admin/categorias/{id}(id=${c.id})}">Editar</a></td></tr><tr th:if="${#lists.isEmpty(categories)}"><td colspan="2" class="empty">Cadastre a primeira categoria.</td></tr></tbody></table></section><form class="panel form-panel" th:action="${id==null?'/admin/categorias':'/admin/categorias/'+id}" method="post" th:object="${form}"><h2 th:text="${id==null?'Nova categoria':'Editar categoria'}"></h2><div class="field"><label for="nome">Nome *</label><input id="nome" type="text" th:field="*{nome}" required maxlength="80"><div th:replace="~{fragments :: fieldError('nome')}"></div></div><div class="form-actions"><button class="button">Salvar categoria</button><a class="button secondary" href="/admin/categorias">Limpar</a></div></form></div></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/dashboard.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Visão geral')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">PAINEL DA LOJA</p><h1>Visão geral</h1><p class="muted">Acompanhe o estoque e as movimentações do seu mercado.</p></div><a class="button" href="/movimentos/novo?motivo=COMPRA&amp;abrir=1">＋ Registrar entrada</a></div>
<form class="filterbar" action="/" method="get"><strong>Movimentações no período</strong><label>De <input aria-label="Data inicial" type="date" name="inicio" th:value="${inicio}"></label><label>Até <input aria-label="Data final" type="date" name="fim" th:value="${fim}"></label><button class="button secondary">Aplicar período</button></form>
<section class="stats" aria-label="Indicadores"><article class="stat featured"><span>Valor estimado do estoque</span><strong th:text="${@view.money(metrics.valor)}"></strong><small>Saldo atual × último custo de entrada</small></article><article class="stat"><span>Produtos ativos</span><strong th:text="${metrics.ativos}"></strong><small>Itens disponíveis para movimentação</small></article><article class="stat attention"><span>Estoque em atenção</span><strong th:text="${metrics.baixos}"></strong><small>Saldo igual ou abaixo do mínimo</small></article></section>
<section class="stats mini"><article class="stat"><span><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 5 5 19M5 5v14h14"/></svg> Entradas no período</span><strong th:text="${@view.money(metrics.entradas)}"></strong><small>Quantidade × custo de compra</small></article><article class="stat"><span><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg> Saídas por venda</span><strong th:text="${@view.money(metrics.vendas)}"></strong><small>Vendas registradas; não comprova recebimento</small></article><article class="stat"><span>− Perdas estimadas</span><strong th:text="${@view.money(metrics.perdas)}"></strong><small>Quantidade × custo registrado na perda</small></article></section>
<section class="panel"><div class="panel-heading"><div><h2>Reposição em atenção</h2><p class="muted">Produtos ativos que atingiram o estoque mínimo.</p></div><a href="/produtos?baixo=true">Ver todos →</a></div><div class="table-scroll"><table><thead><tr><th>Produto</th><th>SKU</th><th>Saldo</th><th>Mínimo</th><th>Ação</th></tr></thead><tbody><tr th:each="p:${low.items}"><td><strong th:text="${p.nome}"></strong></td><td th:text="${p.sku}"></td><td><span class="badge warning" th:text="${p.saldo}+' UN'"></span></td><td th:text="${p.minimo}+' UN'"></td><td><a th:href="@{/movimentos/novo(produto=${p.id},motivo='COMPRA')}">Registrar entrada</a></td></tr><tr th:if="${low.total==0}"><td colspan="5" class="empty">Nenhum produto precisa de reposição neste momento.</td></tr></tbody></table></div></section>
<section class="panel"><div class="panel-heading"><div><h2>Unidades movimentadas por produto</h2><p class="muted">Unidades comerciais (UN), no período selecionado.</p></div></div><div class="table-scroll"><table><thead><tr><th>Produto / SKU</th><th>Entradas</th><th>Vendas</th><th>Perdas</th></tr></thead><tbody><tr th:each="p:${quantities}"><td><span th:text="${p.nome}"></span><small th:text="${p.sku}"></small></td><td th:text="${p.entradas}"></td><td th:text="${p.vendas}"></td><td th:text="${p.perdas}"></td></tr><tr th:if="${#lists.isEmpty(quantities)}"><td colspan="4" class="empty">Ainda não há movimentações neste período.</td></tr></tbody></table></div></section></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/denied.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Acesso não permitido')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">ATENÇÃO</p><h1>Acesso não permitido</h1><p class="muted"></p></div></div><section class="panel form-panel"><p th:text="${message?:'Seu perfil não tem acesso a esta operação, ou a sessão do formulário expirou. Reabra a página e tente novamente.'}"></p><a class="button" href="/">Voltar ao início</a><a class="button secondary" href="/login">Acessar novamente</a></section></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/error.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Não foi possível concluir')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">ATENÇÃO</p><h1>Não foi possível concluir</h1><p class="muted"></p></div></div><section class="panel form-panel"><p th:text="${message?:'Ocorreu um erro. Confira os dados e tente novamente.'}"></p><a class="button" href="/">Voltar ao início</a><a class="button secondary" href="/login">Acessar novamente</a></section></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/fragments.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:fragment="head(title)"><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title th:text="${title} + ' • Estoque Bairro'">Estoque Bairro</title><link rel="stylesheet" th:href="@{/style.css}"><script defer th:src="@{/app.js}"></script></head><body>
<aside th:fragment="nav" class="sidebar"><a class="brand" th:href="@{/}"><span class="brand-mark">eb.</span><span>Estoque<br><strong>Bairro</strong></span></a><span class="nav-caption">SUA OPERAÇÃO</span><nav aria-label="Menu principal"><a href="/" data-nav="/"><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 3h18v18H3zM10 3v18"/></svg> <span>Visão geral</span></a><a href="/produtos" data-nav="/produtos"><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 3h18v18H3zM3 10h18M10 3v18"/></svg> <span>Produtos</span></a><a href="/movimentos/novo?motivo=COMPRA" data-nav="COMPRA"><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 5 5 19M5 5v14h14"/></svg> <span>Registrar entrada</span></a><a href="/movimentos/novo?motivo=VENDA" data-nav="VENDA"><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg> <span>Registrar saída</span></a><a href="/historico" data-nav="/historico"><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16"/></svg> <span>Histórico</span></a><a href="/relatorios" data-nav="/relatorios"><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20V11M10 20V5M16 20v-6M2 20h20"/></svg> <span>Relatórios</span></a><div sec:authorize="hasRole('ADMIN')"><span class="nav-caption">ADMINISTRAÇÃO</span><a href="/admin/categorias" data-nav="/admin/categorias"><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 9 9-9 9-9-9z"/></svg> <span>Categorias</span></a><a href="/admin/usuarios" data-nav="/admin/usuarios"><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M8 9a4 4 0 1 0 8 0 4 4 0 1 0-8 0M4 21v-2a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v2"/></svg> <span>Usuários e acesso</span></a></div></nav><div class="sidebar-bottom"><span class="status-dot"></span> Loja de demonstração<small>Uma loja. Tudo sob controle.</small></div></aside>
<header th:fragment="top" class="topbar"><span>Mercado de bairro <span class="muted">/ Gestão de estoque</span></span><div class="account"><span sec:authentication="name">usuário</span><span class="badge" sec:authorize="hasRole('ADMIN')">Administrador</span><span class="badge" sec:authorize="hasRole('OPERADOR')">Operador</span><form th:action="@{/logout}" method="post"><button class="text-button" type="submit">Sair <svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg></button></form></div></header>
<div th:fragment="alerts"><div th:if="${success}" class="notice success" role="status" th:text="${success}"></div><div th:if="${error}" class="notice error" role="alert" th:text="${error}"></div></div>
<div th:fragment="fieldError(name)" class="field-error" th:if="${#fields.hasErrors(name)}" th:errors="*{__${name}__}" role="alert"></div>
<footer th:fragment="footer" class="footer">Estoque Bairro <span>Controle simples, registros confiáveis. • v1.0.0</span></footer>
</body></html>
```

## src/main/resources/templates/history.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Histórico')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">RASTREABILIDADE</p><h1>Histórico de movimentações</h1><p class="muted">Entradas, vendas e perdas com data, valores e responsável.</p></div></div><form action="/historico" method="get" class="filterbar"><label>De <input aria-label="Data inicial" type="date" name="inicio" th:value="${inicio}"></label><label>Até <input aria-label="Data final" type="date" name="fim" th:value="${fim}"></label><label>ID do produto<input type="number" name="produto" min="1" th:value="${produto}" placeholder="Todos"></label><label>Tipo<select name="tipo"><option value="">Todos</option><option value="ENTRADA" th:selected="${tipo=='ENTRADA'}">Entrada</option><option value="SAIDA" th:selected="${tipo=='SAIDA'}">Saída</option></select></label><button class="button secondary">Filtrar</button><a href="/historico">Limpar</a></form><section class="panel"><div class="panel-heading"><h2>Registros preservados</h2><span class="muted" th:text="${result.total}+' movimentações'"></span></div><div class="table-scroll"><table><thead><tr><th>Registro / data</th><th>Produto</th><th>Motivo</th><th>Qtd.</th><th>Custo unit.</th><th>Venda unit.</th><th>Responsável</th></tr></thead><tbody><tr th:each="r:${result.items}"><td><a th:href="@{/movimentos/{id}(id=${r.id})}" th:text="'#'+${r.id}"></a><small th:text="${@view.date(r.dataHora)}"></small></td><td><strong th:text="${r.produto}"></strong><small th:text="${r.sku}"></small></td><td><span class="badge" th:text="${r.motivo}"></span></td><td th:text="${r.quantidade}+' UN'"></td><td th:text="${@view.money(r.custo)}"></td><td th:text="${r.preco!=null?@view.money(r.preco):'—'}"></td><td th:text="${r.usuario}"></td></tr><tr th:if="${result.total==0}"><td colspan="7" class="empty">Nenhuma movimentação neste período.</td></tr></tbody></table></div><div class="pagination"><a th:if="${result.page>0}" th:href="@{/historico(inicio=${inicio},fim=${fim},produto=${produto},tipo=${tipo},pagina=${result.page-1})}">← Anterior</a><span th:text="'Página '+${result.page+1}+' de '+${result.pages()}"></span><a th:if="${result.page+1<result.pages()}" th:href="@{/historico(inicio=${inicio},fim=${fim},produto=${produto},tipo=${tipo},pagina=${result.page+1})}">Próxima →</a></div></section></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/login.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org"><head th:replace="~{fragments :: head('Entrar')}"></head><body class="login-page"><section class="login-story"><a class="brand" href="/login"><span class="brand-mark">eb.</span><span>Estoque<br><strong>Bairro</strong></span></a><div><span class="eyebrow">FEITO PARA O SEU MERCADO</span><h1>Seu estoque,<br>em ordem.</h1><p>Da primeira entrada à última saída.<br>Mais clareza para cuidar do que é seu.</p><div class="login-art" aria-hidden="true"><div><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M19 5 5 19M5 5v14h14"/></svg><strong>Entradas</strong><span>Reposição organizada</span></div><div><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 3h18v18H3zM3 10h18M10 3v18"/></svg><strong>Estoque</strong><span>Saldos confiáveis</span></div><div><svg class="icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 19 19 5M5 5h14v14"/></svg><strong>Saídas</strong><span>Histórico preservado</span></div></div></div><small>Estoque Bairro • Gestão de estoque</small></section><main class="login-main"><form th:action="@{/login}" method="post" class="login-form"><span class="eyebrow">BEM-VINDO DE VOLTA</span><h2>Acesse sua loja</h2><p class="muted">Entre com a conta fornecida pelo administrador.</p><div class="notice error" th:if="${param.erro}" role="alert">Login ou senha inválidos.</div><div class="notice" th:if="${param.saiu}" role="status">Você saiu com segurança.</div><div class="notice" th:if="${param.expirou}" role="alert">Sua sessão expirou ou o acesso foi alterado. Entre novamente.</div><div class="field"><label for="username">Login</label><input id="username" name="username" required autocomplete="username" maxlength="60" autofocus></div><div class="field"><label for="password">Senha</label><input id="password" name="password" type="password" required autocomplete="current-password"></div><button class="button full">Entrar →</button><p class="hint">Precisa de acesso? Solicite uma conta ao administrador da loja.</p></form><small class="login-footer">Um lugar para cada produto. Um registro para cada movimento.</small></main></body></html>
```

## src/main/resources/templates/movement-detail.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Comprovante de movimentação')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">HISTÓRICO</p><h1>Registro confirmado</h1><p class="muted">Esta movimentação não pode ser editada ou excluída.</p></div></div><section class="panel form-panel"><div class="product-summary"><div><span class="eyebrow" th:text="'MOVIMENTAÇÃO #'+${movement.id}"></span><h2 th:text="${movement.produto}"></h2><span th:text="${movement.sku}"></span></div><span class="badge" th:text="${movement.motivo}"></span></div><dl class="review"><dt>Quantidade</dt><dd th:text="${movement.quantidade}+' UN'"></dd><dt>Custo unitário registrado</dt><dd th:text="${@view.money(movement.custo)}"></dd><dt th:if="${movement.preco!=null}">Preço unitário registrado</dt><dd th:if="${movement.preco!=null}" th:text="${@view.money(movement.preco)}"></dd><dt>Saldo após este registro</dt><dd th:text="${movement.saldoApos}+' UN'"></dd><dt>Saldo atual do produto</dt><dd th:text="${product.saldo}+' UN'"></dd><dt>Data (São Paulo)</dt><dd th:text="${@view.date(movement.dataHora)}"></dd><dt>Responsável</dt><dd th:text="${movement.usuario}"></dd><dt>Justificativa</dt><dd th:text="${#strings.isEmpty(movement.justificativa)?'Não informada':movement.justificativa}"></dd></dl><div class="form-actions"><a class="button" href="/produtos">Voltar aos produtos</a><a class="button secondary" href="/historico">Ver histórico</a></div></section></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/movement-form.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Movimentação')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div th:if="${product!=null}" class="page-heading"><div><p class="eyebrow">MOVIMENTAÇÃO</p><h1><span th:text="${form.motivo=='COMPRA'?'Registrar entrada':'Registrar saída'}"></span></h1><p class="muted">Confira o produto e os valores. Você revisará o resumo antes de confirmar.</p></div></div>
<th:block th:if="${product==null}" th:with="entry=${form.motivo=='COMPRA'}">
<div class="page-heading"><div><p class="eyebrow">MOVIMENTAÇÃO</p><h1 th:text="${entry?'Entradas':'Saídas'}">Entradas</h1><p class="muted" th:text="${entry?'Compras que repõem o estoque. Cada entrada atualiza o último custo do produto.':'Vendas e perdas que reduzem o estoque. Toda saída é conferida contra o saldo.'}"></p></div><button type="button" class="button" data-open-dialog th:text="${entry?'＋ Registrar entrada':'＋ Registrar saída'}">＋ Registrar entrada</button></div>
<div th:object="${form}"><div th:if="${#fields.hasAnyErrors()}" class="notice error" role="alert">A movimentação não foi enviada. Escolha um produto da lista e informe uma quantidade inteira maior que zero.</div></div>
<section class="stats mini" aria-label="Resumo do mês">
<th:block th:if="${entry}"><article class="stat"><span>Entradas neste mês</span><strong th:text="${@view.money(metrics.entradas)}"></strong><small>Quantidade × custo de compra</small></article><article class="stat"><span>Unidades recebidas neste mês</span><strong th:text="${@view.number(monthQuantities)}+' UN'"></strong><small>Soma das compras registradas</small></article><article class="stat attention"><span>Produtos para repor</span><strong th:text="${metrics.baixos}"></strong><small>Saldo igual ou abaixo do mínimo</small></article></th:block>
<th:block th:unless="${entry}"><article class="stat"><span>Vendas neste mês</span><strong th:text="${@view.money(metrics.vendas)}"></strong><small>Preço praticado; não comprova recebimento</small></article><article class="stat"><span>Perdas neste mês</span><strong th:text="${@view.money(metrics.perdas)}"></strong><small>Valorizadas pelo custo de referência</small></article><article class="stat"><span>Unidades que saíram neste mês</span><strong th:text="${@view.number(monthQuantities)}+' UN'"></strong><small>Vendas e perdas somadas</small></article></th:block>
</section>
<section th:if="${entry and low.total>0}" class="panel"><div class="panel-heading"><div><h2>Produtos para repor</h2><p class="muted">Saldo igual ou abaixo do mínimo. Registre a compra direto daqui.</p></div><a href="/produtos?baixo=true">Ver todos</a></div><div class="table-scroll"><table><thead><tr><th>Produto</th><th>Saldo</th><th>Mínimo</th><th>Último custo</th><th><span class="sr-only">Ação</span></th></tr></thead><tbody><tr th:each="p:${low.items}"><td><strong th:text="${p.nome}"></strong><small th:text="${p.sku}"></small></td><td><span class="badge warning" th:text="${p.saldo}+' UN'"></span></td><td th:text="${p.minimo}+' UN'"></td><td th:text="${@view.money(p.custo)}"></td><td class="row-action"><button type="button" class="button secondary small" data-open-dialog th:attr="data-product=${p.id}">Registrar entrada</button></td></tr></tbody></table></div></section>
<section class="panel"><div class="panel-heading"><div><h2 th:text="${entry?'Últimas entradas':'Últimas saídas'}"></h2><p class="muted">Os 12 registros mais recentes. O histórico completo fica em Histórico.</p></div><a th:href="@{/historico(tipo=${entry?'ENTRADA':'SAIDA'})}">Abrir histórico</a></div><div class="table-scroll"><table><thead><tr><th>Data</th><th>Produto</th><th th:unless="${entry}">Motivo</th><th>Quantidade</th><th>Valor unitário</th><th>Total</th><th>Responsável</th></tr></thead><tbody><tr th:each="mv:${recent}" th:with="unit=${mv.motivo=='VENDA'?mv.preco:mv.custo}"><td><a th:href="@{/movimentos/{id}(id=${mv.id})}" th:text="${@view.shortDate(mv.dataHora)}"></a></td><td><strong th:text="${mv.produto}"></strong><small th:text="${mv.sku}"></small></td><td th:unless="${entry}"><span class="badge" th:classappend="${mv.motivo=='PERDA'?'danger':''}" th:text="${mv.motivo=='VENDA'?'Venda':'Perda'}"></span></td><td th:text="${mv.quantidade}+' UN'"></td><td th:text="${@view.money(unit)}"></td><td th:text="${@view.total(unit,mv.quantidade)}"></td><td th:text="${mv.usuario}"></td></tr><tr th:if="${#lists.isEmpty(recent)}"><td th:attr="colspan=${entry?6:7}" class="empty" th:text="${entry?'Nenhuma entrada registrada ainda. Use Registrar entrada para lançar a primeira compra.':'Nenhuma saída registrada ainda. Use Registrar saída para lançar uma venda ou perda.'}"></td></tr></tbody></table></div></section>
<dialog id="movement-dialog" class="dialog" aria-labelledby="dialog-title"><form th:action="@{/movimentos/revisar}" method="post" class="dialog-form">
<div class="dialog-head"><div><h2 id="dialog-title" th:text="${entry?'Registrar entrada':'Registrar saída'}"></h2><p class="muted">Nada é gravado agora. Você confere o resumo na próxima tela.</p></div><button type="button" class="dialog-close" data-close-dialog aria-label="Fechar">×</button></div>
<div class="dialog-body"><input type="hidden" name="chave" th:value="${modalForm.chave}">
<div class="field"><label for="m-produto">Produto *</label><input id="m-busca" type="search" class="product-filter" placeholder="Filtrar por nome ou SKU" autocomplete="off" aria-label="Filtrar produtos" aria-controls="m-produto"><select id="m-produto" name="produtoId" required><option value="">Selecione o produto</option><option th:each="p:${modalProducts}" th:value="${p.id}" th:text="${p.nome}+' ('+${p.sku}+')'+${!entry and p.saldo==0?' sem saldo':''}" th:disabled="${!entry and p.saldo==0}" th:attr="data-saldo=${p.saldo},data-minimo=${p.minimo},data-custo=${p.custo},data-preco=${p.preco}"></option></select><p class="product-info" id="m-info" aria-live="polite">Escolha um produto para ver o saldo.</p></div>
<div class="form-grid"><div class="field"><label for="m-quantidade">Quantidade (UN) *</label><input id="m-quantidade" name="quantidade" type="number" min="1" step="1" value="1" required inputmode="numeric"></div>
<div class="field"><label for="m-motivo">Motivo *</label><select id="m-motivo" name="motivo" required><option th:if="${entry}" value="COMPRA">Compra de fornecedor</option><option th:unless="${entry}" value="VENDA">Venda ao cliente</option><option th:unless="${entry}" value="PERDA">Perda (avaria, validade, furto)</option></select></div></div>
<div class="field" data-dialog-mode="COMPRA"><label for="m-custo">Custo unitário da compra (R$) *</label><input id="m-custo" name="custo" type="number" min="0.01" step="0.01" inputmode="decimal"><p class="hint">Preenchido com o último custo. Altere se o fornecedor mudou o preço.</p></div>
<div class="field" data-dialog-mode="VENDA"><label for="m-preco">Preço unitário de venda (R$)</label><input id="m-preco" name="preco" type="number" step="0.01" readonly><p class="hint">Preço do cadastro. Para mudar, peça ao administrador.</p></div>
<div class="field" data-dialog-mode="PERDA"><label for="m-custo-ref">Custo de referência (R$)</label><input id="m-custo-ref" type="number" step="0.01" readonly><p class="hint">A perda é valorizada pelo custo do último lote comprado.</p></div>
<div class="field"><label for="m-justificativa" id="m-justificativa-label">Observação (opcional)</label><textarea id="m-justificativa" name="justificativa" maxlength="300" rows="3" placeholder=""></textarea></div>
</div>
<div class="dialog-foot"><div class="dialog-total"><span id="m-total-label">Total estimado</span><strong id="m-total">R$ 0,00</strong></div><div class="dialog-actions"><button type="button" class="button secondary" data-close-dialog>Cancelar</button><button class="button" th:text="${entry?'Revisar entrada':'Revisar saída'}">Revisar</button></div></div>
</form></dialog>
</th:block>
<form th:if="${product!=null}" th:action="@{/movimentos/revisar}" method="post" class="panel form-panel" th:object="${form}"><input type="hidden" th:field="*{chave}"><input type="hidden" th:field="*{produtoId}"><div th:replace="~{fragments :: fieldError('chave')}"></div><div th:replace="~{fragments :: fieldError('produtoId')}"></div><div class="product-summary"><div><span class="eyebrow" th:text="${product.sku}"></span><h2 th:text="${product.nome}"></h2><span th:text="${product.categoria}"></span></div><div><span class="muted">Saldo disponível</span><strong th:text="${product.saldo}+' UN'"></strong></div></div><div class="form-grid"><div class="field"><label for="quantidade">Quantidade (UN) *</label><input id="quantidade" type="number" th:field="*{quantidade}" min="1" step="1" required><div th:replace="~{fragments :: fieldError('quantidade')}"></div></div><div class="field"><label for="motivo">Motivo *</label><select id="motivo" th:field="*{motivo}" required><option value="COMPRA">Compra / entrada</option><option value="VENDA">Venda / saída</option><option value="PERDA">Perda / saída</option></select><div th:replace="~{fragments :: fieldError('motivo')}"></div></div><div data-mode="COMPRA"><div class="field"><label for="custo">Custo unitário da compra (R$) *</label><input id="custo" type="number" th:field="*{custo}" min="0.01" step="0.01"><div th:replace="~{fragments :: fieldError('custo')}"></div></div></div><div data-mode="VENDA"><div class="field"><label for="preco">Preço unitário de venda (R$) *</label><input id="preco" type="number" th:field="*{preco}" min="0.01" step="0.01" readonly><div th:replace="~{fragments :: fieldError('preco')}"></div></div><p class="hint">Preço atual do cadastro. Para alterar, solicite ao administrador.</p></div></div><div class="field"><label for="justificativa">Justificativa (obrigatória em perda)</label><textarea id="justificativa" th:field="*{justificativa}" maxlength="300" rows="3"></textarea><div th:replace="~{fragments :: fieldError('justificativa')}"></div></div><div class="form-actions"><button class="button">Revisar movimentação →</button><a class="button secondary" href="/produtos">Cancelar</a></div></form></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/movement-review.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Revisar movimentação')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">ETAPA 2 DE 2</p><h1>Confira antes de confirmar</h1><p class="muted">O saldo só será alterado após a confirmação abaixo.</p></div></div><section class="panel form-panel"><div class="product-summary"><div><span class="eyebrow" th:text="${product.sku}"></span><h2 th:text="${product.nome}"></h2></div><span class="badge" th:text="${command.motivo}"></span></div><dl class="review"><dt>Quantidade</dt><dd th:text="${command.quantidade}+' UN'"></dd><dt>Saldo atual</dt><dd th:text="${product.saldo}+' UN'"></dd><dt>Valor unitário</dt><dd th:text="${@view.money(command.motivo=='COMPRA'?command.custo:(command.motivo=='VENDA'?command.preco:product.custo))}"></dd><dt>Valor total</dt><dd th:text="${@view.money(total)}"></dd><dt>Justificativa</dt><dd th:text="${#strings.isEmpty(command.justificativa)?'Não informada':command.justificativa}"></dd></dl><p class="notice">Após confirmar, o registro será preservado no histórico. Saldo e preço serão conferidos novamente.</p><form th:action="@{/movimentos/confirmar}" method="post"><input type="hidden" name="chave" th:value="${command.chave}"><div class="form-actions"><button class="button">Confirmar movimentação</button><a class="button secondary" th:href="@{/movimentos/novo(produto=${product.id},motivo=${command.motivo})}">Voltar e corrigir</a></div></form></section></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/product-form.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Cadastro de produto')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">CATÁLOGO</p><h1><span th:text="${id==null?'Novo produto':'Editar produto'}"></span></h1><p class="muted">Campos com * são obrigatórios. Os produtos são controlados por unidade (UN).</p></div></div><form class="panel form-panel" th:action="${id==null?'/admin/produtos/novo':'/admin/produtos/'+id}" method="post" th:object="${form}"><div class="form-grid"><div class="field"><label for="sku">SKU *</label><input id="sku" type="text" th:field="*{sku}" required maxlength="40"><div th:replace="~{fragments :: fieldError('sku')}"></div></div><div class="field"><label for="nome">Nome do produto *</label><input id="nome" type="text" th:field="*{nome}" required maxlength="120"><div th:replace="~{fragments :: fieldError('nome')}"></div></div><div class="field"><label for="categoriaId">Categoria *</label><select id="categoriaId" th:field="*{categoriaId}" required><option value="">Selecione</option><option th:each="c:${categories}" th:value="${c.id}" th:text="${c.nome}"></option></select><div th:replace="~{fragments :: fieldError('categoriaId')}"></div></div><div class="field"><label for="minimo">Estoque mínimo (UN) *</label><input id="minimo" type="number" th:field="*{minimo}" min="0" step="1" required><div th:replace="~{fragments :: fieldError('minimo')}"></div></div><div class="field"><label for="custo">Custo de referência (R$) *</label><input id="custo" type="number" th:field="*{custo}" min="0.01" step="0.01" th:readonly="${id!=null}" th:required="${id==null}"><div th:replace="~{fragments :: fieldError('custo')}"></div></div><div class="field"><label for="preco">Preço de venda (R$) *</label><input id="preco" type="number" th:field="*{preco}" min="0.01" step="0.01" required><div th:replace="~{fragments :: fieldError('preco')}"></div></div></div><p class="hint">No cadastro, o saldo começa em zero. Depois, o custo de referência é atualizado somente por entradas.</p><div th:if="${id!=null}" class="field"><label class="checkbox"><input type="checkbox" th:field="*{ativo}">Produto ativo</label><div th:replace="~{fragments :: fieldError('ativo')}"></div></div><div class="form-actions"><button class="button">Salvar produto</button><a class="button secondary" href="/produtos">Cancelar</a></div></form></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/products.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Produtos')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">CATÁLOGO</p><h1>Produtos</h1><p class="muted">Consulte saldos, organize o catálogo e registre movimentações.</p></div><a sec:authorize="hasRole('ADMIN')" class="button" href="/admin/produtos/novo">＋ Novo produto</a></div>
<form class="filterbar" action="/produtos" method="get"><label class="grow">Buscar produto<input name="q" th:value="${q}" placeholder="Nome ou SKU" maxlength="100"></label><label>Categoria<select name="categoria"><option value="">Todas</option><option th:each="c:${categories}" th:value="${c.id}" th:text="${c.nome}" th:selected="${categoria!=null and categoria==c.id}"></option></select></label><label>Situação<select name="situacao"><option value="ativo" th:selected="${situacao=='ativo'}">Ativos</option><option value="inativo" th:selected="${situacao=='inativo'}">Inativos</option><option value="todos" th:selected="${situacao=='todos'}">Todos</option></select></label><label class="checkbox"><input type="checkbox" name="baixo" value="true" th:checked="${baixo}">Só baixo estoque</label><button class="button secondary">Filtrar</button><a href="/produtos">Limpar</a></form>
<section class="panel"><div class="panel-heading"><h2>Catálogo da loja</h2><span class="muted" th:text="${result.total}+' produtos'"></span></div><div class="table-scroll"><table><thead><tr><th>Produto</th><th>Categoria</th><th>Estoque</th><th>Custo ref.</th><th>Venda</th><th>Situação</th><th>Ações</th></tr></thead><tbody><tr th:each="p:${result.items}"><td><strong th:text="${p.nome}"></strong><small th:text="${p.sku}"></small></td><td th:text="${p.categoria}"></td><td><strong th:text="${p.saldo}+' UN'"></strong><small th:text="'Mínimo: '+${p.minimo}"></small><span class="badge warning" th:if="${p.ativo and p.saldo<=p.minimo}">Baixo estoque</span></td><td th:text="${@view.money(p.custo)}"></td><td th:text="${@view.money(p.preco)}"></td><td><span class="badge" th:text="${p.ativo?'Ativo':'Inativo'}"></span></td><td><div class="actions"><a th:if="${p.ativo}" th:href="@{/movimentos/novo(produto=${p.id},motivo='COMPRA')}">Entrada</a><a th:if="${p.ativo}" th:href="@{/movimentos/novo(produto=${p.id},motivo='VENDA')}">Saída</a><a sec:authorize="hasRole('ADMIN')" th:href="@{/admin/produtos/{id}(id=${p.id})}">Editar</a><a th:href="@{/historico(produto=${p.id})}">Histórico</a></div></td></tr><tr th:if="${result.total==0}"><td colspan="7" class="empty">Nenhum produto encontrado. Ajuste os filtros ou cadastre um novo item.</td></tr></tbody></table></div><div class="pagination"><a th:if="${result.page>0}" th:href="@{/produtos(q=${q},categoria=${categoria},situacao=${situacao},baixo=${baixo},pagina=${result.page-1})}">← Anterior</a><span th:text="'Página '+${result.page+1}+' de '+${result.pages()}"></span><a th:if="${result.page+1<result.pages()}" th:href="@{/produtos(q=${q},categoria=${categoria},situacao=${situacao},baixo=${baixo},pagina=${result.page+1})}">Próxima →</a></div></section></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/reports.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Relatórios')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div>

<div class="page-heading"><div><p class="eyebrow">ANÁLISE</p><h1>Relatórios</h1><p class="muted">Entradas, vendas e perdas de <strong th:text="${rotulo}"></strong>.</p></div><a class="button secondary" th:href="@{/relatorios/exportar(periodo=${periodo},inicio=${inicio},fim=${fim})}">Exportar CSV</a></div>

<div class="filterbar report-filter"><nav class="segmented" aria-label="Período do relatório"><a th:each="op:${ {'3','6','12','tudo'} }" th:href="@{/relatorios(periodo=${op})}" th:text="${op=='tudo'?'Todo o período':op+' meses'}" th:classappend="${periodo==op?'active':''}" th:attr="aria-current=${periodo==op?'page':null}"></a></nav>
<form class="custom-range" action="/relatorios" method="get"><input type="hidden" name="periodo" value="personalizado"><label>De <input type="date" name="inicio" th:value="${inicio}" required></label><label>Até <input type="date" name="fim" th:value="${fim}" required></label><button class="button secondary" th:classappend="${periodo=='personalizado'?'active':''}">Aplicar datas</button></form></div>

<section class="stats" aria-label="Totais do período">
<article class="stat"><span>Entradas (compras)</span><strong th:text="${@view.money(t.entradasValor)}"></strong><small th:text="${@view.number(t.entradasQtd)}+' UN em '+${t.nEntradas}+' registros'"></small><small class="delta" th:if="${cmpEntradas!=null}" th:text="${cmpEntradas.semBase?'Sem entradas no período anterior':(cmpEntradas.up?'▲ ':'▼ ')+@view.decimal(cmpEntradas.percent)+'% em relação a '+cmpRotulo}"></small></article>
<article class="stat featured"><span>Vendas</span><strong th:text="${@view.money(t.vendasValor)}"></strong><small th:text="${@view.number(t.vendasQtd)}+' UN em '+${t.nVendas}+' registros'"></small><small class="delta" th:if="${cmpVendas!=null}" th:classappend="${cmpVendas.semBase?'':(cmpVendas.up?'good':'bad')}" th:text="${cmpVendas.semBase?'Sem vendas no período anterior':(cmpVendas.up?'▲ ':'▼ ')+@view.decimal(cmpVendas.percent)+'% em relação a '+cmpRotulo}"></small></article>
<article class="stat"><span>Perdas</span><strong class="loss" th:text="${@view.money(t.perdasValor)}"></strong><small th:text="${@view.number(t.perdasQtd)}+' UN em '+${t.nPerdas}+' registros'"></small><small class="delta" th:if="${cmpPerdas!=null}" th:classappend="${cmpPerdas.semBase?'':(cmpPerdas.up?'bad':'good')}" th:text="${cmpPerdas.semBase?'Sem perdas no período anterior':(cmpPerdas.up?'▲ ':'▼ ')+@view.decimal(cmpPerdas.percent)+'% em relação a '+cmpRotulo}"></small></article>
</section>

<section class="kpis" aria-label="Indicadores do período">
<div><span>Ticket médio por venda</span><strong th:text="${ticketMedio==null?'—':@view.money(ticketMedio)}"></strong><small>Valor vendido ÷ número de vendas</small></div>
<div><span>Vendas por mês, em média</span><strong th:text="${@view.money(mediaMensalVendas)}"></strong><small th:text="'Em '+${months.size()}+(${months.size()==1}?' mês':' meses')"></small></div>
<div><span>Compras por mês, em média</span><strong th:text="${@view.money(mediaMensalEntradas)}"></strong><small>Custo das entradas</small></div>
<div><span>Taxa de perda</span><strong th:text="${taxaPerda==null?'—':@view.decimal(taxaPerda)+'%'}"></strong><small>Unidades perdidas ÷ unidades que saíram</small></div>
<div><span>Saldo de unidades no período</span><strong th:text="${(saldoLiquido>0?'+':'')+@view.number(saldoLiquido)+' UN'}"></strong><small>Entradas menos vendas e perdas</small></div>
<div><span>Melhor mês de vendas</span><strong class="text-value" th:text="${melhorMes==null?'—':melhorMes}"></strong><small th:text="${melhorMes==null?'Sem vendas no período':@view.money(melhorMesValor)}"></small></div>
<div><span>Valor atual do estoque</span><strong th:text="${@view.money(estoqueAtual)}"></strong><small>Saldo × último custo, hoje</small></div>
<div><span>Produtos movimentados</span><strong th:text="${t.produtosMovimentados}"></strong><small th:text="${@view.number(t.movimentacoes)}+' movimentações no total'"></small></div>
</section>

<section class="panel" th:with="c=${valueChart}"><div class="panel-heading"><div><h2>Valores por mês</h2><p class="muted">Compras pelo custo, vendas pelo preço praticado e perdas pelo custo de referência.</p></div><ul class="legend"><li class="entrada">Entradas</li><li class="venda">Vendas</li><li class="perda">Perdas</li></ul></div>
<div class="chart-wrap"><svg class="chart" th:attr="viewBox=|0 0 ${c.width} ${c.height}|" preserveAspectRatio="xMinYMin meet" role="img" aria-label="Gráfico de barras com entradas, vendas e perdas em reais por mês. Os valores exatos estão na tabela Resumo mensal."><g class="grid"><g th:each="g:${c.grid}"><line th:attr="x1=${c.left},x2=${c.width - c.right},y1=${g.y},y2=${g.y}"></line><text th:attr="x=${c.left - 8},y=${g.y + 4}" text-anchor="end" th:text="${g.label}"></text></g></g><rect th:each="b:${c.bars}" th:class="'bar '+${b.kind}" th:attr="x=${b.x},y=${b.y},width=${b.w},height=${b.h}" rx="2"><title th:text="${b.tip}"></title></rect><text th:each="l:${c.labels}" class="axis-month" th:attr="x=${l.x},y=${c.base + 22}" text-anchor="middle" th:text="${l.text}"></text><text th:if="${c.empty}" class="chart-empty" th:attr="x=${c.width / 2},y=${c.base / 2}" text-anchor="middle">Nenhuma movimentação neste período</text></svg></div></section>

<section class="panel" th:with="c=${unitChart}"><div class="panel-heading"><div><h2>Unidades por mês</h2><p class="muted">Quantas unidades entraram e saíram do estoque em cada mês.</p></div><ul class="legend"><li class="entrada">Entradas</li><li class="venda">Vendas</li><li class="perda">Perdas</li></ul></div>
<div class="chart-wrap"><svg class="chart" th:attr="viewBox=|0 0 ${c.width} ${c.height}|" preserveAspectRatio="xMinYMin meet" role="img" aria-label="Gráfico de barras com unidades de entradas, vendas e perdas por mês. Os valores exatos estão na tabela Resumo mensal."><g class="grid"><g th:each="g:${c.grid}"><line th:attr="x1=${c.left},x2=${c.width - c.right},y1=${g.y},y2=${g.y}"></line><text th:attr="x=${c.left - 8},y=${g.y + 4}" text-anchor="end" th:text="${g.label}"></text></g></g><rect th:each="b:${c.bars}" th:class="'bar '+${b.kind}" th:attr="x=${b.x},y=${b.y},width=${b.w},height=${b.h}" rx="2"><title th:text="${b.tip}"></title></rect><text th:each="l:${c.labels}" class="axis-month" th:attr="x=${l.x},y=${c.base + 22}" text-anchor="middle" th:text="${l.text}"></text><text th:if="${c.empty}" class="chart-empty" th:attr="x=${c.width / 2},y=${c.base / 2}" text-anchor="middle">Nenhuma movimentação neste período</text></svg></div></section>

<div class="two-columns">
<section class="panel"><div class="panel-heading"><div><h2>Mais vendidos</h2><p class="muted">Os 10 produtos com mais unidades vendidas.</p></div></div><div class="table-scroll"><table><thead><tr><th>Produto</th><th class="num">Unidades</th><th class="num">Valor</th></tr></thead><tbody><tr th:each="r,st:${topVendas}"><td><span class="rank" th:text="${st.count}"></span><strong th:text="${r.nome}"></strong><small th:text="${r.sku}"></small></td><td class="num" th:text="${@view.number(r.quantidade)}"></td><td class="num" th:text="${@view.money(r.valor)}"></td></tr><tr th:if="${#lists.isEmpty(topVendas)}"><td colspan="3" class="empty">Nenhuma venda neste período.</td></tr></tbody></table></div></section>
<div>
<section class="panel"><div class="panel-heading"><div><h2>Maiores perdas</h2><p class="muted">Onde o estoque está escapando.</p></div></div><div class="table-scroll"><table><thead><tr><th>Produto</th><th class="num">Unidades</th><th class="num">Custo</th></tr></thead><tbody><tr th:each="r:${topPerdas}"><td><strong th:text="${r.nome}"></strong><small th:text="${r.sku}"></small></td><td class="num" th:text="${@view.number(r.quantidade)}"></td><td class="num" th:text="${@view.money(r.valor)}"></td></tr><tr th:if="${#lists.isEmpty(topPerdas)}"><td colspan="3" class="empty">Nenhuma perda neste período.</td></tr></tbody></table></div></section>
<section class="panel"><div class="panel-heading"><div><h2>Mais comprados</h2><p class="muted">Produtos com mais unidades recebidas.</p></div></div><div class="table-scroll"><table><thead><tr><th>Produto</th><th class="num">Unidades</th><th class="num">Custo</th></tr></thead><tbody><tr th:each="r:${topEntradas}"><td><strong th:text="${r.nome}"></strong><small th:text="${r.sku}"></small></td><td class="num" th:text="${@view.number(r.quantidade)}"></td><td class="num" th:text="${@view.money(r.valor)}"></td></tr><tr th:if="${#lists.isEmpty(topEntradas)}"><td colspan="3" class="empty">Nenhuma compra neste período.</td></tr></tbody></table></div></section>
</div>
</div>

<div class="two-columns">
<section class="panel"><div class="panel-heading"><div><h2>Por categoria</h2><p class="muted">Valores do período agrupados pela categoria do produto.</p></div></div><div class="table-scroll"><table><thead><tr><th>Categoria</th><th class="num">Entradas</th><th class="num">Vendas</th><th class="num">Perdas</th></tr></thead><tbody><tr th:each="c:${categorias}"><td><strong th:text="${c.categoria}"></strong></td><td class="num"><span th:text="${@view.money(c.entradasValor)}"></span><small th:text="${@view.number(c.entradasQtd)}+' UN'"></small></td><td class="num"><span th:text="${@view.money(c.vendasValor)}"></span><small th:text="${@view.number(c.vendasQtd)}+' UN'"></small></td><td class="num"><span th:text="${@view.money(c.perdasValor)}"></span><small th:text="${@view.number(c.perdasQtd)}+' UN'"></small></td></tr><tr th:if="${#lists.isEmpty(categorias)}"><td colspan="4" class="empty">Nenhuma movimentação neste período.</td></tr></tbody></table></div></section>
<section class="panel"><div class="panel-heading"><div><h2>Por responsável</h2><p class="muted">Quem registrou as movimentações do período.</p></div></div><div class="table-scroll"><table><thead><tr><th>Usuário</th><th class="num">Entradas</th><th class="num">Vendas</th><th class="num">Perdas</th><th class="num">Total</th></tr></thead><tbody><tr th:each="u:${usuarios}"><td><strong th:text="${u.usuario}"></strong></td><td class="num" th:text="${u.entradas}"></td><td class="num" th:text="${u.vendas}"></td><td class="num" th:text="${u.perdas}"></td><td class="num"><strong th:text="${u.total}"></strong></td></tr><tr th:if="${#lists.isEmpty(usuarios)}"><td colspan="5" class="empty">Nenhuma movimentação neste período.</td></tr></tbody></table></div></section>
</div>

<section class="panel"><div class="panel-heading"><div><h2>Resumo mensal</h2><p class="muted">Do mês mais recente para o mais antigo. Use Exportar CSV para abrir no Excel.</p></div></div><div class="table-scroll"><table class="monthly"><thead><tr><th>Mês</th><th class="num">Entradas (UN)</th><th class="num">Entradas (R$)</th><th class="num">Vendas (UN)</th><th class="num">Vendas (R$)</th><th class="num">Perdas (UN)</th><th class="num">Perdas (R$)</th><th class="num">Saldo (UN)</th></tr></thead><tbody><tr th:each="x:${monthsDesc}" th:classappend="${x.movimentacoes==0?'quiet':''}"><td><strong th:text="${@view.month(x.mes)}"></strong></td><td class="num" th:text="${@view.number(x.entradasQtd)}"></td><td class="num" th:text="${@view.money(x.entradasValor)}"></td><td class="num" th:text="${@view.number(x.vendasQtd)}"></td><td class="num" th:text="${@view.money(x.vendasValor)}"></td><td class="num" th:text="${@view.number(x.perdasQtd)}"></td><td class="num" th:text="${@view.money(x.perdasValor)}"></td><td class="num" th:text="${(x.saldoLiquido>0?'+':'')+@view.number(x.saldoLiquido)}"></td></tr></tbody><tfoot><tr><th>Total</th><th class="num" th:text="${@view.number(t.entradasQtd)}"></th><th class="num" th:text="${@view.money(t.entradasValor)}"></th><th class="num" th:text="${@view.number(t.vendasQtd)}"></th><th class="num" th:text="${@view.money(t.vendasValor)}"></th><th class="num" th:text="${@view.number(t.perdasQtd)}"></th><th class="num" th:text="${@view.money(t.perdasValor)}"></th><th class="num" th:text="${(saldoLiquido>0?'+':'')+@view.number(saldoLiquido)}"></th></tr></tfoot></table></div></section>

<p class="footnote">Vendas mostram o valor registrado no momento da saída e não comprovam recebimento. O sistema não calcula lucro contábil. O valor do estoque usa o último custo de compra de cada produto.</p>
</main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/user-form.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Conta de usuário')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">ADMINISTRAÇÃO</p><h1><span th:text="${id==null?'Novo usuário':'Editar acesso'}"></span></h1><p class="muted">Mantenha ao menos um administrador ativo.</p></div></div><form class="panel form-panel" th:action="${id==null?'/admin/usuarios/novo':'/admin/usuarios/'+id}" method="post" th:object="${form}"><div class="form-grid"><div class="field"><label for="nome">Nome *</label><input id="nome" type="text" th:field="*{nome}" required maxlength="100"><div th:replace="~{fragments :: fieldError('nome')}"></div></div><div class="field"><label for="login">Login *</label><input id="login" type="text" th:field="*{login}" required maxlength="60" th:readonly="${id!=null}" autocomplete="off"><div th:replace="~{fragments :: fieldError('login')}"></div></div><div class="field"><label for="senha">Senha (12 caracteres ou mais)</label><input id="senha" type="password" th:field="*{senha}" minlength="12" autocomplete="new-password" th:required="${id==null}"><div th:replace="~{fragments :: fieldError('senha')}"></div></div><div class="field"><label for="perfil">Perfil *</label><select id="perfil" th:field="*{perfil}"><option value="OPERADOR">Operador</option><option value="ADMIN">Administrador</option></select><div th:replace="~{fragments :: fieldError('perfil')}"></div></div></div><p class="hint" th:if="${id!=null}">Deixe a senha vazia para manter a atual. Alterações nesta conta encerram suas sessões existentes.</p><label class="checkbox"><input type="checkbox" th:field="*{ativo}">Conta ativa</label><div class="form-actions"><button class="button">Salvar conta</button><a class="button secondary" href="/admin/usuarios">Cancelar</a></div></form></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/main/resources/templates/users.html

```html
<!doctype html><html lang="pt-BR" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security"><head th:replace="~{fragments :: head('Usuários')}"></head><body><a href="#conteudo" class="skip">Ir ao conteúdo</a><aside th:replace="~{fragments :: nav}"></aside><div class="workspace"><header th:replace="~{fragments :: top}"></header><main id="conteudo"><div th:replace="~{fragments :: alerts}"></div><div class="page-heading"><div><p class="eyebrow">ADMINISTRAÇÃO</p><h1>Usuários e acesso</h1><p class="muted">Controle quem consulta e movimenta o estoque da loja.</p></div><a class="button" href="/admin/usuarios/novo">＋ Novo usuário</a></div><section class="panel"><div class="table-scroll"><table><thead><tr><th>Nome</th><th>Login</th><th>Perfil</th><th>Situação</th><th>Ação</th></tr></thead><tbody><tr th:each="a:${accounts}"><td th:text="${a.nome}"></td><td th:text="${a.login}"></td><td th:text="${a.perfil=='ADMIN'?'Administrador':'Operador'}"></td><td><span class="badge" th:text="${a.ativo?'Ativo':'Inativo'}"></span></td><td><a th:href="@{/admin/usuarios/{id}(id=${a.id})}">Editar acesso</a></td></tr></tbody></table></div></section><p class="hint">O operador consulta produtos e registra movimentações. Somente administradores alteram cadastros e contas.</p></main><footer th:replace="~{fragments :: footer}"></footer></div></body></html>
```

## src/test/java/br/com/estoquebairro/QualityServer.java

```java
package br.com.estoquebairro;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.util.*;
import org.springframework.boot.SpringApplication;

/** Ambiente isolado de avaliação. Nunca aponta ao banco da loja. */
public class QualityServer {
  public static void main(String[] args) throws Exception {
    String password = System.getenv("QUALITY_PASSWORD");
    if (password == null || password.length() < 12)
      throw new IllegalArgumentException("Defina QUALITY_PASSWORD com pelo menos 12 caracteres.");
    var pg =
        EmbeddedPostgres.builder()
            .setServerConfig("unix_socket_directories", "")
            .setPort(55432)
            .start();
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    pg.close();
                  } catch (Exception ignored) {
                  }
                }));
    var app = new SpringApplication(EstoqueBairroApplication.class);
    app.setDefaultProperties(Map.of("server.address", "127.0.0.1"));
    var context =
        app.run(
            "--spring.datasource.url=" + pg.getJdbcUrl("postgres", "postgres"),
            "--spring.datasource.username=postgres",
            "--spring.datasource.password=",
            "--app.bootstrap.password=" + password,
            "--app.bootstrap.login=admin",
            "--app.demo.enabled=true",
            "--app.demo.password=" + password);
    var db = context.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
    db.execute("CREATE TABLE quality_environment(marcador TEXT NOT NULL)");
    db.update("INSERT INTO quality_environment VALUES (?)", "estoque-bairro-isolado");
  }
}
```

## src/test/java/br/com/estoquebairro/SystemTest.java

```java
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
```
