package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

/**
 * Busca os pedidos do dashboard. Os valores são calculados em Java porque o Spring Data
 * grava BigDecimal como texto no MongoDB, o que impede somas diretas em agregações.
 */
@Service
public class DashboardConsultaService {

    static final int DIAS_PADRAO = 30;

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    public DashboardConsultaService(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
    }

    /** Resultado da consulta: período aplicado, pedidos encontrados e critério dos itens para o ranking. */
    public record Consulta(LocalDate de, LocalDate ate, List<Pedido> pedidos, Predicate<ItemPedido> itemAtende) {
    }

    public Consulta consultar(FiltroDashboard filtro) {
        LocalDate ate = filtro.ate() != null ? filtro.ate() : LocalDate.now();
        LocalDate de = filtro.de() != null ? filtro.de() : ate.minusDays(DIAS_PADRAO - 1);
        if (de.isAfter(ate)) {
            throw new RegraNegocioException("A data inicial não pode ser posterior à data final.");
        }

        Predicate<ItemPedido> itemAtende = criterioDosItens(filtro);
        boolean filtraItens = !vazio(filtro.produtoId()) || !vazio(filtro.tipo());

        List<Pedido> pedidos = pedidoRepository.buscarPorPeriodo(de.atStartOfDay(), ate.plusDays(1).atStartOfDay())
                .stream()
                .filter(p -> filtro.status() == null || statusDe(p) == filtro.status())
                .filter(p -> !filtraItens || itensDe(p).stream().anyMatch(itemAtende))
                .toList();
        return new Consulta(de, ate, pedidos, itemAtende);
    }

    private Predicate<ItemPedido> criterioDosItens(FiltroDashboard filtro) {
        Predicate<ItemPedido> criterio = item -> true;
        if (!vazio(filtro.produtoId())) {
            criterio = criterio.and(item -> filtro.produtoId().equals(item.getProdutoId()));
        }
        if (!vazio(filtro.tipo())) {
            Map<String, String> tiposDoCadastro = tiposDoCadastro();
            String tipo = filtro.tipo().trim();
            criterio = criterio.and(item -> tipo.equalsIgnoreCase(tipoDe(item, tiposDoCadastro)));
        }
        return criterio;
    }

    // Pedidos antigos não têm a categoria no item; ela vem do cadastro do produto
    private String tipoDe(ItemPedido item, Map<String, String> tiposDoCadastro) {
        return item.getTipoProduto() != null ? item.getTipoProduto() : tiposDoCadastro.get(item.getProdutoId());
    }

    private Map<String, String> tiposDoCadastro() {
        Map<String, String> tipos = new HashMap<>();
        for (Produto produto : produtoRepository.findAll()) {
            tipos.put(produto.getId(), produto.getTipo());
        }
        return tipos;
    }

    static StatusPedido statusDe(Pedido pedido) {
        return pedido.getStatus() != null ? pedido.getStatus() : StatusPedido.RECEBIDO;
    }

    static boolean naoCancelado(Pedido pedido) {
        return statusDe(pedido) != StatusPedido.CANCELADO;
    }

    static List<ItemPedido> itensDe(Pedido pedido) {
        return pedido.getItens() != null ? pedido.getItens() : List.of();
    }

    // Pedidos antigos podem não ter valores gravados
    static BigDecimal valorOuZero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
