package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.DashboardIndicadoresService;
import com.umc.pediupatrao.service.DashboardProdutosService;
import com.umc.pediupatrao.service.DashboardSituacaoService;
import com.umc.pediupatrao.service.DashboardVendasService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardPermissoesTest {

    private static final String[] ENDPOINTS = {
        "/api/dashboard/indicadores",
        "/api/dashboard/vendas",
        "/api/dashboard/status",
        "/api/dashboard/produtos-mais-vendidos"
    };

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
    void semLoginRecebe401() throws Exception {
        for (String endpoint : ENDPOINTS) {
            mvc.perform(get(endpoint))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void adminEGerenteConsultam() throws Exception {
        for (String perfil : new String[]{"ADMIN", "GERENTE"}) {
            for (String endpoint : ENDPOINTS) {
                mvc.perform(get(endpoint).with(user(perfil.toLowerCase()).roles(perfil)))
                        .andExpect(status().isOk());
            }
        }
    }

    @Test
    void atendenteRecebe403EmTodosOsEndpoints() throws Exception {
        for (String endpoint : ENDPOINTS) {
            mvc.perform(get(endpoint).with(user("atendente").roles("ATENDENTE")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.erro").value("Acesso negado: seu perfil não tem permissão para esta operação."));
        }
        verify(indicadoresService, never()).calcular(any());
    }

    @Test
    void naoAceitaAlteracoes() throws Exception {
        mvc.perform(post("/api/dashboard/indicadores").with(user("gerente").roles("GERENTE")).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }
}
