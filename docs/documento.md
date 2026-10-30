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

### RF02 — Análise de vendas

O dashboard deve apresentar gráficos que mostrem a evolução das vendas no período consultado:

| Gráfico             | Tipo   | Dados                                            |
|---------------------|--------|--------------------------------------------------|
| Faturamento         | Linha  | Faturamento de cada intervalo de tempo.          |
| Quantidade de pedidos | Barras | Pedidos não cancelados de cada intervalo de tempo. |

O usuário deve poder agrupar os dados por dia, semana ou mês. Intervalos sem vendas devem aparecer com valor zero, para não esconder quedas na operação.
