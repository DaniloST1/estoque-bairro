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
