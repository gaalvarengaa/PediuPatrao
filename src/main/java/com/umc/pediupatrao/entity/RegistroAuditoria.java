package com.umc.pediupatrao.entity;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Evento de auditoria. Não possui setters: depois de criado, o registro não é alterado.
 */
@Document(collection = "auditoria")
public class RegistroAuditoria {

    @Id
    private String id;
    private String usuario;
    private String perfil;
    private LocalDateTime dataHora;
    private TipoOperacao operacao;
    private String entidade;
    private String entidadeId;
    private List<CampoAlterado> alteracoes;
    private String justificativa;

    protected RegistroAuditoria() {
        // usado pelo Spring Data
    }

    public RegistroAuditoria(String usuario, String perfil, LocalDateTime dataHora, TipoOperacao operacao,
            String entidade, String entidadeId, List<CampoAlterado> alteracoes, String justificativa) {
        this.usuario = usuario;
        this.perfil = perfil;
        this.dataHora = dataHora;
        this.operacao = operacao;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.alteracoes = alteracoes != null ? List.copyOf(alteracoes) : List.of();
        this.justificativa = justificativa;
    }

    /** Cópia do registro com outra lista de alterações (usada para mascarar dados na consulta). */
    public RegistroAuditoria comAlteracoes(List<CampoAlterado> novasAlteracoes) {
        RegistroAuditoria copia = new RegistroAuditoria(usuario, perfil, dataHora, operacao,
                entidade, entidadeId, novasAlteracoes, justificativa);
        copia.id = this.id;
        return copia;
    }

    public String getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getPerfil() { return perfil; }
    public LocalDateTime getDataHora() { return dataHora; }
    public TipoOperacao getOperacao() { return operacao; }
    public String getEntidade() { return entidade; }
    public String getEntidadeId() { return entidadeId; }
    public List<CampoAlterado> getAlteracoes() { return alteracoes; }
    public String getJustificativa() { return justificativa; }
}
