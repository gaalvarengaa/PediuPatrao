package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GeradorPedidosDemo {

    public static final int DIAS = 90;

    private static final String ATENDENTE = "atendente";
    private static final String GERENTE = "gerente";

    private final Random random;

    public GeradorPedidosDemo(long semente) {
        this.random = new Random(semente);
    }

    public List<Pedido> gerar(List<Produto> produtos, List<Cliente> clientes, LocalDateTime agora) {
        List<Produto> ativos = produtos.stream().filter(p -> Boolean.TRUE.equals(p.getAtivo())).toList();
        if (ativos.isEmpty() || clientes.isEmpty()) {
            return List.of();
        }
        List<Pedido> pedidos = new ArrayList<>();
        LocalDate hoje = agora.toLocalDate();
        for (int diasAtras = DIAS - 1; diasAtras >= 0; diasAtras--) {
            LocalDate dia = hoje.minusDays(diasAtras);
            int quantidade = quantidadeDoDia(dia, diasAtras);
            for (int i = 0; i < quantidade; i++) {
                LocalDateTime entrada = dia.atStartOfDay().plusMinutes(minutoDoDia());
                if (entrada.isAfter(agora)) {
                    continue;
                }
                Pedido pedido = novoPedido(ativos, clientes, entrada);
                if (diasAtras == 0) {
                    aplicarStatusDoDia(pedido, agora);
                } else {
                    aplicarStatusConcluido(pedido);
                }
                pedidos.add(pedido);
            }
        }
        pedidos.addAll(pedidosEmAndamento(ativos, clientes, agora));
        return pedidos;
    }

    private List<Pedido> pedidosEmAndamento(List<Produto> produtos, List<Cliente> clientes, LocalDateTime agora) {
        List<Pedido> emAndamento = new ArrayList<>();
        int quantidade = 4 + random.nextInt(3);
        for (int i = 0; i < quantidade; i++) {
            LocalDateTime entrada = agora.minusMinutes(5 + (long) i * 70 / quantidade);
            if (!entrada.toLocalDate().equals(agora.toLocalDate())) {
                continue;
            }
            Pedido pedido = novoPedido(produtos, clientes, entrada);
            aplicarStatusDoDia(pedido, agora);
            emAndamento.add(pedido);
        }
        return emAndamento;
    }

    private int quantidadeDoDia(LocalDate dia, int diasAtras) {
        DayOfWeek semana = dia.getDayOfWeek();
        boolean movimentado = semana == DayOfWeek.FRIDAY || semana == DayOfWeek.SATURDAY || semana == DayOfWeek.SUNDAY;
        int base = movimentado ? 8 + random.nextInt(5) : 4 + random.nextInt(4);
        int crescimento = (DIAS - diasAtras) * 3 / DIAS;
        return base + crescimento;
    }

    private int minutoDoDia() {
        if (random.nextInt(100) < 25) {
            return 11 * 60 + random.nextInt(3 * 60);
        }
        return 18 * 60 + random.nextInt(5 * 60);
    }

    private Pedido novoPedido(List<Produto> produtos, List<Cliente> clientes, LocalDateTime entrada) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(clientes.get(random.nextInt(clientes.size())).getId());
        pedido.setDataHoraEntrada(entrada);
        pedido.setResponsavelEntrada(ATENDENTE);
        pedido.setStatus(StatusPedido.RECEBIDO);

        List<ItemPedido> itens = new ArrayList<>();
        for (Produto produto : escolherProdutos(produtos)) {
            int quantidade = random.nextInt(100) < 75 ? 1 : 2;
            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            ItemPedido item = new ItemPedido(produto.getId(), produto.getNome(), quantidade, preco);
            item.setTipoProduto(produto.getTipo());
            itens.add(item);
        }
        pedido.setItens(itens);

        BigDecimal subtotal = itens.stream().map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentual = BigDecimal.ZERO;
        if (random.nextInt(100) < 15) {
            percentual = random.nextBoolean() ? new BigDecimal("5") : new BigDecimal("10");
            pedido.setDescontoAplicadoPor(GERENTE);
            pedido.setDataHoraDesconto(entrada.plusMinutes(3));
        }
        BigDecimal desconto = subtotal.multiply(percentual).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        pedido.setSubtotal(subtotal);
        pedido.setPercentualDesconto(percentual.setScale(2, RoundingMode.HALF_UP));
        pedido.setValorDesconto(desconto);
        pedido.setValorTotal(subtotal.subtract(desconto));
        return pedido;
    }

    private Set<Produto> escolherProdutos(List<Produto> produtos) {
        int quantidade = Math.min(produtos.size(), 1 + random.nextInt(3));
        Set<Produto> escolhidos = new LinkedHashSet<>();
        while (escolhidos.size() < quantidade) {
            escolhidos.add(produtoPonderado(produtos));
        }
        return escolhidos;
    }

    private Produto produtoPonderado(List<Produto> produtos) {
        int total = produtos.stream().mapToInt(this::peso).sum();
        int sorteio = random.nextInt(total);
        for (Produto produto : produtos) {
            sorteio -= peso(produto);
            if (sorteio < 0) {
                return produto;
            }
        }
        return produtos.get(produtos.size() - 1);
    }

    private int peso(Produto produto) {
        String tipo = produto.getTipo() == null ? "" : produto.getTipo();
        int base = switch (tipo) {
            case "Pizza" -> 10;
            case "Bebida" -> 7;
            default -> 3;
        };
        return base + Math.abs(produto.getNome().hashCode() % 5);
    }

    private void aplicarStatusConcluido(Pedido pedido) {
        if (random.nextInt(100) < 6) {
            cancelar(pedido);
            return;
        }
        StatusPedido saida = random.nextInt(100) < 60 ? StatusPedido.SAIU_PARA_ENTREGA : StatusPedido.RETIRADO;
        registrarSaida(pedido, saida);
        pedido.setStatus(StatusPedido.FINALIZADO);
        pedido.setDataHoraFinalizacao(pedido.getDataHoraSaida().plusMinutes(15 + random.nextInt(25)));
        pedido.setResponsavelFinalizacao(ATENDENTE);
    }

    private void aplicarStatusDoDia(Pedido pedido, LocalDateTime agora) {
        long minutos = Duration.between(pedido.getDataHoraEntrada(), agora).toMinutes();
        if (random.nextInt(100) < 5) {
            cancelar(pedido);
        } else if (minutos < 15) {
            pedido.setStatus(StatusPedido.RECEBIDO);
        } else if (minutos < 35) {
            pedido.setStatus(StatusPedido.EM_PREPARACAO);
        } else if (minutos < 45) {
            pedido.setStatus(StatusPedido.PRONTO);
        } else if (minutos < 75) {
            registrarSaida(pedido, random.nextBoolean() ? StatusPedido.SAIU_PARA_ENTREGA : StatusPedido.RETIRADO);
        } else {
            aplicarStatusConcluido(pedido);
            if (pedido.getDataHoraFinalizacao() != null && pedido.getDataHoraFinalizacao().isAfter(agora)) {
                pedido.setDataHoraFinalizacao(agora);
            }
        }
    }

    private void registrarSaida(Pedido pedido, StatusPedido saida) {
        pedido.setStatus(saida);
        pedido.setDataHoraSaida(pedido.getDataHoraEntrada().plusMinutes(30 + random.nextInt(15)));
        pedido.setResponsavelSaida(ATENDENTE);
    }

    private void cancelar(Pedido pedido) {
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setCanceladoPor(GERENTE);
        pedido.setJustificativaCancelamento(random.nextBoolean() ? "Cliente desistiu do pedido" : "Endereço fora da área de entrega");
        pedido.setDataHoraCancelamento(pedido.getDataHoraEntrada().plusMinutes(5 + random.nextInt(10)));
    }
}
