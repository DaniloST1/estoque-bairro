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
