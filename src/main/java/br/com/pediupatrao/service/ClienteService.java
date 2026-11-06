package br.com.pediupatrao.service;
import br.com.pediupatrao.dto.Formularios.ClienteForm;
import br.com.pediupatrao.model.Cliente;
import br.com.pediupatrao.repository.ClienteRepository;
import br.com.pediupatrao.exception.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
public class ClienteService {
    private final ClienteRepository clientes;
    private final AuditoriaService auditoria;
    public ClienteService(ClienteRepository clientes, AuditoriaService auditoria) { this.clientes=clientes; this.auditoria=auditoria; }
    public List<Cliente> listar() { return clientes.findByAtivoTrueOrderByNomeAsc(); }
    public Cliente buscar(String id) { return clientes.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado.")); }
    @Transactional @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente criar(ClienteForm dados) {
        var salvo = clientes.insert(new Cliente(null,null,dados.nome().trim(),dados.telefone(),dados.endereco(),true));
        auditoria.registrar("INCLUSAO_CLIENTE","CLIENTE",salvo.id(),Map.of(),RetratosAuditoria.cliente(salvo),null);
        return salvo;
    }
    @Transactional @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente atualizar(String id, ClienteForm dados) {
        var antes=buscar(id);
        if (!antes.ativo()) throw new RegraNegocioException("Cliente já excluído.");
        var salvo=clientes.save(new Cliente(id,antes.versao(),dados.nome().trim(),dados.telefone(),dados.endereco(),true));
        auditoria.registrar("ALTERACAO_CLIENTE","CLIENTE",id,RetratosAuditoria.cliente(antes),RetratosAuditoria.cliente(salvo),null);
        return salvo;
    }
    @Transactional @PreAuthorize("hasRole('ATENDENTE')")
    public void excluir(String id) {
        var antes=buscar(id);
        if (!antes.ativo()) throw new RegraNegocioException("Cliente já excluído.");
        var salvo=clientes.save(new Cliente(id,antes.versao(),antes.nome(),antes.telefone(),antes.endereco(),false));
        auditoria.registrar("EXCLUSAO_CLIENTE","CLIENTE",id,RetratosAuditoria.cliente(antes),RetratosAuditoria.cliente(salvo),null);
    }
}
