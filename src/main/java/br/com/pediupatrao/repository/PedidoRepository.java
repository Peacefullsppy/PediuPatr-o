package br.com.pediupatrao.repository;

import br.com.pediupatrao.model.Pedido;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    /** Consulta usada pelo painel: início inclusivo, fim exclusivo em UTC. */
    List<Pedido> findByEntradaEmGreaterThanEqualAndEntradaEmLessThan(Instant inicio, Instant fimExclusivo);
    Page<Pedido> findAllByOrderByEntradaEmDesc(Pageable pageable);
}
