package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditoriaServiceTest {

    @Test
    void telefoneEhMascaradoMantendoOsQuatroUltimosDigitos() {
        assertThat(AuditoriaService.mascarar("(11) 98765-4321")).isEqualTo("(**) *****-4321");
    }

    @Test
    void comparacaoIgnoraCamposSemMudanca() {
        List<CampoAlterado> alteracoes = AuditoriaService.compararCampos(
                Map.of("nome", "Ana", "cidade", "Campinas"),
                Map.of("nome", "Ana", "cidade", "Santos"));

        assertThat(alteracoes).containsExactly(new CampoAlterado("cidade", "Campinas", "Santos"));
    }

    @Test
    void senhasNuncaSaoGravadasNaAuditoria() {
        AuditoriaRepository repositorio = mock(AuditoriaRepository.class);
        UsuarioLogado usuarioLogado = mock(UsuarioLogado.class);
        when(usuarioLogado.getUsername()).thenReturn("admin");
        when(usuarioLogado.getPerfil()).thenReturn("ADMIN");
        when(repositorio.insert(any(RegistroAuditoria.class))).thenAnswer(i -> i.getArgument(0));
        AuditoriaService servico = new AuditoriaService(repositorio, usuarioLogado);

        servico.registrar(TipoOperacao.ALTERACAO_CLIENTE, "Cliente", "c1", List.of(
                new CampoAlterado("senha", "antiga", "nova"),
                new CampoAlterado("logradouro", "Rua das Flores", "Av. Brasil")), null);

        verify(repositorio).insert(org.mockito.ArgumentMatchers.argThat(registro ->
                registro.getAlteracoes().stream().noneMatch(a -> a.campo().equals("senha"))
                        && registro.getAlteracoes().size() == 1));
    }
}
