# PediuPatrão

Sistema de gestão de pedidos para pizzaria, com controle de acesso por perfil, fluxo obrigatório de pedidos e auditoria persistente.

Tecnologias: Java 21, Spring Boot 3.4, Spring Security, Thymeleaf e MongoDB.

## Como executar

Pré-requisitos:

- Java 21
- MongoDB em execução (por padrão em `mongodb://localhost:27017`)

Para usar outro endereço do MongoDB, defina a variável `MONGODB_URI`. O banco usado é `pizzaria_gabriel`.

```bash
sh ./mvnw spring-boot:run
```

No Windows (PowerShell ou CMD):

```bash
mvnw.cmd spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

## Dados iniciais

Na primeira execução são criados os usuários abaixo, todos com a senha `teste123`:

| Usuário     | Perfil    |
|-------------|-----------|
| `admin`     | ADMIN     |
| `gerente`   | GERENTE   |
| `atendente` | ATENDENTE |

Troque essas senhas antes de usar o sistema em produção.

Quando não há nenhum produto cadastrado, também é criado um cardápio básico para que os pedidos possam ser registrados de imediato: Pizza Calabresa (R$ 45,00), Pizza Margherita (R$ 42,00), Pizza Quatro Queijos (R$ 52,00), Refrigerante 2L (R$ 12,00) e Borda Recheada (R$ 8,00).

## Perfis e permissões

| Perfil    | Pode                                                                                                  |
|-----------|-------------------------------------------------------------------------------------------------------|
| ADMIN     | Gerenciar usuários e perfis, consultar pedidos e consultar a auditoria.                              |
| GERENTE   | Gerenciar pedidos, aplicar descontos, cancelar pedidos, consultar clientes e consultar a auditoria.   |
| ATENDENTE | Cadastrar e atualizar clientes, registrar pedidos e avançar o status deles.                           |

As regras são aplicadas no backend (`SecurityConfig` e `@PreAuthorize`). Esconder botões na tela não é suficiente, e requisições não autorizadas recebem 403.

## Requisitos implementados

**RF01 — Usuários e permissões.** Três perfis, gestão de usuários restrita ao ADMIN, proteção contra autoelevação (ninguém altera o próprio perfil, renomeia a própria conta ou exclui o próprio usuário) e proteção das APIs por perfil.

**RF02 — Cadastro de pedidos.** Pedido com itens, quantidades, preços, descontos, valor total, responsável pelo registro e data/hora de entrada. Preços vêm do cadastro de produtos e o total é calculado no servidor. Os campos de valor enviados na requisição são ignorados.

**RF03 — Entrada, saída e transições.** Fluxo `RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA / RETIRADO → FINALIZADO`. Saída e finalização gravam data/hora e responsável. Transições inválidas são bloqueadas, e nenhum status muda depois do cancelamento.

**RF04 — Cancelamentos e descontos (somente GERENTE).**
- Cancelamento exige justificativa de pelo menos 5 caracteres e só é permitido antes da saída.
- Desconto percentual maior que 0% e de no máximo 20%, só antes da saída. Um novo desconto substitui o anterior.
- Ambas as operações são auditadas com usuário, data/hora e valores anteriores e novos.

**RF05 — Auditoria.** Registros persistentes no MongoDB (coleção `auditoria`) para inclusão, status, desconto e cancelamento de pedidos, e inclusão, alteração e exclusão de clientes. Cada registro guarda usuário, perfil, data/hora, operação, entidade e ID, campos alterados com valores anteriores e novos, e justificativa quando houver.
- Os registros não têm endpoints de alteração ou exclusão.
- Telefones são mascarados na consulta, e senhas, tokens e credenciais nunca são gravados.
- Consulta na tela `/auditoria` (ADMIN e GERENTE) ou na API, com filtros por entidade, ID, usuário, operação e período.

## Principais endpoints

| Método | Caminho                          | Perfis                  |
|--------|----------------------------------|-------------------------|
| GET    | `/api/pedidos`                   | ADMIN, GERENTE, ATENDENTE |
| POST   | `/api/pedidos`                   | GERENTE, ATENDENTE      |
| PUT    | `/api/pedidos/{id}/status?status=` | GERENTE, ATENDENTE    |
| POST   | `/api/pedidos/{id}/desconto`     | GERENTE                 |
| POST   | `/api/pedidos/{id}/cancelar`     | GERENTE                 |
| GET    | `/api/clientes`                  | GERENTE, ATENDENTE      |
| POST/PUT | `/api/clientes`, `/api/clientes/{id}` | ATENDENTE          |
| DELETE | `/api/clientes/{id}`             | GERENTE                 |
| GET    | `/api/produtos`                  | autenticado             |
| POST/PUT/DELETE | `/api/produtos`         | ADMIN, GERENTE          |
| GET    | `/api/auditoria`                 | ADMIN, GERENTE          |
| *      | `/api/usuarios`                  | ADMIN                   |

Exemplo com autenticação HTTP Basic (não exige token CSRF):

```bash
curl -u gerente:teste123 -X POST http://localhost:8080/api/pedidos/ID/cancelar \
  -H "Content-Type: application/json" \
  -d '{"justificativa": "cliente desistiu"}'
```

## Testes

```bash
sh ./mvnw test
```

São 51 testes automatizados, que não precisam de MongoDB em execução:

- **Regras de negócio (30):** transições de status, cancelamento, desconto, cálculo do total no servidor, auditoria das alterações de clientes (valores anteriores e novos de endereço e telefone), validação de clientes, proteção contra autoalteração de perfil, mascaramento de telefone e exclusão de senhas da auditoria.
- **Permissões das APIs (21):** sobem a configuração real do Spring Security (`@WebMvcTest`) e comprovam o bloqueio no backend. Por exemplo: ATENDENTE e ADMIN recebem 403 ao cancelar ou aplicar desconto; ATENDENTE recebe 403 na auditoria; só o ADMIN gerencia usuários; a auditoria não aceita `POST` nem `DELETE`; requisições de sessão sem token CSRF são recusadas; sem login, a API responde 401.

Os testes também rodam em JDKs mais novos que o 21: o `maven-surefire-plugin` já está configurado para o Mockito funcionar nessas versões.

## Principais alterações em relação à versão inicial

- Perfis passaram de ADMIN/USER para ADMIN, GERENTE e ATENDENTE.
- Novas entidades e serviços para pedidos (itens, valores, responsável, cancelamento e desconto) e para auditoria.
- Telas de pedidos, produtos e auditoria reescritas, com ações conforme o perfil.
- Validação de dados de produtos e clientes no serviço, com mensagens de erro exibidas nas telas.
- Correção de XSS na lista de clientes: os dados passaram a ser inseridos como texto, e os botões aparecem conforme o perfil.
- Respostas de acesso negado em JSON nas APIs (403) e registro em log das tentativas, incluindo falhas de CSRF.
- Testes automatizados de regras de negócio e de permissões.
- Tela de login com mensagens de senha inválida e de saída do sistema.
- Telas de produtos e pedidos montadas conforme o perfil: o ATENDENTE não vê ações de produtos, e o ADMIN não recebe a lista de clientes ao consultar pedidos.
- Busca do cliente no novo pedido digitando o nome ou o telefone.
- Remoção de templates e links que não funcionavam (`produto-form.html`, `sidebar_1.html`, `admin.html`, `configuracoes.html`, rotas `/admin`, `/teste` e `/configuracoes`, links de funcionários, entregadores e horários).
- Remoção dos arquivos do tema AdminLTE (`static/plugins` e `static/dist`, cerca de 2.050 arquivos) que não eram usados por nenhuma página, e de dependências e configurações sem efeito (`lombok`, `jakarta.persistence-api`, `spring.security.user.*`).

## Limitações conhecidas

- A auditoria é gravada depois da alteração do pedido. O MongoDB standalone usado aqui não tem transação, então uma falha entre as duas operações pode deixar um pedido sem registro.
- A imutabilidade da auditoria é garantida pela aplicação (sem endpoints de alteração, repositório só de inclusão e leitura e entidade sem setters). Quem acessa o MongoDB diretamente, por exemplo pelo Compass, ainda consegue alterar os documentos.
- O controle de horários de funcionamento (`horariosAtendimento`) não foi implementado. A coleção só é criada.
- Usuários cadastrados antes desta versão com role `USER` ficam sem perfil (`SEM_PERFIL`) e não acessam o sistema. Basta atribuir um dos três perfis.
- Quando o ADMIN altera o perfil de um usuário que está logado, a mudança vale a partir do próximo login desse usuário (as permissões da sessão são carregadas na autenticação).
