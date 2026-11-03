package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.AgrupamentoVendas;
import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.IndicadoresDashboard;
import com.umc.pediupatrao.dto.ProdutoVendido;
import com.umc.pediupatrao.dto.SituacaoPedidos;
import com.umc.pediupatrao.dto.VendasDashboard;
import com.umc.pediupatrao.service.DashboardIndicadoresService;
import com.umc.pediupatrao.service.DashboardProdutosService;
import com.umc.pediupatrao.service.DashboardSituacaoService;
import com.umc.pediupatrao.service.DashboardVendasService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardIndicadoresService indicadoresService;
    private final DashboardVendasService vendasService;
    private final DashboardSituacaoService situacaoService;
    private final DashboardProdutosService produtosService;

    public DashboardController(DashboardIndicadoresService indicadoresService, DashboardVendasService vendasService,
            DashboardSituacaoService situacaoService, DashboardProdutosService produtosService) {
        this.indicadoresService = indicadoresService;
        this.vendasService = vendasService;
        this.situacaoService = situacaoService;
        this.produtosService = produtosService;
    }

    @GetMapping("/indicadores")
    public IndicadoresDashboard indicadores(FiltroDashboard filtro) {
        return indicadoresService.calcular(filtro);
    }

    @GetMapping("/vendas")
    public VendasDashboard vendas(FiltroDashboard filtro,
            @RequestParam(defaultValue = "DIA") AgrupamentoVendas agrupamento) {
        return vendasService.calcular(filtro, agrupamento);
    }

    @GetMapping("/status")
    public List<SituacaoPedidos> status(FiltroDashboard filtro) {
        return situacaoService.calcular(filtro);
    }

    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoVendido> produtosMaisVendidos(FiltroDashboard filtro,
            @RequestParam(defaultValue = "10") int limite) {
        return produtosService.calcular(filtro, limite);
    }
}
