package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    List<Pedido> findByClienteId(String clienteId);

    boolean existsByClienteId(String clienteId);

    /** Pedidos com entrada a partir de inicio e antes de fim, do mais antigo para o mais recente. */
    @Query(value = "{ 'dataHoraEntrada': { $gte: ?0, $lt: ?1 } }", sort = "{ 'dataHoraEntrada': 1 }")
    List<Pedido> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim);
}
