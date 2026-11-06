package br.com.pediupatrao.service;
import br.com.pediupatrao.model.*;
import br.com.pediupatrao.repository.AuditoriaRepository;
import br.com.pediupatrao.security.UsuarioAtual;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class AuditoriaService {
    private final AuditoriaRepository registros;
    private final UsuarioAtual usuarioAtual;
    private final Clock clock;
    public AuditoriaService(AuditoriaRepository registros, UsuarioAtual usuarioAtual, Clock clock) {
        this.registros=registros; this.usuarioAtual=usuarioAtual; this.clock=clock;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void registrar(String operacao, String entidade, String id,
            Map<String,String> antes, Map<String,String> depois, String justificativa) {
        Set<String> campos = new LinkedHashSet<>(antes.keySet());
        campos.addAll(depois.keySet());
        var mudancas = campos.stream().filter(c -> !Objects.equals(antes.get(c), depois.get(c)))
            .map(c -> new AlteracaoCampo(c, antes.get(c), depois.get(c))).toList();
        if (mudancas.isEmpty()) return;
        var autor = usuarioAtual.obter();
        registros.inserir(new RegistroAuditoria(null, autor.usuario(), autor.perfil(), clock.instant(),
            operacao, entidade, id, mudancas, justificativa));
    }
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Page<RegistroAuditoria> consultar(String entidade, String entidadeId, int pagina) {
        return registros.consultar(entidade, entidadeId, PageRequest.of(Math.max(0,pagina),20));
    }
}
