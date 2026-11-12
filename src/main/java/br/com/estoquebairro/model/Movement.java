package br.com.estoquebairro.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Movement(
    long id,
    long produtoId,
    String produto,
    String sku,
    String usuario,
    String tipo,
    String motivo,
    int quantidade,
    BigDecimal custo,
    BigDecimal preco,
    OffsetDateTime dataHora,
    String justificativa,
    String assinatura,
    int saldoApos) {}
