package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Auditoria das alterações cadastrais de clientes (endereço e telefone).
 */
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void alteracaoDeEnderecoETelefoneEhAuditadaComValoresAnterioresENovos() {
        when(clienteRepository.findById("c1"))
                .thenReturn(Optional.of(cliente("c1", "(11) 98888-1234", "Rua das Flores")));

        clienteService.atualizar("c1", cliente(null, "(11) 97777-4321", "Av. Brasil"));

        ArgumentCaptor<List<CampoAlterado>> alteracoes = ArgumentCaptor.forClass(List.class);
        verify(auditoriaService).registrar(eq(TipoOperacao.ALTERACAO_CLIENTE), eq("Cliente"), eq("c1"),
                alteracoes.capture(), isNull());
        assertThat(alteracoes.getValue()).containsExactly(
                new CampoAlterado("telefone", "(11) 98888-1234", "(11) 97777-4321"),
                new CampoAlterado("logradouro", "Rua das Flores", "Av. Brasil"));
    }

    @Test
    void salvarSemMudancasNaoGeraRegistro() {
        when(clienteRepository.findById("c1"))
                .thenReturn(Optional.of(cliente("c1", "(11) 98888-1234", "Rua das Flores")));

        clienteService.atualizar("c1", cliente(null, "(11) 98888-1234", "Rua das Flores"));

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void inclusaoDeClienteEhAuditada() {
        clienteService.incluir(cliente(null, "(11) 98888-1234", "Rua das Flores"));

        verify(auditoriaService).registrar(eq(TipoOperacao.INCLUSAO_CLIENTE), eq("Cliente"), any(), any(), isNull());
    }

    @Test
    void telefoneInvalidoEhRejeitadoNoServidor() {
        assertThatThrownBy(() -> clienteService.incluir(cliente(null, "1234", "Rua das Flores")))
                .isInstanceOf(RegraNegocioException.class);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void clienteComPedidosNaoPodeSerExcluido() {
        when(clienteRepository.findById("c1"))
                .thenReturn(Optional.of(cliente("c1", "(11) 98888-1234", "Rua das Flores")));
        when(pedidoRepository.existsByClienteId("c1")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.excluir("c1"))
                .isInstanceOf(RegraNegocioException.class);
        verify(clienteRepository, never()).deleteById(anyString());
    }

    private Cliente cliente(String id, String telefone, String logradouro) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNome("Ana Silva");
        cliente.setTelefone(telefone);
        cliente.setLogradouro(logradouro);
        return cliente;
    }
}
