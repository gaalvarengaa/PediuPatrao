package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClienteService {

    static final String ENTIDADE = "Cliente";
    static final int TAMANHO_MAXIMO_NOME = 100;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public Cliente novoCliente(Cliente cliente) {
        return incluir(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    public void excluir(String id) {
        clienteRepository.deleteById(id);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente) {
        if (cliente.getId() == null || cliente.getId().isBlank()) {
            return incluir(cliente);
        }
        return atualizar(cliente.getId(), cliente);
    }

    public Cliente incluir(Cliente dados) {
        Cliente novo = new Cliente();
        copiarDados(dados, novo);
        validar(novo);

        Cliente salvo = clienteRepository.save(novo);

        auditoriaService.registrar(TipoOperacao.INCLUSAO_CLIENTE, ENTIDADE, salvo.getId(),
                AuditoriaService.compararCampos(Map.of(), camposAuditados(salvo)), null);
        return salvo;
    }

    /**
     * Atualiza o cadastro e registra na auditoria somente os campos que mudaram,
     * com os valores anteriores e novos (ex.: logradouro, telefone).
     */
    public Cliente atualizar(String id, Cliente dados) {
        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));

        Map<String, Object> antes = camposAuditados(existente);
        copiarDados(dados, existente);
        validar(existente);

        Cliente salvo = clienteRepository.save(existente);

        List<CampoAlterado> alteracoes = AuditoriaService.compararCampos(antes, camposAuditados(salvo));
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(TipoOperacao.ALTERACAO_CLIENTE, ENTIDADE, salvo.getId(), alteracoes, null);
        }
        return salvo;
    }

    private void copiarDados(Cliente origem, Cliente destino) {
        destino.setNome(limpar(origem.getNome()));
        destino.setTelefone(limpar(origem.getTelefone()));
        destino.setCep(limpar(origem.getCep()));
        destino.setLogradouro(limpar(origem.getLogradouro()));
        destino.setNumero(limpar(origem.getNumero()));
        destino.setComplemento(limpar(origem.getComplemento()));
        destino.setBairro(limpar(origem.getBairro()));
        destino.setCidade(limpar(origem.getCidade()));
        String estado = limpar(origem.getEstado());
        destino.setEstado(estado != null ? estado.toUpperCase() : null);
        // Campo antigo: só é alterado quando enviado explicitamente
        if (origem.getEndereco() != null) {
            destino.setEndereco(limpar(origem.getEndereco()));
        }
    }

    private void validar(Cliente cliente) {
        if (cliente.getNome() == null) {
            throw new RegraNegocioException("O nome do cliente é obrigatório.");
        }
        if (cliente.getNome().length() > TAMANHO_MAXIMO_NOME) {
            throw new RegraNegocioException("O nome do cliente deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
        if (cliente.getTelefone() == null) {
            throw new RegraNegocioException("O telefone do cliente é obrigatório.");
        }
        int digitosTelefone = somenteDigitos(cliente.getTelefone()).length();
        if (digitosTelefone < 10 || digitosTelefone > 11) {
            throw new RegraNegocioException("Telefone inválido: informe DDD e número (10 ou 11 dígitos).");
        }
        if (cliente.getCep() != null && somenteDigitos(cliente.getCep()).length() != 8) {
            throw new RegraNegocioException("CEP inválido: informe 8 dígitos.");
        }
        if (cliente.getEstado() != null && !cliente.getEstado().matches("[A-Z]{2}")) {
            throw new RegraNegocioException("Estado inválido: informe a sigla com 2 letras (ex.: SP).");
        }
    }

    private Map<String, Object> camposAuditados(Cliente cliente) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("nome", cliente.getNome());
        campos.put("telefone", cliente.getTelefone());
        campos.put("cep", cliente.getCep());
        campos.put("logradouro", cliente.getLogradouro());
        campos.put("numero", cliente.getNumero());
        campos.put("complemento", cliente.getComplemento());
        campos.put("bairro", cliente.getBairro());
        campos.put("cidade", cliente.getCidade());
        campos.put("estado", cliente.getEstado());
        campos.put("endereco", cliente.getEndereco());
        return campos;
    }

    private static String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String somenteDigitos(String valor) {
        return valor.replaceAll("\\D", "");
    }
}
