package com.umc.pediupatrao.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.ProdutoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class DashboardPaginaTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PedidoService pedidoService;
    @MockitoBean
    private ClienteService clienteService;
    @MockitoBean
    private ProdutoService produtoService;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private AuditoriaService auditoriaService;
    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void adminEGerenteAbremODashboard() throws Exception {
        for (String perfil : new String[]{"ADMIN", "GERENTE"}) {
            mvc.perform(get("/dashboard").with(user(perfil.toLowerCase()).roles(perfil)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout"))
                    .andExpect(model().attribute("content", "dashboard :: content"));
        }
    }

    @Test
    void atendenteVaiParaAcessoNegado() throws Exception {
        mvc.perform(get("/dashboard").with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void semLoginVaiParaOLogin() throws Exception {
        mvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
