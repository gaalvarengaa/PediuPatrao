package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    static final Set<String> TIPOS = Set.of("Pizza", "Lanche", "Bebida", "Porção", "Adicional");
    static final int TAMANHO_MAXIMO_NOME = 100;

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    /**
     * Cria o produto (sem ID) ou atualiza o existente. Produtos novos entram ativos
     * quando o campo "ativo" não é informado.
     */
    public Produto salvar(Produto dados) {
        Produto produto;
        if (dados.getId() == null || dados.getId().isBlank()) {
            produto = new Produto();
            produto.setAtivo(true);
        } else {
            produto = produtoRepository.findById(dados.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        }

        produto.setNome(dados.getNome() != null ? dados.getNome().trim() : null);
        produto.setTipo(dados.getTipo());
        produto.setPreco(dados.getPreco());
        if (dados.getAtivo() != null) {
            produto.setAtivo(dados.getAtivo());
        }

        validar(produto);
        return produtoRepository.save(produto);
    }

    public void excluir(String id) {
        if (!produtoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Produto não encontrado.");
        }
        // Os itens dos pedidos já copiaram nome e preço, então o histórico é preservado
        produtoRepository.deleteById(id);
    }

    private void validar(Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            throw new RegraNegocioException("O nome do produto é obrigatório.");
        }
        if (produto.getNome().length() > TAMANHO_MAXIMO_NOME) {
            throw new RegraNegocioException("O nome do produto deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
        if (!TIPOS.contains(produto.getTipo())) {
            throw new RegraNegocioException("Tipo de produto inválido.");
        }
        if (produto.getPreco() == null || produto.getPreco() <= 0) {
            throw new RegraNegocioException("O preço deve ser maior que zero.");
        }
    }
}
