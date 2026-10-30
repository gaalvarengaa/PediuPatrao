package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.ProdutoVendido;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardProdutosService {

    static final int LIMITE_MAXIMO = 50;

    private final DashboardConsultaService consultaService;

    public DashboardProdutosService(DashboardConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    public List<ProdutoVendido> calcular(FiltroDashboard filtro, int limite) {
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new RegraNegocioException("O limite deve estar entre 1 e " + LIMITE_MAXIMO + ".");
        }
        DashboardConsultaService.Consulta consulta = consultaService.consultar(filtro);

        Map<String, Acumulado> produtos = new LinkedHashMap<>();
        for (Pedido pedido : consulta.pedidos()) {
            if (DashboardConsultaService.naoCancelado(pedido)) {
                DashboardConsultaService.itensDe(pedido).stream()
                        .filter(consulta.itemAtende())
                        .forEach(item -> acumular(produtos, item));
            }
        }

        return produtos.values().stream()
                .map(a -> new ProdutoVendido(a.produtoId, a.nome, a.quantidade,
                        a.faturamento.setScale(2, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparingLong(ProdutoVendido::quantidade).reversed()
                        .thenComparing(ProdutoVendido::faturamento, Comparator.reverseOrder())
                        .thenComparing(ProdutoVendido::nome, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limite)
                .toList();
    }

    // Os pedidos vêm do mais antigo para o mais recente, então prevalece o nome mais recente do produto
    private void acumular(Map<String, Acumulado> produtos, ItemPedido item) {
        String chave = item.getProdutoId() != null ? item.getProdutoId() : item.getNomeProduto();
        Acumulado acumulado = produtos.computeIfAbsent(chave, k -> new Acumulado());
        acumulado.produtoId = item.getProdutoId();
        acumulado.nome = item.getNomeProduto();
        acumulado.quantidade += item.getQuantidade();
        acumulado.faturamento = acumulado.faturamento.add(DashboardConsultaService.valorOuZero(item.getSubtotal()));
    }

    private static class Acumulado {
        private String produtoId;
        private String nome;
        private long quantidade;
        private BigDecimal faturamento = BigDecimal.ZERO;
    }
}
