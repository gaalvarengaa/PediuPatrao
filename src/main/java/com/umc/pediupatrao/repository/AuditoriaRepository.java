package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.RegistroAuditoria;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Repositório somente de inclusão e leitura: não expõe save/update/delete,
 * garantindo que os registros de auditoria não sejam alterados nem excluídos.
 */
public interface AuditoriaRepository extends Repository<RegistroAuditoria, String> {

    <S extends RegistroAuditoria> S insert(S registro);

    List<RegistroAuditoria> findAllByOrderByDataHoraDesc();
}
