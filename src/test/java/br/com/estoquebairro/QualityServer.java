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
