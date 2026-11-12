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
