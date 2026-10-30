package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.AgrupamentoVendas;
import com.umc.pediupatrao.dto.FiltroDashboard;
import com.umc.pediupatrao.dto.IndicadoresDashboard;
import com.umc.pediupatrao.dto.ProdutoVendido;
import com.umc.pediupatrao.dto.SituacaoPedidos;
import com.umc.pediupatrao.dto.VendasDashboard;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.service.DashboardIndicadoresService;
import com.umc.pediupatrao.service.DashboardProdutosService;
import com.umc.pediupatrao.service.DashboardSituacaoService;
import com.umc.pediupatrao.service.DashboardVendasService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Garante o formato das respostas e o tratamento de parâmetros descritos na documentação da API.
 */
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    private static final FiltroDashboard SEM_FILTROS = new FiltroDashboard(null, null, null, null, null);
    private static final RequestPostProcessor GERENTE = user("gerente").roles("GERENTE");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardIndicadoresService indicadoresService;
    @MockitoBean
    private DashboardVendasService vendasService;
    @MockitoBean
    private DashboardSituacaoService situacaoService;
    @MockitoBean
    private DashboardProdutosService produtosService;
    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void indicadoresRetornamOsValoresCalculados() throws Exception {
        when(indicadoresService.calcular(SEM_FILTROS)).thenReturn(new IndicadoresDashboard(
                new BigDecimal("1250.50"), 30, new BigDecimal("41.68"), 2, new BigDecimal("35.00")));

        mvc.perform(get("/api/dashboard/indicadores").with(GERENTE))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"faturamento":1250.50,"pedidos":30,"ticketMedio":41.68,"cancelamentos":2,"descontos":35.00}
                        """));
    }

    @Test
    void repassaTodosOsFiltrosAoServico() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores").with(GERENTE)
                        .param("de", "2026-10-01").param("ate", "2026-10-31")
                        .param("status", "PRONTO").param("produtoId", "p1").param("tipo", "Pizza"))
                .andExpect(status().isOk());

        verify(indicadoresService).calcular(new FiltroDashboard(LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31), StatusPedido.PRONTO, "p1", "Pizza"));
    }

    @Test
    void vendasUsamAgrupamentoPorDiaComoPadrao() throws Exception {
        when(vendasService.calcular(SEM_FILTROS, AgrupamentoVendas.DIA)).thenReturn(new VendasDashboard(
                AgrupamentoVendas.DIA, List.of(new VendasDashboard.Ponto("2026-10-01", new BigDecimal("412.00"), 9))));

        mvc.perform(get("/api/dashboard/vendas").with(GERENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agrupamento").value("DIA"))
                .andExpect(jsonPath("$.pontos[0].periodo").value("2026-10-01"))
                .andExpect(jsonPath("$.pontos[0].faturamento").value(412.00))
                .andExpect(jsonPath("$.pontos[0].pedidos").value(9));
    }

    @Test
    void statusRetornaSituacaoComDescricao() throws Exception {
        when(situacaoService.calcular(SEM_FILTROS)).thenReturn(List.of(
                new SituacaoPedidos(StatusPedido.EM_PREPARACAO, "Em preparação", 3)));

        mvc.perform(get("/api/dashboard/status").with(GERENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("EM_PREPARACAO"))
                .andExpect(jsonPath("$[0].descricao").value("Em preparação"))
                .andExpect(jsonPath("$[0].quantidade").value(3));
    }

    @Test
    void produtosUsamLimite10ComoPadrao() throws Exception {
        when(produtosService.calcular(SEM_FILTROS, 10)).thenReturn(List.of(
                new ProdutoVendido("p1", "Pizza Calabresa", 18, new BigDecimal("810.00"))));

        mvc.perform(get("/api/dashboard/produtos-mais-vendidos").with(GERENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Pizza Calabresa"))
                .andExpect(jsonPath("$[0].quantidade").value(18));
    }

    @Test
    void statusInvalidoRetorna400ComMensagem() throws Exception {
        mvc.perform(get("/api/dashboard/indicadores").with(GERENTE).param("status", "INEXISTENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido para o parâmetro 'status'."));
    }

    @Test
    void dataInvalidaRetorna400ComMensagem() throws Exception {
        mvc.perform(get("/api/dashboard/status").with(GERENTE).param("de", "31/10/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido para o parâmetro 'de'."));
    }

    @Test
    void agrupamentoInvalidoRetorna400ComMensagem() throws Exception {
        mvc.perform(get("/api/dashboard/vendas").with(GERENTE).param("agrupamento", "ANO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido para o parâmetro 'agrupamento'."));
    }

    @Test
    void limiteNaoNumericoRetorna400ComMensagem() throws Exception {
        mvc.perform(get("/api/dashboard/produtos-mais-vendidos").with(GERENTE).param("limite", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido para o parâmetro 'limite'."));
    }

    @Test
    void violacaoDeRegraRetorna422ComMensagem() throws Exception {
        when(indicadoresService.calcular(any())).thenThrow(
                new RegraNegocioException("A data inicial não pode ser posterior à data final."));

        mvc.perform(get("/api/dashboard/indicadores").with(GERENTE))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("A data inicial não pode ser posterior à data final."));
    }
}
