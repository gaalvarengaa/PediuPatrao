package com.umc.pediupatrao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.AuditoriaService;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Garante as restrições de perfil nas APIs de auditoria, usuários e clientes.
 */
@WebMvcTest({AuditoriaController.class, UsuarioController.class, ClienteController.class})
@Import(SecurityConfig.class)
class AuditoriaUsuarioClientePermissoesTest {

    private static final String CLIENTE = "{\"nome\":\"Maria\",\"telefone\":\"(11) 98888-1234\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    // ===== Auditoria =====

    @Test
    void adminEGerenteConsultamAuditoria() throws Exception {
        mvc.perform(get("/api/auditoria").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auditoria").with(user("gerente").roles("GERENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void atendenteNaoConsultaAuditoria() throws Exception {
        mvc.perform(get("/api/auditoria").with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditoriaNaoPodeSerAlteradaNemExcluida() throws Exception {
        mvc.perform(delete("/api/auditoria").with(user("gerente").roles("GERENTE")).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/auditoria").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ===== Usuários =====

    @Test
    void somenteAdminGerenciaUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").with(user("gerente").roles("GERENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").with(user("atendente").roles("ATENDENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void gerenteNaoCriaUsuarioAdmin() throws Exception {
        mvc.perform(post("/api/usuarios").with(user("gerente").roles("GERENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"novo\",\"password\":\"123456\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        verify(usuarioService, never()).salvarUsuario(any());
    }

    // ===== Clientes =====

    @Test
    void atendenteCadastraCliente() throws Exception {
        mvc.perform(post("/api/clientes").with(user("atendente").roles("ATENDENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CLIENTE))
                .andExpect(status().isCreated());
    }

    @Test
    void gerenteEAdminNaoCadastramCliente() throws Exception {
        mvc.perform(post("/api/clientes").with(user("gerente").roles("GERENTE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CLIENTE))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/clientes").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CLIENTE))
                .andExpect(status().isForbidden());
    }

    @Test
    void somenteGerenteExcluiCliente() throws Exception {
        mvc.perform(delete("/api/clientes/1").with(user("atendente").roles("ATENDENTE")).with(csrf()))
                .andExpect(status().isForbidden());
        verify(clienteService, never()).excluir(anyString());

        mvc.perform(delete("/api/clientes/1").with(user("gerente").roles("GERENTE")).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminNaoConsultaClientes() throws Exception {
        mvc.perform(get("/api/clientes").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
