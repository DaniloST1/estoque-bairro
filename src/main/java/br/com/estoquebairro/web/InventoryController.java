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
