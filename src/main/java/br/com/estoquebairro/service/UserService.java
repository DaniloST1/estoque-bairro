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
