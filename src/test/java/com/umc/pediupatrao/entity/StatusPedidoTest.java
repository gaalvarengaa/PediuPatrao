package com.umc.pediupatrao.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StatusPedidoTest {

    @Test
    void fluxoPermitidoSeguiaOrdemDoEnunciado() {
        assertThat(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.EM_PREPARACAO)).isTrue();
        assertThat(StatusPedido.EM_PREPARACAO.podeIrPara(StatusPedido.PRONTO)).isTrue();
        assertThat(StatusPedido.PRONTO.podeIrPara(StatusPedido.SAIU_PARA_ENTREGA)).isTrue();
        assertThat(StatusPedido.PRONTO.podeIrPara(StatusPedido.RETIRADO)).isTrue();
        assertThat(StatusPedido.SAIU_PARA_ENTREGA.podeIrPara(StatusPedido.FINALIZADO)).isTrue();
        assertThat(StatusPedido.RETIRADO.podeIrPara(StatusPedido.FINALIZADO)).isTrue();
    }

    @Test
    void transicoesInvalidasSaoBloqueadas() {
        assertThat(StatusPedido.EM_PREPARACAO.podeIrPara(StatusPedido.FINALIZADO)).isFalse();
        assertThat(StatusPedido.RECEBIDO.podeIrPara(StatusPedido.PRONTO)).isFalse();
        assertThat(StatusPedido.FINALIZADO.podeIrPara(StatusPedido.RECEBIDO)).isFalse();
        assertThat(StatusPedido.CANCELADO.podeIrPara(StatusPedido.RECEBIDO)).isFalse();
    }

    @Test
    void cancelamentoEDescontoSoAntesDaSaida() {
        assertThat(StatusPedido.PRONTO.isAntesDaSaida()).isTrue();
        assertThat(StatusPedido.SAIU_PARA_ENTREGA.isAntesDaSaida()).isFalse();
        assertThat(StatusPedido.RETIRADO.isAntesDaSaida()).isFalse();
        assertThat(StatusPedido.SAIU_PARA_ENTREGA.isSaida()).isTrue();
    }

    @Test
    void valoresLegadosSaoConvertidos() {
        assertThat(StatusPedido.deValorLegado("EM_PREPARO")).isEqualTo(StatusPedido.EM_PREPARACAO);
        assertThat(StatusPedido.deValorLegado("ENTREGUE")).isEqualTo(StatusPedido.FINALIZADO);
        assertThat(StatusPedido.deValorLegado(null)).isEqualTo(StatusPedido.RECEBIDO);
    }
}
