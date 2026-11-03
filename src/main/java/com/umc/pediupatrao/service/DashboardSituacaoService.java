package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.SituacaoPedidos;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardSituacaoService {

    private final DashboardConsultaService consultaService;

    public DashboardSituacaoService(DashboardConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    public List<SituacaoPedidos> calcular(FiltroDashboard filtro) {
        List<Pedido> pedidos = consultaService.consultar(filtro).pedidos();
        return Arrays.stream(StatusPedido.values())
                .map(status -> new SituacaoPedidos(status, status.getDescricao(), contar(pedidos, status)))
                .toList();
    }

    private long contar(List<Pedido> pedidos, StatusPedido status) {
        return pedidos.stream().filter(p -> DashboardConsultaService.statusDe(p) == status).count();
    }
}
