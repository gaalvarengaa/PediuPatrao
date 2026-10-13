package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.NovoPedidoRequest;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    static final String ENTIDADE = "Pedido";
    static final int QUANTIDADE_MAXIMA_POR_ITEM = 50;

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;
    private final UsuarioLogado usuarioLogado;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository, AuditoriaService auditoriaService, UsuarioLogado usuarioLogado) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
        this.usuarioLogado = usuarioLogado;
    }

    /**
     * Registra a entrada do pedido. Preços vêm do cadastro de produtos e os valores
     * são calculados aqui; nada disso é aceito da requisição.
     */
    public Pedido criarPedido(NovoPedidoRequest dados) {
        if (dados == null || vazio(dados.clienteId())) {
            throw new RegraNegocioException("Informe o cliente do pedido.");
        }
        if (!clienteRepository.existsById(dados.clienteId())) {
            throw new RegraNegocioException("Cliente não encontrado.");
        }
        if (dados.itens() == null || dados.itens().isEmpty()) {
            throw new RegraNegocioException("O pedido deve ter pelo menos um item.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        for (ItemPedidoRequest item : dados.itens()) {
            itens.add(montarItem(item));
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(dados.clienteId());
        pedido.setItens(itens);
        pedido.setPercentualDesconto(BigDecimal.ZERO);
        recalcularValores(pedido);
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.setResponsavelEntrada(usuarioLogado.getUsername());

        Pedido salvo = pedidoRepository.save(pedido);

        auditoriaService.registrar(TipoOperacao.INCLUSAO_PEDIDO, ENTIDADE, salvo.getId(),
                AuditoriaService.compararCampos(Map.of(), camposAuditados(salvo)), null);
        return salvo;
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHoraEntrada"));
    }

    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    public Pedido atualizarStatus(String id, StatusPedido status) {
        Pedido pedido = buscarPorId(id);
        pedido.setStatus(status);
        return pedidoRepository.save(pedido);
    }

    private ItemPedido montarItem(ItemPedidoRequest item) {
        if (item == null || vazio(item.produtoId())) {
            throw new RegraNegocioException("Informe o produto de cada item.");
        }
        Integer quantidade = item.quantidade();
        if (quantidade == null || quantidade < 1 || quantidade > QUANTIDADE_MAXIMA_POR_ITEM) {
            throw new RegraNegocioException(
                    "A quantidade de cada item deve estar entre 1 e " + QUANTIDADE_MAXIMA_POR_ITEM + ".");
        }

        Produto produto = produtoRepository.findById(item.produtoId())
                .orElseThrow(() -> new RegraNegocioException("Produto não encontrado: " + item.produtoId()));
        if (Boolean.FALSE.equals(produto.getAtivo())) {
            throw new RegraNegocioException("O produto " + produto.getNome() + " está inativo.");
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new RegraNegocioException("O produto " + produto.getNome() + " não possui preço válido.");
        }

        BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
        return new ItemPedido(produto.getId(), produto.getNome(), quantidade, preco);
    }

    /** Subtotal = soma dos itens; desconto = subtotal x percentual; total = subtotal - desconto. */
    void recalcularValores(Pedido pedido) {
        BigDecimal subtotal = pedido.getItens().stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal percentual = pedido.getPercentualDesconto() != null ? pedido.getPercentualDesconto() : BigDecimal.ZERO;
        BigDecimal desconto = subtotal.multiply(percentual)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        pedido.setSubtotal(subtotal);
        pedido.setValorDesconto(desconto);
        pedido.setValorTotal(subtotal.subtract(desconto));
    }

    private Map<String, Object> camposAuditados(Pedido pedido) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("clienteId", pedido.getClienteId());
        campos.put("itens", pedido.getItens().toString());
        campos.put("subtotal", pedido.getSubtotal());
        campos.put("percentualDesconto", pedido.getPercentualDesconto());
        campos.put("valorTotal", pedido.getValorTotal());
        campos.put("status", pedido.getStatus());
        campos.put("dataHoraEntrada", pedido.getDataHoraEntrada());
        campos.put("responsavelEntrada", pedido.getResponsavelEntrada());
        return campos;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
