package com.umc.pediupatrao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.NovoPedidoRequest;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private UsuarioLogado usuarioLogado;

    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pedidoService = new PedidoService(pedidoRepository, clienteRepository, produtoRepository,
                auditoriaService, usuarioLogado);
        when(usuarioLogado.getUsername()).thenReturn("atendente");
        when(usuarioLogado.getPerfil()).thenReturn("ATENDENTE");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void criarPedidoCalculaValorComPrecoDoCadastro() {
        Produto pizza = new Produto();
        pizza.setId("p1");
        pizza.setNome("Calabresa");
        pizza.setPreco(30.0);
        pizza.setAtivo(true);
        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(pizza));

        Pedido pedido = pedidoService.criarPedido(
                new NovoPedidoRequest("c1", List.of(new ItemPedidoRequest("p1", 2))));

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.RECEBIDO);
        assertThat(pedido.getResponsavelEntrada()).isEqualTo("atendente");
        assertThat(pedido.getSubtotal()).isEqualByComparingTo("60.00");
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("60.00");
    }

    @Test
    void criarPedidoRejeitaProdutoInativo() {
        Produto inativo = new Produto();
        inativo.setId("p1");
        inativo.setNome("Refrigerante");
        inativo.setPreco(5.0);
        inativo.setAtivo(false);
        when(clienteRepository.existsById("c1")).thenReturn(true);
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> pedidoService.criarPedido(
                new NovoPedidoRequest("c1", List.of(new ItemPedidoRequest("p1", 1)))))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void atualizarStatusRejeitaTransicaoInvalida() {
        Pedido pedido = pedidoEm(StatusPedido.EM_PREPARACAO);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.atualizarStatus("id", StatusPedido.FINALIZADO))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void atualizarStatusNaSaidaRegistraResponsavel() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        Pedido salvo = pedidoService.atualizarStatus("id", StatusPedido.RETIRADO);

        assertThat(salvo.getStatus()).isEqualTo(StatusPedido.RETIRADO);
        assertThat(salvo.getResponsavelSaida()).isEqualTo("atendente");
        assertThat(salvo.getDataHoraSaida()).isNotNull();
    }

    @Test
    void atualizarStatusNaoPermiteCancelarPorAqui() {
        Pedido pedido = pedidoEm(StatusPedido.PRONTO);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.atualizarStatus("id", StatusPedido.CANCELADO))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void nenhumStatusPodeMudarDepoisDoCancelamento() {
        Pedido pedido = pedidoEm(StatusPedido.CANCELADO);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.atualizarStatus("id", StatusPedido.EM_PREPARACAO))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cancelamentoExigeJustificativa() {
        Pedido pedido = pedidoEm(StatusPedido.RECEBIDO);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar("id", "abc"))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cancelamentoBloqueadoAposSaida() {
        Pedido pedido = pedidoEm(StatusPedido.SAIU_PARA_ENTREGA);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar("id", "cliente desistiu do pedido"))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cancelamentoAntesDaSaidaGuardaDados() {
        Pedido pedido = pedidoEm(StatusPedido.EM_PREPARACAO);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        Pedido cancelado = pedidoService.cancelar("id", "cliente desistiu do pedido");

        assertThat(cancelado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(cancelado.getCanceladoPor()).isEqualTo("atendente");
        assertThat(cancelado.getJustificativaCancelamento()).isEqualTo("cliente desistiu do pedido");
        assertThat(cancelado.getItens()).hasSize(1);
    }

    @Test
    void descontoAcimaDoLimiteEhRejeitado() {
        Pedido pedido = pedidoEm(StatusPedido.RECEBIDO);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.aplicarDesconto("id", new BigDecimal("21"), null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void descontoRecalculaTotal() {
        Pedido pedido = pedidoEm(StatusPedido.RECEBIDO);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        Pedido comDesconto = pedidoService.aplicarDesconto("id", new BigDecimal("10"), "cliente fiel");

        assertThat(comDesconto.getValorDesconto()).isEqualByComparingTo("5.00");
        assertThat(comDesconto.getValorTotal()).isEqualByComparingTo("45.00");
        assertThat(comDesconto.getDescontoAplicadoPor()).isEqualTo("atendente");
    }

    @Test
    void descontoBloqueadoAposSaida() {
        Pedido pedido = pedidoEm(StatusPedido.FINALIZADO);
        when(pedidoRepository.findById("id")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.aplicarDesconto("id", new BigDecimal("10"), null))
                .isInstanceOf(RegraNegocioException.class);
    }

    /** Pedido de 2 x R$ 25,00 (subtotal R$ 50,00) no status informado. */
    private Pedido pedidoEm(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setId("id");
        pedido.setClienteId("c1");
        pedido.setItens(new java.util.ArrayList<>(List.of(
                new ItemPedido("p1", "Pizza", 2, new BigDecimal("25.00")))));
        pedido.setStatus(status);
        pedido.setPercentualDesconto(BigDecimal.ZERO);
        pedido.setSubtotal(new BigDecimal("50.00"));
        pedido.setValorTotal(new BigDecimal("50.00"));
        return pedido;
    }
}
