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
