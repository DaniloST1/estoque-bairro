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
