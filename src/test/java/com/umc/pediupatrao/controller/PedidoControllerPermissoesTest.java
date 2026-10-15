package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Garante que as restrições de perfil da API de pedidos são aplicadas no backend.
 */
@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class PedidoControllerPermissoesTest {

    private static final String CANCELAMENTO = "{\"justificativa\":\"cliente desistiu\"}";
    private static final String DESCONTO = "{\"percentual\":10}";
    private static final String NOVO_PEDIDO = "{\"clienteId\":\"c1\",\"itens\":[{\"produtoId\":\"p1\",\"quantidade\":1}]}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PedidoService pedidoService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void semLoginRecebe401() throws Exception {
        mvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void todosOsPerfisConsultamPedidos() throws Exception {
        for (String perfil : new String[]{"ADMIN", "GERENTE", "ATENDENTE"}) {
            mvc.perform(get("/api/pedidos").with(user(perfil.toLowerCase()).roles(perfil)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void atendenteNaoPodeCancelar() throws Exception {
        mvc.perform(post("/api/pedidos/1/cancelar").with(user("atendente").roles("ATENDENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isForbidden());
        verify(pedidoService, never()).cancelar(anyString(), anyString());
    }

    @Test
    void adminNaoPodeCancelar() throws Exception {
        mvc.perform(post("/api/pedidos/1/cancelar").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isForbidden());
        verify(pedidoService, never()).cancelar(anyString(), anyString());
    }

    @Test
    void gerentePodeCancelar() throws Exception {
        when(pedidoService.cancelar(anyString(), anyString())).thenReturn(new Pedido());

        mvc.perform(post("/api/pedidos/1/cancelar").with(user("gerente").roles("GERENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CANCELAMENTO))
                .andExpect(status().isOk());
    }

    @Test
    void atendenteNaoPodeDarDesconto() throws Exception {
        mvc.perform(post("/api/pedidos/1/desconto").with(user("atendente").roles("ATENDENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DESCONTO))
                .andExpect(status().isForbidden());
        verify(pedidoService, never()).aplicarDesconto(anyString(), any(), any());
    }

    @Test
    void adminNaoPodeDarDesconto() throws Exception {
        mvc.perform(post("/api/pedidos/1/desconto").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DESCONTO))
                .andExpect(status().isForbidden());
        verify(pedidoService, never()).aplicarDesconto(anyString(), any(), any());
    }

    @Test
    void gerentePodeDarDesconto() throws Exception {
        when(pedidoService.aplicarDesconto(anyString(), any(), any())).thenReturn(new Pedido());

        mvc.perform(post("/api/pedidos/1/desconto").with(user("gerente").roles("GERENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DESCONTO))
                .andExpect(status().isOk());
    }

    @Test
    void adminNaoRegistraPedido() throws Exception {
        mvc.perform(post("/api/pedidos").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_PEDIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void atendenteRegistraPedido() throws Exception {
        when(pedidoService.criarPedido(any())).thenReturn(new Pedido());

        mvc.perform(post("/api/pedidos").with(user("atendente").roles("ATENDENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(NOVO_PEDIDO))
                .andExpect(status().isCreated());
    }

    @Test
    void adminNaoAlteraStatus() throws Exception {
        mvc.perform(put("/api/pedidos/1/status").param("status", "EM_PREPARACAO")
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void requisicaoDeSessaoSemTokenCsrfEhBloqueada() throws Exception {
        mvc.perform(post("/api/pedidos/1/desconto").with(user("gerente").roles("GERENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content(DESCONTO))
                .andExpect(status().isForbidden());
        verify(pedidoService, never()).aplicarDesconto(anyString(), any(), any());
    }
}
