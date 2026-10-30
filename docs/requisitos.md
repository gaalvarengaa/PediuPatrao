# Requisitos do Dashboard Administrativo

## Objetivo

O Dashboard Administrativo permite que o responsável pelo estabelecimento acompanhe o desempenho das vendas, visualize informações sobre os pedidos e identifique tendências da operação, auxiliando na tomada de decisões.

O painel faz parte do sistema Pediu Patrão e consome dados de uma API própria, integrada ao banco MongoDB já usado pelo sistema.

## Escopo

**Incluído:**

- Consulta de indicadores de vendas, gráficos, situação dos pedidos e produtos mais vendidos.
- Filtros de consulta aplicados a todas as informações do painel.
- Endpoints da API que fornecem esses dados.

**Fora do escopo:**

- Cadastro ou alteração de pedidos pelo painel. O dashboard é somente leitura.
- Exportação de relatórios.
- Comparação entre estabelecimentos. O sistema atende uma única pizzaria.

## Perfis de acesso

| Perfil    | Acesso ao dashboard | Justificativa                                           |
|-----------|---------------------|---------------------------------------------------------|
| ADMIN     | Permitido           | Administra o sistema e já consulta pedidos e auditoria. |
| GERENTE   | Permitido           | É o responsável pelo acompanhamento da operação.        |
| ATENDENTE | Negado (403)        | Sua função é registrar pedidos, não analisar vendas.    |
