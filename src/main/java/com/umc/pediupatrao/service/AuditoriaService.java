package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.CampoAlterado;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.entity.TipoOperacao;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Campos que nunca são gravados na auditoria. */
    private static final Set<String> CAMPOS_PROIBIDOS = Set.of("password", "senha", "token");

    /** Dados pessoais exibidos de forma parcial na consulta. */
    private static final Set<String> CAMPOS_MASCARADOS = Set.of("telefone");

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioLogado usuarioLogado;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioLogado usuarioLogado) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioLogado = usuarioLogado;
    }

    public RegistroAuditoria registrar(TipoOperacao operacao, String entidade, String entidadeId,
            List<CampoAlterado> alteracoes, String justificativa) {
        List<CampoAlterado> permitidas = alteracoes == null ? List.of() : alteracoes.stream()
                .filter(a -> !CAMPOS_PROIBIDOS.contains(a.campo().toLowerCase()))
                .toList();

        RegistroAuditoria registro = new RegistroAuditoria(
                usuarioLogado.getUsername(),
                usuarioLogado.getPerfil(),
                LocalDateTime.now(),
                operacao,
                entidade,
                entidadeId,
                permitidas,
                justificativa);
        return auditoriaRepository.insert(registro);
    }

    /**
     * Consulta os registros (mais recentes primeiro), com filtros opcionais.
     * Telefones são mascarados para limitar a exposição de dados pessoais.
     */
    public List<RegistroAuditoria> consultar(String entidade, String entidadeId, String usuario) {
        return auditoriaRepository.findAllByOrderByDataHoraDesc().stream()
                .filter(r -> vazio(entidade) || entidade.equalsIgnoreCase(r.getEntidade()))
                .filter(r -> vazio(entidadeId) || entidadeId.equals(r.getEntidadeId()))
                .filter(r -> vazio(usuario) || usuario.equalsIgnoreCase(r.getUsuario()))
                .map(this::mascararDadosPessoais)
                .toList();
    }

    /**
     * Compara dois mapas campo -> valor e devolve somente os campos que mudaram.
     */
    public static List<CampoAlterado> compararCampos(Map<String, ?> antes, Map<String, ?> depois) {
        Set<String> campos = new LinkedHashSet<>();
        campos.addAll(antes.keySet());
        campos.addAll(depois.keySet());

        List<CampoAlterado> alteracoes = new ArrayList<>();
        for (String campo : campos) {
            String anterior = formatarValor(antes.get(campo));
            String novo = formatarValor(depois.get(campo));
            if (!Objects.equals(anterior, novo)) {
                alteracoes.add(new CampoAlterado(campo, anterior, novo));
            }
        }
        return alteracoes;
    }

    public static String formatarValor(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal numero) {
            return numero.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        if (valor instanceof LocalDateTime data) {
            return data.format(FORMATO_DATA);
        }
        if (valor instanceof Enum<?> e) {
            return e.name();
        }
        String texto = valor.toString();
        return texto.isBlank() ? null : texto;
    }

    static String mascarar(String valor) {
        if (valor == null) {
            return null;
        }
        int digitos = (int) valor.chars().filter(Character::isDigit).count();
        int visiveisAPartir = digitos - 4;
        StringBuilder sb = new StringBuilder();
        int contador = 0;
        for (char c : valor.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(contador++ < visiveisAPartir ? '*' : c);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private RegistroAuditoria mascararDadosPessoais(RegistroAuditoria registro) {
        boolean possuiDadoPessoal = registro.getAlteracoes().stream().anyMatch(a -> CAMPOS_MASCARADOS.contains(a.campo()));
        if (!possuiDadoPessoal) {
            return registro;
        }
        List<CampoAlterado> alteracoes = registro.getAlteracoes().stream()
                .map(a -> CAMPOS_MASCARADOS.contains(a.campo())
                        ? new CampoAlterado(a.campo(), mascarar(a.valorAnterior()), mascarar(a.valorNovo()))
                        : a)
                .toList();
        return registro.comAlteracoes(alteracoes);
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
