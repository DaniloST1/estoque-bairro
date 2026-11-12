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
