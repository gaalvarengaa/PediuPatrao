package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.service.ProdutoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public List<Produto> listarProdutos() {
        return produtoService.listarProdutos();
    }

    @PostMapping
    public ResponseEntity<Produto> criarProduto(@RequestBody Produto produto) {
        produto.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.salvar(produto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> atualizarProduto(@PathVariable String id, @RequestBody Produto produto) {
        produto.setId(id);
        return ResponseEntity.ok(produtoService.salvar(produto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirProduto(@PathVariable String id) {
        produtoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
