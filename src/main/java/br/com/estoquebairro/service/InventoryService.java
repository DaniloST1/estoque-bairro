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
