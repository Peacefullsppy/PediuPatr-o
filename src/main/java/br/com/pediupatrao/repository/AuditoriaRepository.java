package br.com.pediupatrao.repository;
import br.com.pediupatrao.model.RegistroAuditoria;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
@Repository
public class AuditoriaRepository {
    private final MongoTemplate mongo;
    public AuditoriaRepository(MongoTemplate mongo) { this.mongo = mongo; }
    // A auditoria só oferece inserção e consulta. Não expõe save, update ou delete.
    public RegistroAuditoria inserir(RegistroAuditoria registro) { return mongo.insert(registro); }
    public Page<RegistroAuditoria> consultar(String entidade, String entidadeId, Pageable pagina) {
        Query consulta = new Query();
        if (entidade != null && !entidade.isBlank()) consulta.addCriteria(Criteria.where("entidade").is(entidade));
        if (entidadeId != null && !entidadeId.isBlank()) consulta.addCriteria(Criteria.where("entidadeId").is(entidadeId));
        long total = mongo.count(consulta, RegistroAuditoria.class);
        consulta.with(pagina).with(Sort.by(Sort.Direction.DESC, "ocorridoEm", "id"));
        return new PageImpl<>(mongo.find(consulta, RegistroAuditoria.class), pagina, total);
    }
}
