package br.com.estoquebairro.model;

import java.math.BigDecimal;

public record Product(
    long id,
    String sku,
    String nome,
    long categoriaId,
    String categoria,
    BigDecimal custo,
    BigDecimal preco,
    int minimo,
    int saldo,
    boolean ativo) {}
