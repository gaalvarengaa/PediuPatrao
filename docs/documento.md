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

### RF03 — Situação dos pedidos

O dashboard deve apresentar a quantidade de pedidos em cada situação do fluxo, dentro do período consultado:

| Situação          | Significado                                  |
|-------------------|----------------------------------------------|
| Recebido          | Pedido registrado, aguardando o preparo.     |
| Em preparação     | Pedido sendo preparado.                      |
| Pronto            | Pedido pronto, aguardando a saída.           |
| Saiu para entrega | Pedido a caminho do cliente.                 |
| Retirado          | Pedido retirado pelo cliente.                |
| Finalizado        | Pedido concluído.                            |
| Cancelado         | Pedido cancelado pelo gerente.               |

Situações sem pedidos devem aparecer com quantidade zero.

### RF04 — Produtos mais vendidos

O dashboard deve apresentar o ranking dos produtos mais vendidos no período consultado, com nome, quantidade vendida e faturamento de cada produto. O ranking é ordenado pela maior quantidade e, em caso de empate, pelo maior faturamento. O usuário pode definir quantos produtos aparecem, de 1 a 50, sendo 10 o padrão.

### RF05 — Filtros de consulta

O usuário deve poder filtrar todas as informações do dashboard por:

| Filtro    | Comportamento                                                  |
|-----------|----------------------------------------------------------------|
| Período   | Data inicial e data final, ambas incluídas. Sem preenchimento, considera os últimos 30 dias. |
| Status    | Apenas pedidos na situação escolhida.                          |
| Produto   | Apenas pedidos que contenham o produto escolhido.              |
| Categoria | Apenas pedidos que contenham produtos da categoria escolhida.  |

Os filtros podem ser combinados. A data inicial não pode ser posterior à data final.

### RF06 — Integração com a API

O dashboard deve obter todos os dados pelos endpoints da API, descritos neste documento, sem receber valores calculados diretamente no HTML da página. A API deve consultar o banco MongoDB do sistema e responder em JSON.

### RF07 — Atualização e tratamento de dados

**Atualização:**

- Os dados são atualizados automaticamente a cada 60 segundos e também por um botão.
- A atualização mantém os filtros escolhidos.
- A tela informa o horário da última atualização.

**Tratamento na tela:**

- Enquanto carrega, a tela mostra um indicador de carregamento.
- Sem dados no período, a tela mostra uma mensagem em vez de gráficos vazios.
- Em caso de erro, a tela mostra a mensagem recebida e mantém os últimos dados carregados.
- Com a sessão expirada, o usuário vai para o login. Sem permissão, vai para a página de acesso negado.

**Tratamento na API:**

- Filtros inválidos são recusados com uma mensagem de erro.
- Períodos sem pedidos retornam valores zerados ou listas vazias, não erro.
- Pedidos antigos são tratados conforme as regras de negócio abaixo.

## Regras de negócio

| Código | Regra |
|--------|-------|
| RN01   | Pedidos cancelados não entram no faturamento, na quantidade de pedidos, no ticket médio, nos descontos, nos gráficos de vendas nem no ranking. Eles aparecem apenas no indicador de cancelamentos e na situação dos pedidos. |
| RN02   | O período é aplicado sobre a data e hora de entrada do pedido, no horário local do estabelecimento. |
| RN03   | A semana começa na segunda-feira. |
| RN04   | Os valores monetários têm duas casas decimais. O ticket médio é arredondado para cima a partir da metade do centavo. |
| RN05   | O faturamento de um pedido é o valor total, já com desconto. No ranking, o faturamento de um produto é a soma dos subtotais dos seus itens, sem o desconto, que é aplicado sobre o pedido inteiro. |
| RN06   | Com o filtro de produto ou de categoria, os indicadores e gráficos consideram o pedido inteiro, e o ranking considera apenas os itens que atendem ao filtro. |
| RN07   | A categoria do produto é copiada para o item no registro do pedido, preservando o histórico. Em pedidos antigos, é obtida pelo cadastro do produto. Itens cujo produto não existe mais não atendem ao filtro de categoria. |
| RN08   | Status de versões antigas são convertidos: `EM_PREPARO` para Em preparação e `ENTREGUE` para Finalizado. Valores ausentes em pedidos antigos são considerados zero. |

## Requisitos não funcionais

| Código | Categoria        | Requisito |
|--------|------------------|-----------|
| RNF01  | Segurança        | Somente os perfis ADMIN e GERENTE acessam o dashboard. A regra é aplicada no backend: o perfil ATENDENTE recebe 403, e requisições sem login recebem 401. |
| RNF02  | Privacidade      | Os endpoints do dashboard não retornam dados pessoais de clientes. |
| RNF03  | Desempenho       | Cada endpoint responde em até 2 segundos com 5.000 pedidos no período consultado, com índice na data de entrada do pedido. |
| RNF04  | Confiabilidade   | Todos os valores são calculados no servidor, com tipo decimal, a partir dos dados gravados. |
| RNF05  | Usabilidade      | A tela funciona em computador e celular e exibe valores em reais e datas no formato brasileiro. |
| RNF06  | Compatibilidade  | A solução usa a mesma base do sistema: Java 21, Spring Boot 3.4, Spring Security, Thymeleaf e MongoDB. Os gráficos usam Chart.js. |
| RNF07  | Manutenibilidade | O código segue a separação em controller, service e repository e tem testes automatizados de cálculos, filtros, permissões e erros. |

## Documentação dos endpoints

Os endpoints exigem login, por sessão do navegador ou HTTP Basic, e aceitam apenas `GET`.

### Parâmetros comuns

Os parâmetros são opcionais e valem para todos os endpoints.

| Parâmetro   | Tipo                  | Descrição                                                                       |
|-------------|-----------------------|---------------------------------------------------------------------------------|
| `de`        | Data (`AAAA-MM-DD`)   | Data inicial. Padrão: 29 dias antes da data final.                              |
| `ate`       | Data (`AAAA-MM-DD`)   | Data final. Padrão: hoje.                                                       |
| `status`    | Texto                 | `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO` ou `CANCELADO`. |
| `produtoId` | Texto                 | ID do produto.                                                                  |
| `tipo`      | Texto                 | Categoria do produto, como `Pizza` ou `Bebida`.                                 |

### Possíveis erros

Com exceção do 401, os erros retornam JSON no formato `{"erro": "mensagem"}`.

| Status | Quando ocorre                      | Exemplo de mensagem                                                |
|--------|------------------------------------|--------------------------------------------------------------------|
| 400    | Parâmetro em formato inválido      | `Valor inválido para o parâmetro 'status'.`                        |
| 401    | Requisição sem login               | Resposta sem corpo                                                 |
| 403    | Perfil sem permissão               | `Acesso negado: seu perfil não tem permissão para esta operação.`  |
| 422    | Valores que violam as regras       | `A data inicial não pode ser posterior à data final.`              |

### GET `/api/dashboard/indicadores`

Retorna os indicadores gerais de vendas (RF01).

```bash
curl -u gerente:teste123 "http://localhost:8080/api/dashboard/indicadores?de=2026-10-01&ate=2026-10-31"
```

Resposta `200`:

```json
{
  "faturamento": 1250.50,
  "pedidos": 30,
  "ticketMedio": 41.68,
  "cancelamentos": 2,
  "descontos": 35.00
}
```

### GET `/api/dashboard/vendas`

Retorna os dados dos gráficos de vendas (RF02).

Parâmetro adicional:

| Parâmetro     | Tipo  | Descrição                                     |
|---------------|-------|-----------------------------------------------|
| `agrupamento` | Texto | `DIA`, `SEMANA` ou `MES`. Padrão: `DIA`.      |

O campo `periodo` da resposta traz a data no agrupamento por dia, a data da segunda-feira no agrupamento por semana e `AAAA-MM` no agrupamento por mês.

```bash
curl -u gerente:teste123 "http://localhost:8080/api/dashboard/vendas?de=2026-10-01&ate=2026-10-02&agrupamento=DIA"
```

Resposta `200`:

```json
{
  "agrupamento": "DIA",
  "pontos": [
    { "periodo": "2026-10-01", "faturamento": 412.00, "pedidos": 9 },
    { "periodo": "2026-10-02", "faturamento": 0.00, "pedidos": 0 }
  ]
}
```

### GET `/api/dashboard/status`

Retorna a quantidade de pedidos em cada situação (RF03). As sete situações sempre aparecem; o exemplo abaixo está resumido.

```bash
curl -u gerente:teste123 "http://localhost:8080/api/dashboard/status?de=2026-10-01&ate=2026-10-31"
```

Resposta `200`:

```json
[
  { "status": "RECEBIDO", "descricao": "Recebido", "quantidade": 3 },
  { "status": "EM_PREPARACAO", "descricao": "Em preparação", "quantidade": 0 },
  { "status": "CANCELADO", "descricao": "Cancelado", "quantidade": 2 }
]
```

### GET `/api/dashboard/produtos-mais-vendidos`

Retorna o ranking de produtos (RF04).

Parâmetro adicional:

| Parâmetro | Tipo   | Descrição                                                                 |
|-----------|--------|---------------------------------------------------------------------------|
| `limite`  | Número | Quantidade de produtos, de 1 a 50. Padrão: 10. Fora dessa faixa, retorna 422. |

```bash
curl -u gerente:teste123 "http://localhost:8080/api/dashboard/produtos-mais-vendidos?de=2026-10-01&ate=2026-10-31&limite=2"
```

Resposta `200`:

```json
[
  { "produtoId": "6721f0a1c3", "nome": "Pizza Calabresa", "quantidade": 18, "faturamento": 810.00 },
  { "produtoId": "6721f0a1c4", "nome": "Refrigerante 2L", "quantidade": 12, "faturamento": 144.00 }
]
```
