# Dashboard Administrativo

## Requisitos funcionais

### RF01 — Indicadores gerais de vendas

O dashboard deve apresentar, para o período consultado:

| Indicador          | Cálculo                                                 |
|--------------------|---------------------------------------------------------|
| Faturamento        | Soma do valor total dos pedidos não cancelados.         |
| Pedidos            | Quantidade de pedidos não cancelados.                   |
| Ticket médio       | Faturamento dividido pela quantidade de pedidos não cancelados. Sem pedidos, o valor é zero. |
| Cancelamentos      | Quantidade de pedidos cancelados.                       |
| Descontos          | Soma dos descontos concedidos nos pedidos não cancelados. |
