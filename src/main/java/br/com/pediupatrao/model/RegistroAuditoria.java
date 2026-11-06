package br.com.pediupatrao.model;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
@Document("auditoria")
public record RegistroAuditoria(@Id String id, String usuario, Perfil perfil,
    @Indexed Instant ocorridoEm, String operacao, String entidade, @Indexed String entidadeId,
    List<AlteracaoCampo> alteracoes, String justificativa) {
    public RegistroAuditoria { alteracoes = List.copyOf(alteracoes); }
}
