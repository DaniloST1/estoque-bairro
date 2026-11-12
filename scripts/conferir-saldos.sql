-- O resultado deve ser vazio. Consulta somente leitura.
SELECT p.id,p.sku,p.saldo,
 coalesce(sum(CASE WHEN m.tipo='ENTRADA' THEN m.quantidade ELSE -m.quantidade END),0) AS saldo_historico
FROM produto p LEFT JOIN movimentacao m ON m.produto_id=p.id
GROUP BY p.id
HAVING p.saldo <> coalesce(sum(CASE WHEN m.tipo='ENTRADA' THEN m.quantidade ELSE -m.quantidade END),0)
 OR p.saldo<0;
